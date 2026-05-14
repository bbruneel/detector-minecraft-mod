package org.bruneel.detector.client.command;

import net.minecraft.util.Identifier;
import org.bruneel.detector.client.detect.DetectionKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectorClientCommandsSuggestionTest {
	@Test
	void suggestedIdsForEntityKindContainsHorse() {
		Iterable<Identifier> result = DetectorClientCommands.suggestedIdsForKind(
			DetectionKind.ENTITY,
			java.util.List.of(Identifier.of("minecraft:horse")),
			java.util.List.of(Identifier.of("minecraft:stone"))
		);
		boolean containsHorse = contains(result, "minecraft:horse");
		assertTrue(containsHorse);
	}

	@Test
	void suggestedIdsForBlockKindContainsStone() {
		Iterable<Identifier> result = DetectorClientCommands.suggestedIdsForKind(
			DetectionKind.BLOCK,
			java.util.List.of(Identifier.of("minecraft:horse")),
			java.util.List.of(Identifier.of("minecraft:stone"))
		);
		boolean containsStone = contains(result, "minecraft:stone");
		assertTrue(containsStone);
	}

	@Test
	void suggestedIdsForUnknownKindReturnsEmpty() {
		boolean hasAny = DetectorClientCommands.suggestedIdsForKind(
			null,
			java.util.List.of(Identifier.of("minecraft:horse")),
			java.util.List.of(Identifier.of("minecraft:stone"))
		).iterator().hasNext();
		assertFalse(hasAny);
	}

	@Test
	void allOreIdsFromRegistryFiltersMinecraftOreLikeBlocks() {
		var result = DetectorClientCommands.allOreIdsFromRegistry(java.util.List.of(
			Identifier.of("minecraft:stone"),
			Identifier.of("minecraft:diamond_ore"),
			Identifier.of("minecraft:deepslate_redstone_ore"),
			Identifier.of("minecraft:ancient_debris"),
			Identifier.of("example:tin_ore")
		));

		assertEquals(java.util.List.of(
			Identifier.of("minecraft:ancient_debris"),
			Identifier.of("minecraft:deepslate_redstone_ore"),
			Identifier.of("minecraft:diamond_ore")
		), result);
	}

	private static boolean contains(Iterable<Identifier> ids, String expectedId) {
		for (Identifier id : ids) {
			if (expectedId.equals(id.toString())) {
				return true;
			}
		}
		return false;
	}
}
