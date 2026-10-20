package com.qateralong.screenfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qateralong.screenfx.FxType;
import com.qateralong.screenfx.ScreenFx;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public final class Overlays {
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "textures/misc/vignette.png");
    private static final ResourceLocation NOISE = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "noise");
    private static final int NOISE_W = 192;
    private static final int NOISE_H = 108;
    private static DynamicTexture noise;
    private static final Random RANDOM = new Random();
    private static long lastNoise;

    private Overlays() {
    }

    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        render(g);
    }

    public static void render(GuiGraphics g) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        for (ActiveFx fx : FxManager.snapshot()) {
            if (fx.type.kind != FxType.Kind.OVERLAY) {
                continue;
            }
            float s = fx.strength();
            if (s <= 0.001F) {
                continue;
            }
            switch (fx.type) {
                case BRIGHTEN -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    g.fill(0, 0, w, h, argb(Math.min(1.0F, s * 0.6F), fx.color));
                    g.flush();
                    RenderSystem.defaultBlendFunc();
                }
                case VIGNETTE -> vignette(g, w, h, fx.color, Math.min(1.0F, s));
                case HEARTBEAT -> {
                    float t = (Util.getMillis() % 1000L) / 1000.0F;
                    float beat = (float) (Math.exp(-Math.pow((t - 0.08) * 14.0, 2.0)) + 0.6 * Math.exp(-Math.pow((t - 0.32) * 14.0, 2.0)));
                    vignette(g, w, h, fx.color, Math.min(1.0F, s * (0.3F + 0.7F * beat)));
                }
                case LETTERBOX -> {
                    int bar = Math.round(h * 0.12F * Math.min(1.0F, s));
                    g.fill(0, 0, w, bar, 0xFF000000 | fx.color);
                    g.fill(0, h - bar, w, h, 0xFF000000 | fx.color);
                }
                case STATIC -> staticNoise(g, w, h, Math.min(1.0F, s));
                default -> g.fill(0, 0, w, h, argb(Math.min(1.0F, s), fx.color));
            }
        }
    }

    private static void vignette(GuiGraphics g, int w, int h, int color, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, alpha);
        g.blit(VIGNETTE, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void staticNoise(GuiGraphics g, int w, int h, float alpha) {
        if (noise == null) {
            noise = new DynamicTexture(NOISE_W, NOISE_H, false);
            Minecraft.getInstance().getTextureManager().register(NOISE, noise);
        }
        long now = Util.getMillis();
        if (now - lastNoise > 40L) {
            lastNoise = now;
            NativeImage image = noise.getPixels();
            if (image != null) {
                for (int y = 0; y < NOISE_H; y++) {
                    boolean band = RANDOM.nextInt(40) == 0;
                    for (int x = 0; x < NOISE_W; x++) {
                        int v = RANDOM.nextInt(256);
                        if (band) {
                            v = Math.min(255, v + 80);
                        }
                        image.setPixelRGBA(x, y, (RANDOM.nextInt(160) + 95) << 24 | v << 16 | v << 8 | v);
                    }
                }
                noise.upload();
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(NOISE, 0, 0, w, h, 0.0F, 0.0F, NOISE_W, NOISE_H, NOISE_W, NOISE_H);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static int argb(float alpha, int rgb) {
        return (Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }
}
