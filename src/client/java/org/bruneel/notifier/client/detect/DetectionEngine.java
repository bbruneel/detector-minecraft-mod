package org.bruneel.notifier.client.detect;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import org.bruneel.notifier.NotifierMod;

import java.util.List;

public final class DetectionEngine {
	private final TargetRegistry targetRegistry;
	private final DetectionRuntimeState state;
	private final boolean verboseLogging;

	public DetectionEngine(TargetRegistry targetRegistry, DetectionRuntimeState state, boolean verboseLogging) {
		this.targetRegistry = targetRegistry;
		this.state = state;
		this.verboseLogging = verboseLogging;
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
		int cooldown = state.tickCooldown(key);

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
		boolean wasNearby = state.wasNearby(key);

		if (verboseLogging) {
			NotifierMod.LOGGER.info(
				"Detect scan kind={}, id={}, count={}, nearby={}, wasNearby={}, cooldown={}",
				target.kind(),
				target.id(),
				nearbyCount,
				nearby,
				wasNearby,
				cooldown
			);
		}

		if (nearby && !wasNearby && cooldown == 0) {
			player.sendMessage(Text.literal(target.messageTemplate()), true);
			state.setCooldown(key, target.cooldownTicks());
			NotifierMod.LOGGER.info("Detect message sent kind={}, id={}", target.kind(), target.id());
		}

		state.setWasNearby(key, nearby);
	}
}
