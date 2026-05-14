package org.bruneel.detector.client.detect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.bruneel.detector.DetectorMod;

import java.util.Objects;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class DetectionEngine {
	private final TargetRegistry targetRegistry;
	private final DetectionRuntimeState state;
	private final boolean verboseLogging;
	private final ScanHighlightState scanHighlightState;
	private final BooleanSupplier highlightOnMatch;

	public DetectionEngine(
		TargetRegistry targetRegistry,
		DetectionRuntimeState state,
		boolean verboseLogging,
		ScanHighlightState scanHighlightState,
		BooleanSupplier highlightOnMatch
	) {
		this.targetRegistry = targetRegistry;
		this.state = state;
		this.verboseLogging = verboseLogging;
		this.scanHighlightState = Objects.requireNonNull(scanHighlightState, "scanHighlightState");
		this.highlightOnMatch = Objects.requireNonNull(highlightOnMatch, "highlightOnMatch");
	}

	public void tick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			return;
		}

		ClientPlayerEntity player = client.player;
		ClientWorld world = client.world;
		List<DetectionTarget> enabledTargets = targetRegistry.enabledTargets();

		for (DetectionTarget target : enabledTargets) {
			tickTarget(world, player, target);
		}
	}

	private void tickTarget(ClientWorld world, ClientPlayerEntity player, DetectionTarget target) {
		String key = target.key();
		int messageCooldown = state.tickMessageCooldown(key);
		int highlightCooldown = state.tickHighlightCooldown(key);

		int count = state.nextIntervalCount(key);
		if (count < target.checkIntervalTicks()) {
			return;
		}
		state.resetIntervalCount(key);

		int nearbyCount = switch (target.kind()) {
			case ENTITY -> EntityScanner.countNearby(world, player, target);
			case BLOCK -> BlockScanner.countNearby(world, player, target);
		};

		boolean nearby = nearbyCount > 0;
		boolean shouldSendMessage = shouldTrigger(nearby, messageCooldown);
		boolean shouldRefreshHighlights = shouldTrigger(nearby, highlightCooldown);

		if (verboseLogging) {
			DetectorMod.LOGGER.info(
				"Detect trigger check key={}, kind={}, id={}, count={}, nearby={}, messageCooldown={}, shouldSendMessage={}, highlightCooldown={}, shouldRefreshHighlights={}",
				key,
				target.kind(),
				target.id(),
				nearbyCount,
				nearby,
				messageCooldown,
				shouldSendMessage,
				highlightCooldown,
				shouldRefreshHighlights
			);
		}

		if (shouldSendMessage) {
			player.sendMessage(Text.literal(target.messageTemplate()), true);
			state.setMessageCooldown(key, target.messageCooldownTicks());
			DetectorMod.LOGGER.info(
				"Detect message sent kind={}, id={}, messageCooldownTicks={}",
				target.kind(),
				target.id(),
				target.messageCooldownTicks()
			);
		}

		if (highlightOnMatch.getAsBoolean() && shouldRefreshHighlights) {
			int limit = DetectionScanService.PER_TARGET_LIMIT + 1;
			List<DetectionScanHit> hits = switch (target.kind()) {
				case ENTITY -> EntityScanner.findNearby(world, player, target, limit);
				case BLOCK -> BlockScanner.findNearby(world, player, target, limit);
			};
			if (verboseLogging && hits.size() >= limit) {
				DetectorMod.LOGGER.info(
					"Detect highlight results reached per-target max kind={}, id={}, returnedHits={}, maxPerTarget={}, note=results_may_be_truncated",
					target.kind(),
					target.id(),
					hits.size(),
					DetectionScanService.PER_TARGET_LIMIT
				);
			}

			var highlighted = scanHighlightState.upsertWithScanResults(hits, world.getTime());
			state.setHighlightCooldown(key, target.highlightCooldownTicks());
			DetectorMod.LOGGER.info(
				"Detect highlight updated mode=upsert kind={}, id={}, entities={}, blocks={}, total={}, highlightCooldownTicks={}",
				target.kind(),
				target.id(),
				highlighted.entities(),
				highlighted.blocks(),
				highlighted.total(),
				target.highlightCooldownTicks()
			);

			if (verboseLogging) {
				DetectorMod.LOGGER.info(
					"Detect highlight details kind={}, id={}, hits={}",
					target.kind(),
					target.id(),
					hits
				);
			}
		}
	}

	static boolean shouldTrigger(boolean nearby, int cooldown) {
		return nearby && cooldown == 0;
	}
}
