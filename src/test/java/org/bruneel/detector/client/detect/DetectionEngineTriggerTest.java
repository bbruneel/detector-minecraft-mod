package org.bruneel.detector.client.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionEngineTriggerTest {
	@Test
	void shouldTriggerWheneverNearbyAndCooldownIsZero() {
		assertTrue(DetectionEngine.shouldTrigger(true, 0), "Trigger on first entry");
		assertTrue(
			DetectionEngine.shouldTrigger(true, 0),
			"Re-trigger while still nearby once cooldown has expired"
		);

		assertFalse(DetectionEngine.shouldTrigger(true, 1), "No trigger when cooldown > 0");
		assertFalse(DetectionEngine.shouldTrigger(false, 0), "No trigger when not nearby");
		assertFalse(DetectionEngine.shouldTrigger(false, 5), "No trigger when not nearby and cooldown > 0");
	}
}
