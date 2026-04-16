package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanHighlightStateTest {
	@Test
	void replaceWithScanResultsAssignsColorsByKind() {
		ScanHighlightState state = new ScanHighlightState(60);
		List<DetectionScanHit> hits = List.of(
			new DetectionScanHit(
				DetectionKind.ENTITY,
				Identifier.of("minecraft", "horse"),
				10,
				64,
				10,
				3.0,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				1
			),
			new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "diamond_ore"),
				11,
				12,
				11,
				null,
				null,
				null
			)
		);

		ScanHighlightState.HighlightBatchResult result = state.replaceWithScanResults(hits, 100);

		assertEquals(2, result.total());
		assertEquals(1, result.entities());
		assertEquals(1, result.blocks());
		assertEquals(60, result.ttlTicks());
		assertEquals(2, state.activeHighlights(100).size());
		assertEquals(1.0F, state.activeHighlights(100).get(0).color().red());
		assertEquals(1.0F, state.activeHighlights(100).get(1).color().blue());
	}

	@Test
	void activeHighlightsPrunesEntriesAfterExpiryTick() {
		ScanHighlightState state = new ScanHighlightState(5);
		state.replaceWithScanResults(
			List.of(new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "gold_ore"),
				1,
				2,
				3,
				null,
				null,
				null
			)),
			10
		);

		assertEquals(1, state.activeHighlights(14).size());
		assertEquals(0, state.activeHighlights(15).size());
	}
}
