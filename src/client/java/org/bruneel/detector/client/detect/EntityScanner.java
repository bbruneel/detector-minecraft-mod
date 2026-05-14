package org.bruneel.detector.client.detect;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public final class EntityScanner {
	private EntityScanner() {
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
		if (!Registries.ENTITY_TYPE.containsId(target.id())) {
			return List.of();
		}
		EntityType<?> type = Registries.ENTITY_TYPE.get(target.id());

		Box searchArea = player.getBoundingBox().expand(target.radius());
		List<Entity> entities = world.getEntitiesByClass(Entity.class, searchArea, entity -> entity.getType() == type);
		List<DetectionScanHit> hits = new ArrayList<>();
		for (Entity entity : entities) {
			if (hits.size() >= limit) {
				break;
			}
			BlockPos pos = entity.getBlockPos();
			hits.add(new DetectionScanHit(
				DetectionKind.ENTITY,
				target.id(),
				pos.getX(),
				pos.getY(),
				pos.getZ(),
				(double) player.distanceTo(entity),
				entity.getUuid(),
				entity.getId()
			));
		}
		return hits;
	}
}
