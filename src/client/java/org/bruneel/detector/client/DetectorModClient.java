package org.bruneel.detector.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.bruneel.detector.DetectorMod;
import org.bruneel.detector.client.command.DetectorClientCommands;
import org.bruneel.detector.client.detect.DetectionEngine;
import org.bruneel.detector.client.detect.DetectionRuntimeState;
import org.bruneel.detector.client.detect.DetectorConfigStore;
import org.bruneel.detector.client.detect.ScanHighlightState;
import org.bruneel.detector.client.detect.TargetRegistry;
import org.bruneel.detector.client.render.ScanHighlightRenderer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Objects;

public class DetectorModClient implements ClientModInitializer {
	private TargetRegistry targetRegistry;
	private DetectionEngine detectionEngine;
	private DetectorConfigStore configStore;
	private boolean verboseLogging;
	private AtomicBoolean highlightOnMatch;
	private boolean hasLoggedWaitingForWorld = false;
	private ScanHighlightState scanHighlightState;
	private ScanHighlightRenderer scanHighlightRenderer;
	private long lastHighlightRenderDebugTick = Long.MIN_VALUE;
	private long lastHighlightRenderDetailTick = Long.MIN_VALUE;
	private boolean hasLoggedRenderCallbackSeen = false;

	@Override
	public void onInitializeClient() {
		configStore = new DetectorConfigStore(FabricLoader.getInstance().getConfigDir());
		DetectorConfigStore.LoadResult loadResult = configStore.loadOrDefault();
		targetRegistry = loadResult.registry();
		verboseLogging = loadResult.verboseLogging();
		scanHighlightState = new ScanHighlightState();
		scanHighlightRenderer = new ScanHighlightRenderer();

		highlightOnMatch = new AtomicBoolean(loadResult.highlightOnMatch());
		configStore.save(targetRegistry, verboseLogging, highlightOnMatch.get());

		detectionEngine = new DetectionEngine(
			targetRegistry,
			new DetectionRuntimeState(),
			verboseLogging,
			scanHighlightState,
			highlightOnMatch::get
		);

		DetectorClientCommands.register(
			targetRegistry,
			configStore,
			() -> verboseLogging,
			highlightOnMatch,
			scanHighlightState
		);

		DetectorMod.LOGGER.info(
			"Detector initialized with {} targets (enabled={}, verbose={})",
			targetRegistry.allTargets().size(),
			targetRegistry.enabledTargets().size(),
			verboseLogging
		);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		WorldRenderEvents.BEFORE_ENTITIES.register(this::renderScanHighlights);
		DetectorMod.LOGGER.info("Detector render hook registered: BEFORE_ENTITIES");
	}

	private void onEndClientTick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			if (verboseLogging && !hasLoggedWaitingForWorld) {
				DetectorMod.LOGGER.info("Detector waiting for client world/player to load");
				hasLoggedWaitingForWorld = true;
			}
			return;
		}
		if (hasLoggedWaitingForWorld) {
			DetectorMod.LOGGER.info("Detector detected client world/player; scanning active");
			hasLoggedWaitingForWorld = false;
		}
		detectionEngine.tick(client);
	}

	private void renderScanHighlights(WorldRenderContext context) {
		if (!hasLoggedRenderCallbackSeen) {
			hasLoggedRenderCallbackSeen = true;
			DetectorMod.LOGGER.info("Detector render callback invoked: BEFORE_ENTITIES");
		}

		MinecraftClient client = MinecraftClient.getInstance();
		if (client.world == null || scanHighlightState == null || scanHighlightRenderer == null) {
			return;
		}
		final var world = Objects.requireNonNull(client.world);
		Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
		List<ScanHighlightState.ScanHighlight> activeHighlights = scanHighlightState.activeHighlights(world.getTime());
		long worldTime = client.world == null ? -1L : client.world.getTime();

		if (worldTime - lastHighlightRenderDebugTick >= 20) {
			lastHighlightRenderDebugTick = worldTime;
			DetectorMod.LOGGER.info(
				"Highlight render callback tick={} extractedPresent={} extractedCount={}",
				worldTime,
				activeHighlights != null,
				activeHighlights == null ? 0 : activeHighlights.size()
			);
		}

		if (activeHighlights == null || activeHighlights.isEmpty()) {
			return;
		}

		float tickDelta = client.getRenderTickCounter().getTickProgress(true);

		if (worldTime - lastHighlightRenderDetailTick >= 20) {
			lastHighlightRenderDetailTick = worldTime;
			var first = activeHighlights.getFirst();
			DetectorMod.LOGGER.info(
				"Highlight render tick={} active={} phase=end_main first={} id={} box=[{},{},{} -> {},{},{}]",
				worldTime,
				activeHighlights.size(),
				first.kind(),
				first.targetId(),
				first.box().minX,
				first.box().minY,
				first.box().minZ,
				first.box().maxX,
				first.box().maxY,
				first.box().maxZ
			);
		}

		scanHighlightRenderer.render(context, world, activeHighlights, cameraPos, tickDelta);
	}

}
