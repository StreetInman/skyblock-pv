package io.github.streetinman.skyblockpv.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.config.PvConfig;

/**
 * Player model size. Only the render state is scaled, after the game has filled it in, so the
 * real hitbox, movement and everything the server sees stay the same.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void skyblockpv$scalePlayer(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		PvConfig.PlayerModel settings = SkyblockPvClient.config().playerModel;
		if (settings.scale == 1.0 || !(entity instanceof Player)) return;
		if (entity == Minecraft.getInstance().player || settings.includeOtherPlayers) {
			state.scale *= (float) settings.scale;
		}
	}
}
