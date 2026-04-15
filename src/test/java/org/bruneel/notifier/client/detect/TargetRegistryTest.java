package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetRegistryTest {
	@Test
	void upsertReplacesExistingTarget() {
		TargetRegistry registry = new TargetRegistry();
		Identifier id = Identifier.of("minecraft", "horse");
		registry.upsert(new DetectionTarget(DetectionKind.ENTITY, id, true, 16.0, 10, 100, "one"));
		registry.upsert(new DetectionTarget(DetectionKind.ENTITY, id, false, 20.0, 20, 200, "two"));

		assertEquals(1, registry.allTargets().size());
		DetectionTarget target = registry.find(DetectionKind.ENTITY, "minecraft:horse");
		assertNotNull(target);
		assertFalse(target.enabled());
		assertEquals(20.0, target.radius());
	}

	@Test
	void enabledTargetsFiltersDisabledEntries() {
		TargetRegistry registry = new TargetRegistry();
		registry.upsert(new DetectionTarget(
			DetectionKind.ENTITY, Identifier.of("minecraft", "cow"), true, 16.0, 10, 100, "cow"
		));
		registry.upsert(new DetectionTarget(
			DetectionKind.BLOCK, Identifier.of("minecraft", "diamond_ore"), false, 8.0, 30, 120, "ore"
		));

		assertEquals(2, registry.allTargets().size());
		assertEquals(1, registry.enabledTargets().size());
		assertTrue(registry.enabledTargets().getFirst().id().toString().contains("cow"));
	}
}
