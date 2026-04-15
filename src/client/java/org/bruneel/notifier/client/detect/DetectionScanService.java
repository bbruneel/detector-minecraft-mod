package org.bruneel.notifier.client.detect;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import org.bruneel.notifier.NotifierMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DetectionScanService {
	public static final int PER_TARGET_LIMIT = 20;
	public static final int TOTAL_LIMIT = 120;

	private DetectionScanService() {
	}

	public static ScanExecution run(
		ClientWorld world,
		ClientPlayerEntity player,
		List<DetectionTarget> enabledTargets
	) {
		Objects.requireNonNull(world, "world");
		Objects.requireNonNull(player, "player");
		Objects.requireNonNull(enabledTargets, "enabledTargets");

		int invalidTargets = 0;
		List<DetectionScanHit> hits = new ArrayList<>();

		for (DetectionTarget target : enabledTargets) {
			switch (target.kind()) {
				case ENTITY -> {
					if (!Registries.ENTITY_TYPE.containsId(target.id())) {
						invalidTargets++;
						NotifierMod.LOGGER.warn("Detect scan skipped invalid entity id={}", target.id());
						continue;
					}
					hits.addAll(EntityScanner.findNearby(world, player, target, PER_TARGET_LIMIT + 1));
				}
				case BLOCK -> {
					if (!Registries.BLOCK.containsId(target.id())) {
						invalidTargets++;
						NotifierMod.LOGGER.warn("Detect scan skipped invalid block id={}", target.id());
						continue;
					}
					hits.addAll(BlockScanner.findNearby(world, player, target, PER_TARGET_LIMIT + 1));
				}
			}
		}

		DetectionScanReportBuilder.ScanReport report = DetectionScanReportBuilder.build(
			hits,
			enabledTargets.size(),
			invalidTargets,
			PER_TARGET_LIMIT,
			TOTAL_LIMIT
		);
		return new ScanExecution(enabledTargets.size(), report);
	}

	public record ScanExecution(int enabledTargets, DetectionScanReportBuilder.ScanReport report) {
	}
}
