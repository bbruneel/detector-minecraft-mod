package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;

public record DetectionScanHit(
	DetectionKind kind,
	Identifier id,
	int x,
	int y,
	int z,
	Double distance
) {
	public String key() {
		return kind.name() + ":" + id;
	}
}
