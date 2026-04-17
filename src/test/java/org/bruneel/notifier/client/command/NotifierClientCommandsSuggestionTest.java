package org.bruneel.notifier.client.command;

import net.minecraft.util.Identifier;
import org.bruneel.notifier.client.detect.DetectionKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifierClientCommandsSuggestionTest {
	@Test
	void suggestedIdsForEntityKindContainsHorse() {
		Iterable<Identifier> result = NotifierClientCommands.suggestedIdsForKind(
			DetectionKind.ENTITY,
			java.util.List.of(Identifier.of("minecraft:horse")),
			java.util.List.of(Identifier.of("minecraft:stone"))
		);
		boolean containsHorse = contains(result, "minecraft:horse");
		assertTrue(containsHorse);
	}

	@Test
	void suggestedIdsForBlockKindContainsStone() {
		Iterable<Identifier> result = NotifierClientCommands.suggestedIdsForKind(
			DetectionKind.BLOCK,
			java.util.List.of(Identifier.of("minecraft:horse")),
			java.util.List.of(Identifier.of("minecraft:stone"))
		);
		boolean containsStone = contains(result, "minecraft:stone");
		assertTrue(containsStone);
	}

	@Test
	void suggestedIdsForUnknownKindReturnsEmpty() {
		boolean hasAny = NotifierClientCommands.suggestedIdsForKind(
			null,
			java.util.List.of(Identifier.of("minecraft:horse")),
			java.util.List.of(Identifier.of("minecraft:stone"))
		).iterator().hasNext();
		assertFalse(hasAny);
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
