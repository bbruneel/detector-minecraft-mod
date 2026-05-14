package org.bruneel.detector.client.detect;

import net.minecraft.util.Identifier;

import java.util.UUID;

public record DetectionScanHit(
	DetectionKind kind,
	Identifier id,
	int x,
	int y,
	int z,
	Double distance,
	UUID entityUuid,
	Integer entityId
) {
	public String key() {
		return kind.name() + ":" + id;
	}
}
