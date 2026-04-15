package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;
import org.bruneel.notifier.client.command.DetectionCommandParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierParsingTest {
	@Test
	void parseKindAcceptsCaseInsensitiveKinds() {
		assertEquals(DetectionKind.ENTITY, DetectionCommandParser.parseKind("entity"));
		assertEquals(DetectionKind.BLOCK, DetectionCommandParser.parseKind("BLOCK"));
	}

	@Test
	void parseKindRejectsUnknownValue() {
		assertThrows(IllegalArgumentException.class, () -> DetectionCommandParser.parseKind("mob"));
	}

	@Test
	void parseIdentifierRejectsInvalidId() {
		assertThrows(RuntimeException.class, () -> DetectionCommandParser.parseIdentifier("bad id"));
	}

	@Test
	void parseIdentifierAcceptsNamespacedIds() {
		Identifier id = DetectionCommandParser.parseIdentifier("minecraft:horse");
		assertEquals("minecraft:horse", id.toString());
	}
}
