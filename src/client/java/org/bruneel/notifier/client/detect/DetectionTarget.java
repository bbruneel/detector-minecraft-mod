package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;

public record DetectionTarget(
	DetectionKind kind,
	Identifier id,
	boolean enabled,
	double radius,
	int checkIntervalTicks,
	int cooldownTicks,
	String messageTemplate
) {
	public String key() {
		return kind.name().toLowerCase() + ":" + id;
	}

	public DetectionTarget withEnabled(boolean value) {
		return new DetectionTarget(kind, id, value, radius, checkIntervalTicks, cooldownTicks, messageTemplate);
	}

	public DetectionTarget withRadius(double value) {
		return new DetectionTarget(kind, id, enabled, value, checkIntervalTicks, cooldownTicks, messageTemplate);
	}

	public DetectionTarget withCheckIntervalTicks(int value) {
		return new DetectionTarget(kind, id, enabled, radius, value, cooldownTicks, messageTemplate);
	}

	public DetectionTarget withCooldownTicks(int value) {
		return new DetectionTarget(kind, id, enabled, radius, checkIntervalTicks, value, messageTemplate);
	}
}
