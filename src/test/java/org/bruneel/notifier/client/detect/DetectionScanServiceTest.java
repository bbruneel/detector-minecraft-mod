package org.bruneel.notifier.client.detect;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DetectionScanServiceTest {
	@Test
	void scanExecutionExposesHitsAlongsideReport() {
		List<DetectionScanHit> hits = List.of(
			new DetectionScanHit(
				DetectionKind.ENTITY,
				Identifier.of("minecraft", "horse"),
				5,
				70,
				-1,
				2.3,
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				1
			)
		);
		DetectionScanReportBuilder.ScanReport report = DetectionScanReportBuilder.build(hits, 1, 0, 20, 120);

		DetectionScanService.ScanExecution execution = new DetectionScanService.ScanExecution(1, hits, report);

		assertEquals(1, execution.enabledTargets());
		assertEquals(1, execution.hits().size());
		assertEquals(report, execution.report());
	}
}
