package org.bruneel.notifier;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifierMod implements ModInitializer {
	public static final String MOD_ID = "notifier";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Notifier initialized");
	}
}
