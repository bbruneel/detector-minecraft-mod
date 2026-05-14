package org.bruneel.detector.client.detect;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScanHighlightStateTest {
	@Test
	void replaceWithScanResultsAssignsColorsByKindAndOreType() {
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
		assertEquals(0.25F, state.activeHighlights(100).get(1).color().red());
		assertEquals(0.9F, state.activeHighlights(100).get(1).color().green());
		assertEquals(0.95F, state.activeHighlights(100).get(1).color().blue());
	}

	@Test
	void replaceWithScanResultsUsesDefaultBlockColorForNonOreBlocks() {
		ScanHighlightState state = new ScanHighlightState(60);
		state.replaceWithScanResults(
			List.of(new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "stone"),
				1,
				2,
				3,
				null,
				null,
				null
			)),
			20
		);

		ScanHighlightState.ScanHighlightColor color = state.activeHighlights(20).getFirst().color();
		assertEquals(0.2F, color.red());
		assertEquals(0.45F, color.green());
		assertEquals(1.0F, color.blue());
	}

	@Test
	void replaceWithScanResultsUsesDeepslateOreColorMapping() {
		ScanHighlightState state = new ScanHighlightState(60);
		state.replaceWithScanResults(
			List.of(new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "deepslate_redstone_ore"),
				1,
				2,
				3,
				null,
				null,
				null
			)),
			20
		);

		ScanHighlightState.ScanHighlightColor color = state.activeHighlights(20).getFirst().color();
		assertEquals(0.95F, color.red());
		assertEquals(0.2F, color.green());
		assertEquals(0.2F, color.blue());
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

	@Test
	void upsertWithScanResultsKeepsExistingHighlightsAndAddsNewOnes() {
		ScanHighlightState state = new ScanHighlightState(60);
		state.upsertWithScanResults(
			List.of(new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "diamond_ore"),
				1,
				2,
				3,
				null,
				null,
				null
			)),
			100
		);

		ScanHighlightState.HighlightBatchResult result = state.upsertWithScanResults(
			List.of(new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "gold_ore"),
				4,
				5,
				6,
				null,
				null,
				null
			)),
			101
		);

		assertEquals(1, result.total());
		assertEquals(0, result.entities());
		assertEquals(1, result.blocks());
		assertEquals(2, state.activeHighlights(101).size());
	}
}
