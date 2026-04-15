package org.bruneel.notifier.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import org.bruneel.notifier.NotifierMod;
import org.bruneel.notifier.client.command.NotifierClientCommands;
import org.bruneel.notifier.client.detect.DetectionEngine;
import org.bruneel.notifier.client.detect.DetectionRuntimeState;
import org.bruneel.notifier.client.detect.NotifierConfigStore;
import org.bruneel.notifier.client.detect.TargetRegistry;

public class NotifierModClient implements ClientModInitializer {
	private TargetRegistry targetRegistry;
	private DetectionEngine detectionEngine;
	private NotifierConfigStore configStore;
	private boolean verboseLogging;
	private boolean hasLoggedWaitingForWorld = false;

	@Override
	public void onInitializeClient() {
		configStore = new NotifierConfigStore(FabricLoader.getInstance().getConfigDir());
		NotifierConfigStore.LoadResult loadResult = configStore.loadOrDefault();
		targetRegistry = loadResult.registry();
		verboseLogging = loadResult.verboseLogging();
		configStore.save(targetRegistry, verboseLogging);

		detectionEngine = new DetectionEngine(targetRegistry, new DetectionRuntimeState(), verboseLogging);
		NotifierClientCommands.register(targetRegistry, configStore, () -> verboseLogging);

		NotifierMod.LOGGER.info(
			"Notifier initialized with {} targets (enabled={}, verbose={})",
			targetRegistry.allTargets().size(),
			targetRegistry.enabledTargets().size(),
			verboseLogging
		);
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
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
}
