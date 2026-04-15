package org.bruneel.notifier.client.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DetectionRuntimeStateTest {
	@Test
	void tickCooldownDecrementsUntilZero() {
		DetectionRuntimeState state = new DetectionRuntimeState();
		state.setCooldown("k", 2);

		assertEquals(1, state.tickCooldown("k"));
		assertEquals(0, state.tickCooldown("k"));
		assertEquals(0, state.tickCooldown("k"));
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
