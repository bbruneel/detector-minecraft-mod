package org.bruneel.notifier.client;

import org.bruneel.notifier.NotifierMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.Objects;

public class NotifierModClient implements ClientModInitializer {
	private static final double DETECTION_RADIUS = 16.0;
	private static final int CHECK_INTERVAL_TICKS = 10;
	private static final int MESSAGE_COOLDOWN_TICKS = 100;
	private static final boolean VERBOSE_LOGGING = true;

	private int tickCounter = 0;
	private int messageCooldown = 0;
	private boolean wasChickenNearby = false;
	private boolean hasLoggedWaitingForWorld = false;

	@Override
	public void onInitializeClient() {
		NotifierMod.LOGGER.info(
			"Chicken notifier initialized (radius={}, intervalTicks={}, cooldownTicks={}, verbose={})",
			DETECTION_RADIUS,
			CHECK_INTERVAL_TICKS,
			MESSAGE_COOLDOWN_TICKS,
			VERBOSE_LOGGING
		);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
	}

	private void onEndClientTick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			if (VERBOSE_LOGGING && !hasLoggedWaitingForWorld) {
				NotifierMod.LOGGER.info("Chicken notifier waiting for client world/player to load");
				hasLoggedWaitingForWorld = true;
			}
			wasChickenNearby = false;
			return;
		}
		if (hasLoggedWaitingForWorld) {
			NotifierMod.LOGGER.info("Chicken notifier detected client world/player; scanning active");
			hasLoggedWaitingForWorld = false;
		}
		final var player = Objects.requireNonNull(client.player);
		final var world = Objects.requireNonNull(client.world);

		if (messageCooldown > 0) {
			messageCooldown--;
		}

		tickCounter++;
		// Skip most ticks to reduce per-tick work; scanning runs every CHECK_INTERVAL_TICKS.
		if (tickCounter < CHECK_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;

		Box searchArea = player.getBoundingBox().expand(DETECTION_RADIUS);
		int nearbyChickenCount = world
			.getEntitiesByClass(ChickenEntity.class, searchArea, chicken -> true)
			.size();
		boolean chickenNearby = nearbyChickenCount > 0;

		if (VERBOSE_LOGGING) {
			NotifierMod.LOGGER.info(
				"Chicken scan: count={}, previousNearby={}, cooldown={}, playerPos=({}, {}, {})",
				nearbyChickenCount,
				wasChickenNearby,
				messageCooldown,
				player.getBlockX(),
				player.getBlockY(),
				player.getBlockZ()
			);
		}

		if (chickenNearby && !wasChickenNearby && messageCooldown == 0) {
			player.sendMessage(Text.literal("A chicken is in the neighbourhood!"), true);
			messageCooldown = MESSAGE_COOLDOWN_TICKS;
			NotifierMod.LOGGER.info("Chicken notifier message sent to player");
		}
		wasChickenNearby = chickenNearby;
	}
}
