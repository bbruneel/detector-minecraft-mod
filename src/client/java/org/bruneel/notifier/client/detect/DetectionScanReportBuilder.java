package org.bruneel.notifier.client.detect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class DetectionScanReportBuilder {
	private DetectionScanReportBuilder() {
	}

	public static ScanReport build(
		List<DetectionScanHit> hits,
		int scannedTargets,
		int invalidTargets,
		int perTargetLimit,
		int totalLimit
	) {
		Objects.requireNonNull(hits, "hits");
		if (perTargetLimit < 1) {
			throw new IllegalArgumentException("perTargetLimit must be > 0");
		}
		if (totalLimit < 1) {
			throw new IllegalArgumentException("totalLimit must be > 0");
		}

		Map<String, List<DetectionScanHit>> grouped = new LinkedHashMap<>();
		hits.stream()
			.sorted(hitComparator())
			.forEach(hit -> grouped.computeIfAbsent(hit.key(), ignored -> new ArrayList<>()).add(hit));

		List<String> details = new ArrayList<>();
		int shownMatches = 0;

		for (List<DetectionScanHit> byTarget : grouped.values()) {
			int shownForTarget = 0;
			for (DetectionScanHit hit : byTarget) {
				if (shownForTarget >= perTargetLimit || shownMatches >= totalLimit) {
					break;
				}
				details.add(formatHit(hit));
				shownForTarget++;
				shownMatches++;
			}
			if (shownMatches >= totalLimit) {
				break;
			}
		}

		int totalMatches = hits.size();
		int truncatedMatches = Math.max(0, totalMatches - shownMatches);

		List<String> chatLines = new ArrayList<>();
		chatLines.add("notifier: scan complete (targets=" + scannedTargets + ", matches=" + totalMatches + ")");
		if (details.isEmpty()) {
			chatLines.add("notifier: no tracked entities or blocks found nearby.");
		} else {
			chatLines.addAll(details);
		}
		if (truncatedMatches > 0) {
			chatLines.add("notifier: output truncated, " + truncatedMatches + " additional matches not shown.");
		}
		if (invalidTargets > 0) {
			chatLines.add("notifier: skipped " + invalidTargets + " invalid target id(s); check /notifier detect list.");
		}

		return new ScanReport(chatLines, totalMatches, shownMatches, truncatedMatches, invalidTargets);
	}

	private static Comparator<DetectionScanHit> hitComparator() {
		return Comparator
			.comparing((DetectionScanHit hit) -> hit.kind().name())
			.thenComparing(hit -> hit.id().toString())
			.thenComparing(hit -> hit.distance() == null ? Double.MAX_VALUE : hit.distance())
			.thenComparingInt(DetectionScanHit::x)
			.thenComparingInt(DetectionScanHit::y)
			.thenComparingInt(DetectionScanHit::z);
	}

	private static String formatHit(DetectionScanHit hit) {
		String base = "notifier: " + hit.kind().name().toLowerCase(Locale.ROOT)
			+ " " + hit.id()
			+ " at " + hit.x() + " " + hit.y() + " " + hit.z();
		if (hit.distance() == null) {
			return base;
		}
		return base + " dist=" + String.format(Locale.ROOT, "%.1f", hit.distance());
	}

	public record ScanReport(
		List<String> chatLines,
		int totalMatches,
		int shownMatches,
		int truncatedMatches,
		int invalidTargets
	) {
	}
}
