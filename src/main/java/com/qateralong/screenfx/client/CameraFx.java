package com.qateralong.screenfx.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qateralong.screenfx.FxType;
import net.minecraft.Util;

public final class CameraFx {
    private CameraFx() {
    }

    private static float wobble(float t, float seed) {
        return (float) (Math.sin(t * 1.0 + seed) * 0.5 + Math.sin(t * 2.31 + seed * 1.7) * 0.3 + Math.sin(t * 4.67 + seed * 2.9) * 0.2);
    }

    public static void apply(PoseStack pose, float partialTick) {
        float roll = 0.0F;
        float pitch = 0.0F;
        float yaw = 0.0F;
        float t = (Util.getMillis() % 1_000_000L) / 1000.0F;
        boolean any = false;
        for (ActiveFx fx : FxManager.snapshot()) {
            if (fx.type.kind != FxType.Kind.CAMERA) {
                continue;
            }
            float s = fx.strength();
            if (s <= 0.001F) {
                continue;
            }
            any = true;
            switch (fx.type) {
                case SHAKE -> {
                    float fast = t * 28.0F;
                    yaw += wobble(fast, 1.3F) * 2.4F * s;
                    pitch += wobble(fast, 7.1F) * 2.4F * s;
                    roll += wobble(fast, 3.7F) * 1.6F * s;
                }
                case DRUNK -> {
                    roll += (float) Math.sin(t * 1.1) * 8.0F * s;
                    pitch += (float) Math.sin(t * 0.8 + 0.5) * 3.0F * s;
                    yaw += (float) Math.sin(t * 0.6 + 1.0) * 5.0F * s;
                }
                case SPIN -> roll += fx.age() * 120.0F * fx.intensity * fx.envelope();
                case UPSIDEDOWN -> roll += 180.0F * Math.min(1.0F, s);
                default -> {
                }
            }
        }
        if (!any) {
            return;
        }
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    public static double fov(double fov) {
        float zoom = FxManager.strength(FxType.ZOOM);
        float wide = FxManager.strength(FxType.WIDE);
        double result = fov;
        if (zoom > 0.0F) {
            result /= 1.0 + zoom * 3.0;
        }
        if (wide > 0.0F) {
            result *= 1.0 + wide * 0.6;
        }
        return Math.max(1.0, Math.min(175.0, result));
    }
}
