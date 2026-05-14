package org.bruneel.detector.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.render.LayeringTransform;
import net.minecraft.client.render.OutputTarget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.Identifier;
import org.bruneel.detector.DetectorMod;
import org.bruneel.detector.client.mixin.RenderPipelinesAccessor;

public final class NoDepthLineLayer {
	private NoDepthLineLayer() {
	}

	private static final RenderPipeline DETECTOR_LINES_NO_DEPTH_PIPELINE = RenderPipelinesAccessor.detector$register(
		RenderPipeline.builder(RenderPipelinesAccessor.detector$getRenderTypeLinesSnippet())
			.withLocation(Identifier.of(DetectorMod.MOD_ID, "pipeline/lines_no_depth"))
			.withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
			.build()
	);

	public static final RenderLayer LINES_NO_DEPTH = RenderLayer.of(
		DetectorMod.MOD_ID + "_lines_no_depth",
		RenderSetup.builder(DETECTOR_LINES_NO_DEPTH_PIPELINE)
			.layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
			.outputTarget(OutputTarget.MAIN_TARGET)
			.build()
	);
}
