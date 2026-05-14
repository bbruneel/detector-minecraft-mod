package org.bruneel.detector.client.detect;

import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class BlockScanner {
	private BlockScanner() {
	}

	public static int countNearby(ClientWorld world, ClientPlayerEntity player, DetectionTarget target) {
		return findNearby(world, player, target, Integer.MAX_VALUE).size();
	}

	public static List<DetectionScanHit> findNearby(
		ClientWorld world,
		ClientPlayerEntity player,
		DetectionTarget target,
		int limit
	) {
		if (!Registries.BLOCK.containsId(target.id())) {
			return List.of();
		}
		Block block = Registries.BLOCK.get(target.id());
		Block alternateOreVariant = resolveAlternateOreVariant(target.id());

		int radius = (int) Math.floor(target.radius());
		List<DetectionScanHit> hits = new ArrayList<>();
		BlockPos center = player.getBlockPos();

		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = center.add(x, y, z);
					if (!world.isChunkLoaded(pos)) {
						continue;
					}
					if (world.getBlockState(pos).isOf(block)
						|| (alternateOreVariant != null && world.getBlockState(pos).isOf(alternateOreVariant))) {
						hits.add(new DetectionScanHit(
							DetectionKind.BLOCK,
							target.id(),
							pos.getX(),
							pos.getY(),
							pos.getZ(),
							null,
							null,
							null
						));
						if (hits.size() >= limit) {
							return hits;
						}
					}
				}
			}
		}
		return hits;
	}

	private static Block resolveAlternateOreVariant(Identifier id) {
		String path = id.getPath();
		if (!path.endsWith("_ore")) {
			return null;
		}

		String alternatePath;
		if (path.startsWith("deepslate_")) {
			alternatePath = path.substring("deepslate_".length());
		} else {
			alternatePath = "deepslate_" + path;
		}

		Identifier alternateId = Identifier.of(id.getNamespace(), alternatePath);
		if (!Registries.BLOCK.containsId(alternateId)) {
			return null;
		}
		return Registries.BLOCK.get(alternateId);
	}
}
