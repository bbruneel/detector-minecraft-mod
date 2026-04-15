package org.bruneel.notifier.client.detect;

import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

public final class BlockScanner {
	private BlockScanner() {
	}

	public static int countNearby(ClientWorld world, ClientPlayerEntity player, DetectionTarget target) {
		if (!Registries.BLOCK.containsId(target.id())) {
			return 0;
		}
		Block block = Registries.BLOCK.get(target.id());

		int radius = (int) Math.floor(target.radius());
		int matches = 0;
		BlockPos center = player.getBlockPos();

		for (int x = -radius; x <= radius; x++) {
			for (int y = -radius; y <= radius; y++) {
				for (int z = -radius; z <= radius; z++) {
					BlockPos pos = center.add(x, y, z);
					if (!world.isChunkLoaded(pos)) {
						continue;
					}
					if (world.getBlockState(pos).isOf(block)) {
						matches++;
					}
				}
			}
		}
		return matches;
	}
}
