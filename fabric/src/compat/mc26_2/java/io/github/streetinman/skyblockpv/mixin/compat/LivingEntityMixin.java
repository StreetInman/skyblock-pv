package io.github.streetinman.skyblockpv.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.config.PvConfig;

/**
 * Swing animation speed for your own player. This only changes how long the arm animation
 * lasts on your screen: swing packets, attack timing and cooldowns are untouched.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
	private void skyblockpv$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		PvConfig.ItemAnimations a = SkyblockPvClient.config().itemAnimations;
		if (!a.enabled || a.swingSpeed == 1.0 || (Object) this != Minecraft.getInstance().player) return;
		cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValueI() / a.swingSpeed)));
	}
}
