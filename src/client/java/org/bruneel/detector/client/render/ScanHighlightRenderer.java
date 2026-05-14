package org.bruneel.detector.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.bruneel.detector.client.detect.DetectionKind;
import org.bruneel.detector.client.detect.ScanHighlightState;

import java.util.List;

/**
 * Renders {@link ScanHighlightState.ScanHighlight} overlays (currently box outlines).
 */
public final class ScanHighlightRenderer {
	public void render(
		WorldRenderContext context,
		ClientWorld world,
		List<ScanHighlightState.ScanHighlight> highlights,
		Vec3d cameraPos,
		float tickDelta
	) {
		MatrixStack matrices = context.matrices();
		VertexConsumer lines = context.consumers().getBuffer(NoDepthLineLayer.LINES_NO_DEPTH);

		for (ScanHighlightState.ScanHighlight highlight : highlights) {
			Box outline;
			if (highlight.kind() == DetectionKind.BLOCK) {
				Box shifted = highlight.box().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
				outline = shifted.expand(0.002D);
			} else {
				Entity entity = resolveEntity(world, highlight);
				if (entity == null) {
					continue;
				}

				Vec3d lerpedPos = entity.getLerpedPos(tickDelta);
				Vec3d currentPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
				Vec3d delta = lerpedPos.subtract(currentPos);

				Box box = entity.getBoundingBox().offset(delta.x, delta.y, delta.z).expand(0.05D);
				outline = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
			}

			ScanHighlightState.ScanHighlightColor color = highlight.color();
			drawBoxLines(matrices, lines, outline, color);
		}
	}

	private static Entity resolveEntity(ClientWorld world, ScanHighlightState.ScanHighlight highlight) {
		Integer entityId = highlight.entityId();
		if (entityId == null) {
			return null;
		}

		Entity entity = world.getEntityById(entityId);
		if (entity == null) {
			return null;
		}
		if (highlight.entityUuid() != null && !highlight.entityUuid().equals(entity.getUuid())) {
			return null;
		}
		return entity;
	}

	private static void drawBoxLines(
		MatrixStack matrices,
		VertexConsumer lines,
		Box box,
		ScanHighlightState.ScanHighlightColor color
	) {
		var entry = matrices.peek();
		float r = color.red();
		float g = color.green();
		float b = color.blue();
		float a = color.alpha();

		emitLine(lines, entry, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, r, g, b, a);

		emitLine(lines, entry, box.maxX, box.minY, box.maxZ, box.maxX, box.minY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);

		emitLine(lines, entry, box.maxX, box.maxY, box.minZ, box.maxX, box.minY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);

		emitLine(lines, entry, box.minX, box.maxY, box.maxZ, box.minX, box.minY, box.maxZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, r, g, b, a);
		emitLine(lines, entry, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, r, g, b, a);
	}

	private static void emitLine(
		VertexConsumer lines,
		MatrixStack.Entry entry,
		double sx, double sy, double sz,
		double ex, double ey, double ez,
		float r, float g, float b, float a
	) {
		float nx = (float) (ex - sx);
		float ny = (float) (ey - sy);
		float nz = (float) (ez - sz);
		float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (length <= 1.0e-6F) {
			return;
		}
		nx /= length;
		ny /= length;
		nz /= length;

		lines.vertex(entry, (float) sx, (float) sy, (float) sz)
			.color(r, g, b, a)
			.normal(entry, nx, ny, nz)
			.lineWidth(2.0F);
		lines.vertex(entry, (float) ex, (float) ey, (float) ez)
			.color(r, g, b, a)
			.normal(entry, nx, ny, nz)
			.lineWidth(2.0F);
	}
}

