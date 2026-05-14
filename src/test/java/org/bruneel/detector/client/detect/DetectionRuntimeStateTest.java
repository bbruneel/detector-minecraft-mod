package org.bruneel.detector.client.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DetectionRuntimeStateTest {
	@Test
	void messageAndHighlightCooldownsDecrementUntilZero() {
		DetectionRuntimeState state = new DetectionRuntimeState();
		state.setMessageCooldown("k", 2);
		state.setHighlightCooldown("k", 1);

		assertEquals(1, state.tickMessageCooldown("k"));
		assertEquals(0, state.tickMessageCooldown("k"));
		assertEquals(0, state.tickMessageCooldown("k"));

		assertEquals(0, state.tickHighlightCooldown("k"));
		assertEquals(0, state.tickHighlightCooldown("k"));
	}

	@Test
	void intervalCounterResetsWhenRequested() {
		DetectionRuntimeState state = new DetectionRuntimeState();

		assertEquals(1, state.nextIntervalCount("k"));
		assertEquals(2, state.nextIntervalCount("k"));
		state.resetIntervalCount("k");
		assertEquals(1, state.nextIntervalCount("k"));
	}
}
