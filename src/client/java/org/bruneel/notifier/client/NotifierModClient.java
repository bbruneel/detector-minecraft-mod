package org.bruneel.notifier.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.bruneel.notifier.NotifierMod;
import org.bruneel.notifier.client.command.NotifierClientCommands;
import org.bruneel.notifier.client.detect.DetectionEngine;
import org.bruneel.notifier.client.detect.DetectionRuntimeState;
import org.bruneel.notifier.client.detect.NotifierConfigStore;
import org.bruneel.notifier.client.detect.ScanHighlightState;
import org.bruneel.notifier.client.detect.TargetRegistry;
import org.bruneel.notifier.client.render.ScanHighlightRenderer;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.Objects;

public class NotifierModClient implements ClientModInitializer {
	private TargetRegistry targetRegistry;
	private DetectionEngine detectionEngine;
	private NotifierConfigStore configStore;
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
		configStore = new NotifierConfigStore(FabricLoader.getInstance().getConfigDir());
		NotifierConfigStore.LoadResult loadResult = configStore.loadOrDefault();
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

		NotifierClientCommands.register(
			targetRegistry,
			configStore,
			() -> verboseLogging,
			highlightOnMatch,
			scanHighlightState
		);

		NotifierMod.LOGGER.info(
			"Notifier initialized with {} targets (enabled={}, verbose={})",
			targetRegistry.allTargets().size(),
			targetRegistry.enabledTargets().size(),
			verboseLogging
		);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
		WorldRenderEvents.BEFORE_ENTITIES.register(this::renderScanHighlights);
		NotifierMod.LOGGER.info("Notifier render hook registered: BEFORE_ENTITIES");
	}

	private void onEndClientTick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			if (verboseLogging && !hasLoggedWaitingForWorld) {
				NotifierMod.LOGGER.info("Notifier waiting for client world/player to load");
				hasLoggedWaitingForWorld = true;
			}
			return;
		}
		if (hasLoggedWaitingForWorld) {
			NotifierMod.LOGGER.info("Notifier detected client world/player; scanning active");
			hasLoggedWaitingForWorld = false;
		}
		detectionEngine.tick(client);
	}

	private void renderScanHighlights(WorldRenderContext context) {
		if (!hasLoggedRenderCallbackSeen) {
			hasLoggedRenderCallbackSeen = true;
			NotifierMod.LOGGER.info("Notifier render callback invoked: BEFORE_ENTITIES");
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
			NotifierMod.LOGGER.info(
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
			NotifierMod.LOGGER.info(
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
