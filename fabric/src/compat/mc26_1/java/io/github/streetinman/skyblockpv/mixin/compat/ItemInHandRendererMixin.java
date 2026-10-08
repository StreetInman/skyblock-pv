package io.github.streetinman.skyblockpv.mixin.compat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.entity.HumanoidArm;

import io.github.streetinman.skyblockpv.SkyblockPvClient;
import io.github.streetinman.skyblockpv.config.PvConfig;

/** First-person item position, rotation and size, and the option to skip the equip dip. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
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
		pose.mulPose(Axis.XP.rotationDegrees((float) a.rotationX));
		pose.mulPose(Axis.YP.rotationDegrees((float) (side * a.rotationY)));
		pose.mulPose(Axis.ZP.rotationDegrees((float) (side * a.rotationZ)));
		float scale = (float) a.scale;
		pose.scale(scale, scale, scale);
	}
}
