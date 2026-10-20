package com.qateralong.screenfx.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.qateralong.screenfx.FxType;

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
        float dx = 0.0F;
        float dy = 0.0F;
        boolean any = false;
        for (ActiveFx fx : FxManager.current()) {
            if (fx.type.kind != FxType.Kind.CAMERA) {
                continue;
            }
            float s = fx.strength();
            if (s <= 0.001F) {
                continue;
            }
            any = true;
            float t = fx.time();
            switch (fx.type) {
                case SHAKE -> {
                    float fast = t * 28.0F;
                    yaw += wobble(fast, fx.seed) * 2.4F * s;
                    pitch += wobble(fast, fx.seed + 7.1F) * 2.4F * s;
                    roll += wobble(fast, fx.seed + 3.7F) * 1.6F * s;
                }
                case DRUNK -> {
                    roll += (float) Math.sin(t * 1.1) * 8.0F * s;
                    pitch += (float) Math.sin(t * 0.8 + 0.5) * 3.0F * s;
                    yaw += (float) Math.sin(t * 0.6 + 1.0) * 5.0F * s;
                }
                case SPIN -> roll += t * 120.0F * fx.intensity * fx.envelope();
                case UPSIDEDOWN -> roll += 180.0F * Math.min(1.0F, s);
                case SWAY -> {
                    roll += (float) Math.sin(t * 0.55 + fx.seed) * 3.5F * s;
                    yaw += (float) Math.sin(t * 0.37 + fx.seed * 0.5F) * 2.0F * s;
                    pitch += (float) Math.sin(t * 0.29) * 1.2F * s;
                }
                case TILT -> roll += fx.number("angle", 25.0F) * s;
                case BOB -> {
                    dy += (float) Math.sin(t * 6.0) * 0.06F * s;
                    dx += (float) Math.cos(t * 3.0) * 0.04F * s;
                }
                case EARTHQUAKE -> {
                    float slow = t * 9.0F;
                    yaw += wobble(slow, fx.seed) * 1.2F * s;
                    pitch += wobble(slow, fx.seed + 4.2F) * 1.5F * s;
                    roll += wobble(slow, fx.seed + 9.3F) * 0.8F * s;
                    dy += wobble(t * 13.0F, fx.seed + 2.0F) * 0.08F * s;
                }
                case KICK -> {
                    float age = fx.age() * fx.speed();
                    float curve = age < 0.05F ? age / 0.05F : (float) Math.exp(-(age - 0.05F) * 6.0F);
                    pitch -= 7.0F * s * curve;
                    roll += (fx.seed % 2.0F < 1.0F ? 1.0F : -1.0F) * 2.5F * s * curve;
                }
                case CAMERA -> {
                    roll += fx.number("roll", 0.0F) * s;
                    pitch += fx.number("pitch", 0.0F) * s;
                    yaw += fx.number("yaw", 0.0F) * s;
                    float shake = fx.number("shake", 0.0F);
                    if (shake > 0.0F) {
                        float fast = fx.age() * fx.number("frequency", 20.0F);
                        yaw += wobble(fast, fx.seed) * shake * s;
                        pitch += wobble(fast, fx.seed + 7.1F) * shake * s;
                    }
                }
                default -> {
                }
            }
        }
        if (!any) {
            return;
        }
        if (dx != 0.0F || dy != 0.0F) {
            pose.translate(dx, dy, 0.0F);
        }
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
    }

    public static double fov(double fov) {
        double result = fov;
        for (ActiveFx fx : FxManager.current()) {
            if (fx.type.kind != FxType.Kind.CAMERA) {
                continue;
            }
            float s = fx.strength();
            if (s <= 0.001F) {
                continue;
            }
            switch (fx.type) {
                case ZOOM -> result /= 1.0 + s * 3.0;
                case WIDE -> result *= 1.0 + s * 0.6;
                case VERTIGO -> result *= 1.0 + 0.25 * s * Math.sin(fx.time() * 1.5);
                case BREATHE -> result *= 1.0 + 0.035 * s * Math.sin(fx.time() * 1.3);
                case CAMERA -> result *= 1.0 + (fx.number("fov", 1.0F) - 1.0) * Math.min(1.0F, s);
                default -> {
                }
            }
        }
        return Math.max(1.0, Math.min(175.0, result));
    }
}
