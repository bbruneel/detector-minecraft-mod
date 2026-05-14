package org.bruneel.detector;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DetectorMod implements ModInitializer {
	public static final String MOD_ID = "detector";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Detector mod initialized");
	}
}
