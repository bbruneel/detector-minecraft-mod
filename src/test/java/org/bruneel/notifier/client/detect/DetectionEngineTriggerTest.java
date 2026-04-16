package org.bruneel.notifier.client.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionEngineTriggerTest {
	@Test
	void shouldTriggerOnlyOnEdgeTransitionWithZeroCooldown() {
		assertTrue(DetectionEngine.shouldTrigger(true, false, 0));

		assertFalse(DetectionEngine.shouldTrigger(true, true, 0), "No trigger when wasNearby already true");
		assertFalse(DetectionEngine.shouldTrigger(true, false, 1), "No trigger when cooldown > 0");
		assertFalse(DetectionEngine.shouldTrigger(false, false, 0), "No trigger when not nearby");
	}
}

