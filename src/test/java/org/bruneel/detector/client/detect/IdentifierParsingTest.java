package org.bruneel.detector.client.detect;

import net.minecraft.util.Identifier;
import org.bruneel.detector.client.command.DetectionCommandParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IdentifierParsingTest {
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
