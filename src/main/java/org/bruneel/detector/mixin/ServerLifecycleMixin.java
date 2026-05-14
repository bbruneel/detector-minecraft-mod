package org.bruneel.detector.mixin;

import org.bruneel.detector.DetectorMod;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ServerLifecycleMixin {
	@Inject(method = "runServer", at = @At("HEAD"))
	private void detector$onServerRunStart(CallbackInfo ci) {
		DetectorMod.LOGGER.info("Server run loop started");
	}
}
