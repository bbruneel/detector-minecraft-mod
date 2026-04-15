package com.example.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;

import java.util.Objects;

public class ExampleModClient implements ClientModInitializer {
	private static final double DETECTION_RADIUS = 16.0;
	private static final int CHECK_INTERVAL_TICKS = 10;
	private static final int MESSAGE_COOLDOWN_TICKS = 100;

	private int tickCounter = 0;
	private int messageCooldown = 0;
	private boolean wasChickenNearby = false;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
	}

	private void onEndClientTick(MinecraftClient client) {
		if (client.player == null || client.world == null) {
			wasChickenNearby = false;
			return;
		}
		final var player = Objects.requireNonNull(client.player);
		final var world = Objects.requireNonNull(client.world);

		if (messageCooldown > 0) {
			messageCooldown--;
		}

		tickCounter++;
		if (tickCounter < CHECK_INTERVAL_TICKS) {
			return;
		}
		tickCounter = 0;

		Box searchArea = player.getBoundingBox().expand(DETECTION_RADIUS);
		boolean chickenNearby = !world
			.getEntitiesByClass(ChickenEntity.class, searchArea, chicken -> true)
			.isEmpty();

		if (chickenNearby && !wasChickenNearby && messageCooldown == 0) {
			player.sendMessage(Text.literal("A chicken is in the neighbourhood!"), true);
			messageCooldown = MESSAGE_COOLDOWN_TICKS;
		}
		wasChickenNearby = chickenNearby;
	}
}