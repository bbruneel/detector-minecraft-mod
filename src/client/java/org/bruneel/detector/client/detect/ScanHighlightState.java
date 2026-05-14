package org.bruneel.detector.client.detect;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.bruneel.detector.DetectorMod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class ScanHighlightState {
	public static final int DEFAULT_TTL_TICKS = 1200;
	private static final Map<String, ScanHighlightColor> ORE_COLOR_BY_SUFFIX = Map.ofEntries(
		Map.entry("diamond_ore", new ScanHighlightColor(0.25F, 0.9F, 0.95F, 1.0F)),
		Map.entry("emerald_ore", new ScanHighlightColor(0.2F, 0.95F, 0.35F, 1.0F)),
		Map.entry("gold_ore", new ScanHighlightColor(1.0F, 0.84F, 0.2F, 1.0F)),
		Map.entry("iron_ore", new ScanHighlightColor(0.84F, 0.62F, 0.45F, 1.0F)),
		Map.entry("copper_ore", new ScanHighlightColor(0.88F, 0.5F, 0.3F, 1.0F)),
		Map.entry("coal_ore", new ScanHighlightColor(0.35F, 0.35F, 0.35F, 1.0F)),
		Map.entry("redstone_ore", new ScanHighlightColor(0.95F, 0.2F, 0.2F, 1.0F)),
		Map.entry("lapis_ore", new ScanHighlightColor(0.2F, 0.45F, 0.95F, 1.0F)),
		Map.entry("nether_quartz_ore", new ScanHighlightColor(0.92F, 0.92F, 0.92F, 1.0F)),
		Map.entry("nether_gold_ore", new ScanHighlightColor(1.0F, 0.75F, 0.15F, 1.0F)),
		Map.entry("ancient_debris", new ScanHighlightColor(0.6F, 0.35F, 0.25F, 1.0F))
	);

	private final int ttlTicks;
	private final List<ScanHighlight> highlights = new ArrayList<>();

	public ScanHighlightState() {
		this(DEFAULT_TTL_TICKS);
	}

	public ScanHighlightState(int ttlTicks) {
		if (ttlTicks < 1) {
			throw new IllegalArgumentException("ttlTicks must be > 0");
		}
		this.ttlTicks = ttlTicks;
	}

	public HighlightBatchResult replaceWithScanResults(List<DetectionScanHit> hits, long worldTime) {
		Objects.requireNonNull(hits, "hits");
		List<ScanHighlight> next = buildHighlights(hits, worldTime);
		int entities = 0;
		int blocks = 0;
		for (ScanHighlight highlight : next) {
			if (highlight.kind() == DetectionKind.ENTITY) {
				entities++;
			} else if (highlight.kind() == DetectionKind.BLOCK) {
				blocks++;
			}
		}

		highlights.clear();
		highlights.addAll(next);
		return new HighlightBatchResult(next.size(), entities, blocks, ttlTicks);
	}

	public HighlightBatchResult upsertWithScanResults(List<DetectionScanHit> hits, long worldTime) {
		Objects.requireNonNull(hits, "hits");
		pruneExpired(worldTime);

		List<ScanHighlight> next = buildHighlights(hits, worldTime);
		int entities = 0;
		int blocks = 0;
		for (ScanHighlight highlight : next) {
			highlights.removeIf(existing -> sameHighlight(existing, highlight));
			highlights.add(highlight);
			if (highlight.kind() == DetectionKind.ENTITY) {
				entities++;
			} else if (highlight.kind() == DetectionKind.BLOCK) {
				blocks++;
			}
		}

		return new HighlightBatchResult(next.size(), entities, blocks, ttlTicks);
	}

	public List<ScanHighlight> activeHighlights(long worldTime) {
		pruneExpired(worldTime);
		return List.copyOf(highlights);
	}

	public int pruneExpired(long worldTime) {
		int removed = 0;
		Iterator<ScanHighlight> iterator = highlights.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().expiresAtTick() <= worldTime) {
				iterator.remove();
				removed++;
			}
		}
		return removed;
	}

	private List<ScanHighlight> buildHighlights(List<DetectionScanHit> hits, long worldTime) {
		List<ScanHighlight> next = new ArrayList<>(hits.size());
		long expiresAt = worldTime + ttlTicks;
		for (DetectionScanHit hit : hits) {
			ScanHighlightColor color = colorFor(hit);
			if (hit.kind() != DetectionKind.ENTITY && hit.kind() != DetectionKind.BLOCK) {
				DetectorMod.LOGGER.warn("Skipping unknown highlight kind={} for id={}", hit.kind(), hit.id());
				continue;
			}
			BlockPos origin = new BlockPos(hit.x(), hit.y(), hit.z());
			next.add(new ScanHighlight(
				hit.kind(),
				hit.id().toString(),
				new Box(origin),
				hit.entityUuid(),
				hit.entityId(),
				color,
				expiresAt
			));
		}
		return next;
	}

	private static boolean sameHighlight(ScanHighlight a, ScanHighlight b) {
		return a.kind() == b.kind()
			&& Objects.equals(a.targetId(), b.targetId())
			&& Objects.equals(a.entityUuid(), b.entityUuid())
			&& Objects.equals(a.entityId(), b.entityId())
			&& Double.compare(a.box().minX, b.box().minX) == 0
			&& Double.compare(a.box().minY, b.box().minY) == 0
			&& Double.compare(a.box().minZ, b.box().minZ) == 0
			&& Double.compare(a.box().maxX, b.box().maxX) == 0
			&& Double.compare(a.box().maxY, b.box().maxY) == 0
			&& Double.compare(a.box().maxZ, b.box().maxZ) == 0;
	}

	private static ScanHighlightColor colorFor(DetectionScanHit hit) {
		return switch (hit.kind()) {
			case ENTITY -> ScanHighlightColor.entityRed();
			case BLOCK -> blockColorFor(hit.id().getPath());
		};
	}

	private static ScanHighlightColor blockColorFor(String blockPath) {
		if (blockPath == null) {
			return ScanHighlightColor.blockBlue();
		}
		ScanHighlightColor mapped = ORE_COLOR_BY_SUFFIX.get(blockPath);
		if (mapped != null) {
			return mapped;
		}
		if (blockPath.startsWith("deepslate_")) {
			mapped = ORE_COLOR_BY_SUFFIX.get(blockPath.substring("deepslate_".length()));
			if (mapped != null) {
				return mapped;
			}
		}
		return ScanHighlightColor.blockBlue();
	}

	public record HighlightBatchResult(int total, int entities, int blocks, int ttlTicks) {
	}

	public record ScanHighlight(
		DetectionKind kind,
		String targetId,
		Box box,
		UUID entityUuid,
		Integer entityId,
		ScanHighlightColor color,
		long expiresAtTick
	) {
		public Vec3d min() {
			return new Vec3d(box.minX, box.minY, box.minZ);
		}

		public Vec3d max() {
			return new Vec3d(box.maxX, box.maxY, box.maxZ);
		}
	}

	public record ScanHighlightColor(float red, float green, float blue, float alpha) {
		public static ScanHighlightColor entityRed() {
			return new ScanHighlightColor(1.0F, 0.15F, 0.15F, 1.0F);
		}

		public static ScanHighlightColor blockBlue() {
			return new ScanHighlightColor(0.2F, 0.45F, 1.0F, 1.0F);
		}
	}
}
