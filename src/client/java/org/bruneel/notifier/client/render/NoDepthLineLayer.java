package org.bruneel.notifier.client.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import org.bruneel.notifier.NotifierMod;

public final class NoDepthLineLayer {
	private NoDepthLineLayer() {
	}

	public static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
		NotifierMod.MOD_ID + "_lines_no_depth",
		RenderSetup.builder(RenderPipelines.LINES)
			.layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);
}
