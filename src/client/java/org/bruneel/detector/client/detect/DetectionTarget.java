package org.bruneel.detector.client.detect;

import net.minecraft.util.Identifier;

public record DetectionTarget(
	DetectionKind kind,
	Identifier id,
	boolean enabled,
	double radius,
	int checkIntervalTicks,
	int messageCooldownTicks,
	int highlightCooldownTicks,
	String messageTemplate
) {
	public String key() {
		return kind.name().toLowerCase() + ":" + id;
	}

	public DetectionTarget withEnabled(boolean value) {
		return new DetectionTarget(
			kind,
			id,
			value,
			radius,
			checkIntervalTicks,
			messageCooldownTicks,
			highlightCooldownTicks,
			messageTemplate
		);
	}

	public DetectionTarget withRadius(double value) {
		return new DetectionTarget(
			kind,
			id,
			enabled,
			value,
			checkIntervalTicks,
			messageCooldownTicks,
			highlightCooldownTicks,
			messageTemplate
		);
	}

	public DetectionTarget withCheckIntervalTicks(int value) {
		return new DetectionTarget(
			kind,
			id,
			enabled,
			radius,
			value,
			messageCooldownTicks,
			highlightCooldownTicks,
			messageTemplate
		);
	}

	public DetectionTarget withMessageCooldownTicks(int value) {
		return new DetectionTarget(
			kind,
			id,
			enabled,
			radius,
			checkIntervalTicks,
			value,
			highlightCooldownTicks,
			messageTemplate
		);
	}

	public DetectionTarget withHighlightCooldownTicks(int value) {
		return new DetectionTarget(
			kind,
			id,
			enabled,
			radius,
			checkIntervalTicks,
			messageCooldownTicks,
			value,
			messageTemplate
		);
	}

	public DetectionTarget withCooldownTicks(int value) {
		return new DetectionTarget(
			kind,
			id,
			enabled,
			radius,
			checkIntervalTicks,
			value,
			value,
			messageTemplate
		);
	}
}
