package io.github.streetinman.skyblockpv.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.world.entity.HumanoidArm;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.config.PvConfig;

/**
 * 26.3 version of the first-person item tweaks. 26.3 replaced ItemInHandRenderer with
 * FirstPersonHandsAndItemsRenderer and swings now affect attack strength, so swing speed is
 * changed only in the rendered animation here (faster only), never in the player's swing timing.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMixin {
	@ModifyVariable(method = "applyItemArmTransform", at = @At("HEAD"), argsOnly = true)
	private float skyblockpv$noEquipAnimation(float equipProgress) {
		PvConfig.ItemAnimations a = SkyblockPvClient.config().itemAnimations;
		return a.enabled && a.noEquipAnimation ? 0 : equipProgress;
	}

	@Inject(method = "applyItemArmTransform", at = @At("TAIL"))
	private void skyblockpv$transformItem(PoseStack pose, HumanoidArm arm, float equipProgress, CallbackInfo ci) {
		PvConfig.ItemAnimations a = SkyblockPvClient.config().itemAnimations;
		if (!a.enabled) return;
		int side = arm == HumanoidArm.RIGHT ? 1 : -1;
		pose.translate(side * a.offsetX, a.offsetY, a.offsetZ);
		pose.rotateDegrees(Axis.XP, (float) a.rotationX);
		pose.rotateDegrees(Axis.YP, (float) (side * a.rotationY));
		pose.rotateDegrees(Axis.ZP, (float) (side * a.rotationZ));
		float scale = (float) a.scale;
		pose.scale(scale, scale, scale);
	}

	/** The third float argument of submitArmWithItem is the swing progress (0 → 1). */
	@ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private float skyblockpv$swingSpeed(float swingProgress) {
		PvConfig.ItemAnimations a = SkyblockPvClient.config().itemAnimations;
		if (!a.enabled || a.swingSpeed <= 1.0 || swingProgress <= 0) return swingProgress;
		float faster = (float) (swingProgress * a.swingSpeed);
		return faster >= 1 ? 0 : faster;
	}
}
