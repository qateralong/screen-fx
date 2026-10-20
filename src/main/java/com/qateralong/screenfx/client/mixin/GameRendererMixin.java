package com.qateralong.screenfx.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.qateralong.screenfx.client.CameraFx;
import com.qateralong.screenfx.client.PostFx;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;bindWrite(Z)V", ordinal = 0))
    private void screenfx$post(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PostFx.process(deltaTracker.getGameTimeDeltaTicks());
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void screenfx$camera(GameRenderer renderer, PoseStack pose, float partialTick, Operation<Void> original) {
        original.call(renderer, pose, partialTick);
        CameraFx.apply(pose, partialTick);
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void screenfx$fov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> cir) {
        if (useFovSetting) {
            cir.setReturnValue(CameraFx.fov(cir.getReturnValue()));
        }
    }
}
