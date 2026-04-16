package org.bruneel.notifier.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.bruneel.notifier.NotifierMod;
import org.bruneel.notifier.client.command.NotifierClientCommands;
import org.bruneel.notifier.client.detect.DetectionKind;
import org.bruneel.notifier.client.detect.DetectionEngine;
import org.bruneel.notifier.client.detect.DetectionRuntimeState;
import org.bruneel.notifier.client.detect.NotifierConfigStore;
import org.bruneel.notifier.client.detect.ScanHighlightState;
import org.bruneel.notifier.client.detect.TargetRegistry;
import org.bruneel.notifier.client.render.NoDepthLineLayer;

import java.util.List;
import java.util.Objects;

public class NotifierModClient implements ClientModInitializer {
	private TargetRegistry targetRegistry;
	private DetectionEngine detectionEngine;
	private NotifierConfigStore configStore;
	private boolean verboseLogging;
	private boolean hasLoggedWaitingForWorld = false;
	private ScanHighlightState scanHighlightState;
	private long lastHighlightRenderDebugTick = Long.MIN_VALUE;
	private long lastHighlightRenderDetailTick = Long.MIN_VALUE;
	private boolean hasLoggedRenderCallbackSeen = false;

	@Override
	public void onInitializeClient() {
		configStore = new NotifierConfigStore(FabricLoader.getInstance().getConfigDir());
		NotifierConfigStore.LoadResult loadResult = configStore.loadOrDefault();
		targetRegistry = loadResult.registry();
		verboseLogging = loadResult.verboseLogging();
		configStore.save(targetRegistry, verboseLogging);

		detectionEngine = new DetectionEngine(targetRegistry, new DetectionRuntimeState(), verboseLogging);
		scanHighlightState = new ScanHighlightState();
		NotifierClientCommands.register(targetRegistry, configStore, () -> verboseLogging, scanHighlightState);

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
		if (client.world == null || scanHighlightState == null) {
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

		MatrixStack matrices = context.matrices();
		VertexConsumer lines = context.consumers().getBuffer(NoDepthLineLayer.LINES_NO_DEPTH);

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

		for (ScanHighlightState.ScanHighlight highlight : activeHighlights) {
			Box shifted = highlight.box().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
			Box outline = highlight.kind() == DetectionKind.BLOCK
				? shifted.expand(0.002D)
				: shifted;
			ScanHighlightState.ScanHighlightColor color = highlight.color();
			drawBoxLines(matrices, lines, outline, color);
		}
	}

	private static void drawBoxLines(
		MatrixStack matrices,
		VertexConsumer lines,
		Box box,
		ScanHighlightState.ScanHighlightColor color
	) {
		var entry = matrices.peek();
		float r = color.red();
		float g = color.green();
		float b = color.blue();
		float a = color.alpha();

		emitLine(lines, entry, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, r, g, b, a);

		emitLine(lines, entry, box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);

		emitLine(lines, entry, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);

		emitLine(lines, entry, box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
	}

	private static void emitLine(
		VertexConsumer lines,
		MatrixStack.Entry entry,
		double sx, double sy, double sz,
		double ex, double ey, double ez,
		float r, float g, float b, float a
	) {
		float nx = (float) (ex - sx);
		float ny = (float) (ey - sy);
		float nz = (float) (ez - sz);
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length <= 1.0e-6F) {
			return;
		}
		nx /= length;
		ny /= length;
		nz /= length;

		lines.vertex(entry, (float) sx, (float) sy, (float) sz)
			.color(r, g, b, a)
			.normal(entry, nx, ny, nz)
			.lineWidth(2.0F);
		lines.vertex(entry, (float) ex, (float) ey, (float) ez)
			.color(r, g, b, a)
			.normal(entry, nx, ny, nz)
			.lineWidth(2.0F);
	}

}
