package org.bruneel.notifier.mixin;

import org.bruneel.notifier.NotifierMod;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class ServerLifecycleMixin {
	@Inject(method = "runServer", at = @At("HEAD"))
	private void notifier$onServerRunStart(CallbackInfo ci) {
		NotifierMod.LOGGER.info("Server run loop started");
	}
}
