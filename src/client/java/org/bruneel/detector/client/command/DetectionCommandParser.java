package org.bruneel.detector.client.command;

import net.minecraft.util.Identifier;

public final class DetectionCommandParser {
	private DetectionCommandParser() {
	}

	public static Identifier parseIdentifier(String raw) {
		return Identifier.of(raw.trim());
	}
}
