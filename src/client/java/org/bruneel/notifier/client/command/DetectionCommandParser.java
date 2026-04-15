package org.bruneel.notifier.client.command;

import net.minecraft.util.Identifier;
import org.bruneel.notifier.client.detect.DetectionKind;

public final class DetectionCommandParser {
	private DetectionCommandParser() {
	}

	public static DetectionKind parseKind(String raw) {
		return DetectionKind.valueOf(raw.trim().toUpperCase());
	}

	public static Identifier parseIdentifier(String raw) {
		return Identifier.of(raw.trim());
	}
}
