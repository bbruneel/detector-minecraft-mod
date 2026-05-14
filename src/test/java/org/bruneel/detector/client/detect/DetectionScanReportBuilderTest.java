package org.bruneel.detector.client.detect;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionScanReportBuilderTest {
	@Test
	void buildFormatsEntityAndBlockResultsWithCoordinates() {
		List<DetectionScanHit> hits = List.of(
			new DetectionScanHit(
				DetectionKind.ENTITY,
				Identifier.of("minecraft", "horse"),
				10,
				64,
				-3,
				4.24,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				1
			),
			new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "diamond_ore"),
				11,
				12,
				-4,
				null,
				null,
				null
			)
		);

		DetectionScanReportBuilder.ScanReport report = DetectionScanReportBuilder.build(hits, 2, 0, 20, 120);

		assertEquals(2, report.totalMatches());
		assertEquals(2, report.shownMatches());
		assertTrue(report.chatLines().stream().anyMatch(line -> line.contains("entity minecraft:horse at 10 64 -3 dist=4.2")));
		assertTrue(report.chatLines().stream().anyMatch(line -> line.contains("block minecraft:diamond_ore at 11 12 -4")));
	}

	@Test
	void buildAppliesPerTargetAndTotalOutputCaps() {
		List<DetectionScanHit> hits = List.of(
			new DetectionScanHit(
				DetectionKind.ENTITY,
				Identifier.of("minecraft", "horse"),
				0,
				64,
				0,
				1.0,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				1
			),
			new DetectionScanHit(
				DetectionKind.ENTITY,
				Identifier.of("minecraft", "horse"),
				1,
				64,
				0,
				2.0,
				UUID.fromString("00000000-0000-0000-0000-000000000002"),
				2
			),
			new DetectionScanHit(
				DetectionKind.ENTITY,
				Identifier.of("minecraft", "horse"),
				2,
				64,
				0,
				3.0,
				UUID.fromString("00000000-0000-0000-0000-000000000003"),
				3
			),
			new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "diamond_ore"),
				3,
				11,
				0,
				null,
				null,
				null
			),
			new DetectionScanHit(
				DetectionKind.BLOCK,
				Identifier.of("minecraft", "diamond_ore"),
				4,
				11,
				0,
				null,
				null,
				null
			)
		);

		DetectionScanReportBuilder.ScanReport report = DetectionScanReportBuilder.build(hits, 2, 0, 2, 3);

		assertEquals(5, report.totalMatches());
		assertEquals(3, report.shownMatches());
		assertEquals(2, report.truncatedMatches());
		assertTrue(report.chatLines().stream().anyMatch(line -> line.contains("output truncated, 2 additional matches")));
	}

	@Test
	void buildReportsInvalidConfiguredTargets() {
		DetectionScanReportBuilder.ScanReport report = DetectionScanReportBuilder.build(List.of(), 1, 1, 20, 120);

		assertEquals(0, report.totalMatches());
		assertEquals(1, report.invalidTargets());
		assertTrue(report.chatLines().stream().anyMatch(line -> line.contains("skipped 1 invalid target id(s)")));
	}

	@Test
	void registryEnabledTargetsCanDriveScanFiltering() {
		TargetRegistry registry = new TargetRegistry();
		registry.upsert(new DetectionTarget(
			DetectionKind.ENTITY,
			Identifier.of("minecraft", "horse"),
			true,
			16.0,
			10,
			300,
			100,
			"horse"
		));
		registry.upsert(new DetectionTarget(
			DetectionKind.BLOCK,
			Identifier.of("minecraft", "diamond_ore"),
			false,
			8.0,
			30,
			360,
			120,
			"ore"
		));

		assertEquals(1, registry.enabledTargets().size());
		assertEquals("minecraft:horse", registry.enabledTargets().getFirst().id().toString());
	}
}
