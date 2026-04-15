package org.bruneel.notifier.client.detect;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.Box;

public final class EntityScanner {
	private EntityScanner() {
	}

	public static int countNearby(ClientWorld world, ClientPlayerEntity player, DetectionTarget target) {
		if (!Registries.ENTITY_TYPE.containsId(target.id())) {
			return 0;
		}
		EntityType<?> type = Registries.ENTITY_TYPE.get(target.id());

		Box searchArea = player.getBoundingBox().expand(target.radius());
		return world.getEntitiesByClass(Entity.class, searchArea, entity -> entity.getType() == type).size();
	}
}
