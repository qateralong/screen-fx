package com.qateralong.screenfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.qateralong.screenfx.FxType;
import com.qateralong.screenfx.ScreenFx;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class Overlays {
    private static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "textures/misc/vignette.png");
    private static final ResourceLocation NOISE = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "noise");
    private static final ResourceLocation GRAIN = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "grain");
    private static final ResourceLocation SOFT_HOLE = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "soft_hole");
    private static final ResourceLocation HARD_HOLE = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "hard_hole");
    private static final ResourceLocation FROST = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "frost");
    private static final ResourceLocation BLOOD = ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "blood");
    private static final int NOISE_W = 192;
    private static final int NOISE_H = 108;
    private static final Random RANDOM = new Random();
    private static final Set<ResourceLocation> GENERATED = new HashSet<>();
    private static DynamicTexture noise;
    private static DynamicTexture grain;
    private static long lastNoise;
    private static long lastGrain;

    private Overlays() {
    }

    public static void renderHud(GuiGraphics g, DeltaTracker delta) {
        if (Minecraft.getInstance().screen == null) {
            render(g, false);
        }
    }

    public static void renderUnderHud(GuiGraphics g) {
        render(g, true);
    }

    public static void render(GuiGraphics g) {
        render(g, false);
    }

    private static void render(GuiGraphics g, boolean worldLayer) {
        g.pose().pushPose();
        g.pose().translate(0.0F, 0.0F, worldLayer ? 0.0F : 8000.0F);
        draw(g, worldLayer);
        g.flush();
        g.pose().popPose();
    }

    private static void draw(GuiGraphics g, boolean worldLayer) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        for (ActiveFx fx : FxManager.current()) {
            if (fx.type.kind != FxType.Kind.OVERLAY || fx.worldLayer() != worldLayer) {
                continue;
            }
            float s = fx.strength();
            if (s <= 0.001F) {
                continue;
            }
            float t = fx.time();
            float a = Math.min(1.0F, s);
            switch (fx.type) {
                case BRIGHTEN -> additive(g, w, h, Math.min(1.0F, s * 0.6F), fx.color);
                case VIGNETTE -> textured(g, VIGNETTE, w, h, fx.color, a, Blend.NORMAL);
                case HEARTBEAT -> {
                    float p = (t % 1.0F);
                    float beat = (float) (Math.exp(-Math.pow((p - 0.08) * 14.0, 2.0)) + 0.6 * Math.exp(-Math.pow((p - 0.32) * 14.0, 2.0)));
                    textured(g, VIGNETTE, w, h, fx.color, Math.min(1.0F, s * (0.3F + 0.7F * beat)), Blend.NORMAL);
                }
                case LETTERBOX -> {
                    int bar = Math.round(h * fx.number("size", 0.12F) * a);
                    g.fill(0, 0, w, bar, 0xFF000000 | fx.color);
                    g.fill(0, h - bar, w, h, 0xFF000000 | fx.color);
                }
                case STATIC -> staticNoise(g, w, h, a);
                case PULSE -> g.fill(0, 0, w, h, argb(a * (0.5F + 0.5F * (float) Math.sin(t * Math.PI * 2.0)), fx.color));
                case STROBE -> {
                    if ((t * 4.0F) % 1.0F < 0.5F) {
                        g.fill(0, 0, w, h, argb(a, fx.color));
                    }
                }
                case BLINK -> {
                    float period = fx.number("period", 3.0F);
                    float phase = t % period;
                    float close = phase < 0.3F ? (float) Math.sin(phase / 0.3F * Math.PI) : 0.0F;
                    lids(g, w, h, close * a, fx.color);
                }
                case EYES -> lids(g, w, h, a, fx.color);
                case TUNNEL -> hole(g, SOFT_HOLE, w, h, Mth.lerp(a, 2.2F, 0.35F) * Math.max(w, h), fx.color, 1.0F);
                case IRIS -> hole(g, HARD_HOLE, w, h, (1.0F - a) * 1.25F * (float) Math.sqrt(w * w + h * h), fx.color, 1.0F);
                case FROST -> {
                    float scale = 1.0F + (1.0F - a) * 0.5F;
                    centered(g, generated(FROST, Overlays::frost), w, h, scale, fx.color, Math.min(1.0F, a * 1.2F), Blend.NORMAL);
                }
                case BLOOD -> textured(g, generated(BLOOD, Overlays::blood), w, h, fx.color, a, Blend.NORMAL);
                case BURN -> {
                    float flicker = 0.75F + 0.25F * (float) (Math.sin(t * 11.0 + fx.seed) * 0.6 + Math.sin(t * 23.0) * 0.4);
                    textured(g, VIGNETTE, w, h, fx.color, Math.min(1.0F, a * flicker), Blend.ADD);
                    textured(g, VIGNETTE, w, h, fx.color, Math.min(1.0F, a * flicker * 0.6F), Blend.NORMAL);
                }
                case SCANLINES -> scanlines(g, w, h, a, fx.color, t);
                case GRAIN -> grain(g, w, h, a);
                case GRADIENT -> gradient(g, w, h, a, fx.color, fx.text("side", "bottom"), fx.number("size", 0.5F));
                case IMAGE -> image(g, fx, w, h, a);
                default -> g.fill(0, 0, w, h, argb(a, fx.color));
            }
        }
    }

    private enum Blend { NORMAL, ADD, MULTIPLY }

    private static void blend(Blend blend) {
        RenderSystem.enableBlend();
        switch (blend) {
            case ADD -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            case MULTIPLY -> RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            default -> RenderSystem.defaultBlendFunc();
        }
    }

    private static void reset(GuiGraphics g) {
        g.flush();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private static void additive(GuiGraphics g, int w, int h, float alpha, int color) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.fill(0, 0, w, h, argb(alpha, color));
        g.flush();
        RenderSystem.defaultBlendFunc();
    }

    private static void setColor(GuiGraphics g, int color, float alpha, Blend blend) {
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float gr = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        if (blend == Blend.MULTIPLY) {
            g.setColor(r * alpha, gr * alpha, b * alpha, alpha);
        } else {
            g.setColor(r, gr, b, alpha);
        }
    }

    private static void textured(GuiGraphics g, ResourceLocation texture, int w, int h, int color, float alpha, Blend blend) {
        blend(blend);
        setColor(g, color, alpha, blend);
        g.blit(texture, 0, 0, w, h, 0.0F, 0.0F, 256, 256, 256, 256);
        reset(g);
    }

    private static void centered(GuiGraphics g, ResourceLocation texture, int w, int h, float scale, int color, float alpha, Blend blend) {
        int sw = Math.round(w * scale);
        int sh = Math.round(h * scale);
        blend(blend);
        setColor(g, color, alpha, blend);
        g.blit(texture, (w - sw) / 2, (h - sh) / 2, sw, sh, 0.0F, 0.0F, 256, 256, 256, 256);
        reset(g);
    }

    private static void hole(GuiGraphics g, ResourceLocation texture, int w, int h, float diameter, int color, float alpha) {
        int d = Math.max(0, Math.round(diameter));
        int x0 = w / 2 - d / 2;
        int y0 = h / 2 - d / 2;
        int fill = argb(alpha, color);
        if (y0 > 0) {
            g.fill(0, 0, w, y0, fill);
        }
        if (y0 + d < h) {
            g.fill(0, y0 + d, w, h, fill);
        }
        if (x0 > 0) {
            g.fill(0, Math.max(0, y0), x0, Math.min(h, y0 + d), fill);
        }
        if (x0 + d < w) {
            g.fill(x0 + d, Math.max(0, y0), w, Math.min(h, y0 + d), fill);
        }
        if (d > 0) {
            blend(Blend.NORMAL);
            setColor(g, color, alpha, Blend.NORMAL);
            g.blit(generated(texture, texture == HARD_HOLE ? Overlays::hardHole : Overlays::softHole), x0, y0, d, d, 0.0F, 0.0F, 256, 256, 256, 256);
            reset(g);
        }
    }

    private static void lids(GuiGraphics g, int w, int h, float close, int color) {
        if (close <= 0.001F) {
            return;
        }
        int solid = 0xFF000000 | color;
        int clear = color & 0xFFFFFF;
        int soft = Math.round(4.0F + 10.0F * (1.0F - close));
        float half = w / 2.0F;
        for (int x = 0; x < w; x += 2) {
            float u = (x + 1 - half) / half;
            float shape = close * (0.6F + 0.4F * u * u) + close * close * close * close * 0.45F;
            int edge = Math.round(h / 2.0F * Math.min(1.0F, shape));
            g.fill(x, 0, x + 2, edge, solid);
            g.fill(x, h - edge, x + 2, h, solid);
            if (edge < h / 2) {
                g.fillGradient(x, edge, x + 2, Math.min(h / 2, edge + soft), solid, clear);
                g.fillGradient(x, Math.max(h / 2, h - edge - soft), x + 2, h - edge, clear, solid);
            }
        }
    }

    private static void scanlines(GuiGraphics g, int w, int h, float alpha, int color, float t) {
        int line = argb(alpha * 0.6F, color);
        for (int y = 0; y < h; y += 2) {
            g.fill(0, y, w, y + 1, line);
        }
        int band = Math.round(h * 0.12F);
        int y = Math.round(((t * 0.25F) % 1.2F - 0.1F) * h);
        g.fillGradient(0, y - band, w, y, 0x00FFFFFF, argb(alpha * 0.12F, 0xFFFFFF));
        g.fillGradient(0, y, w, y + band, argb(alpha * 0.12F, 0xFFFFFF), 0x00FFFFFF);
    }

    private static void gradient(GuiGraphics g, int w, int h, float alpha, int color, String side, float size) {
        int extent = Math.round(h * Mth.clamp(size, 0.0F, 1.0F));
        int solid = argb(alpha, color);
        int clear = color & 0xFFFFFF;
        if (side.equals("top") || side.equals("both")) {
            g.fillGradient(0, 0, w, extent, solid, clear);
        }
        if (side.equals("bottom") || side.equals("both")) {
            g.fillGradient(0, h - extent, w, h, clear, solid);
        }
    }

    private static void image(GuiGraphics g, ActiveFx fx, int w, int h, float alpha) {
        ResourceLocation texture = ResourceLocation.tryParse(fx.text("texture", ""));
        if (texture == null) {
            return;
        }
        if (!texture.getPath().endsWith(".png")) {
            texture = texture.withPath("textures/" + texture.getPath() + ".png");
        }
        int frames = Math.max(1, Math.round(fx.number("frames", 1.0F)));
        int frame = frames == 1 ? 0 : (int) (fx.age() * fx.number("fps", 10.0F)) % frames;
        Blend blend = switch (fx.text("blend", "normal")) {
            case "add" -> Blend.ADD;
            case "multiply" -> Blend.MULTIPLY;
            default -> Blend.NORMAL;
        };
        int x;
        int y;
        int iw;
        int ih;
        if (fx.text("fit", "stretch").equals("center")) {
            float size = fx.number("size", 0.5F) * h;
            float aspect = fx.number("aspect", 1.0F);
            iw = Math.round(size * aspect);
            ih = Math.round(size);
            x = Math.round((w - iw) / 2.0F + fx.number("x", 0.0F) * w);
            y = Math.round((h - ih) / 2.0F + fx.number("y", 0.0F) * h);
        } else {
            x = 0;
            y = 0;
            iw = w;
            ih = h;
        }
        blend(blend);
        setColor(g, fx.color, alpha, blend);
        g.blit(texture, x, y, iw, ih, 0.0F, frame * 256.0F, 256, 256, 256, 256 * frames);
        reset(g);
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
        reset(g);
    }

    private static void grain(GuiGraphics g, int w, int h, float alpha) {
        int gw = 480;
        int gh = 270;
        if (grain == null) {
            grain = new DynamicTexture(gw, gh, false);
            Minecraft.getInstance().getTextureManager().register(GRAIN, grain);
        }
        long now = Util.getMillis();
        if (now - lastGrain > 42L) {
            lastGrain = now;
            NativeImage image = grain.getPixels();
            if (image != null) {
                for (int y = 0; y < gh; y++) {
                    for (int x = 0; x < gw; x++) {
                        int v = RANDOM.nextBoolean() ? 255 : 0;
                        image.setPixelRGBA(x, y, RANDOM.nextInt(110) << 24 | v << 16 | v << 8 | v);
                    }
                }
                grain.upload();
            }
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1.0F, 1.0F, 1.0F, alpha);
        g.blit(GRAIN, 0, 0, w, h, 0.0F, 0.0F, gw, gh, gw, gh);
        reset(g);
    }

    private static ResourceLocation generated(ResourceLocation id, java.util.function.Consumer<NativeImage> painter) {
        if (GENERATED.add(id)) {
            NativeImage image = new NativeImage(256, 256, true);
            painter.accept(image);
            DynamicTexture texture = new DynamicTexture(image);
            texture.setFilter(true, false);
            Minecraft.getInstance().getTextureManager().register(id, texture);
        }
        return id;
    }

    public static void forgetGenerated() {
        GENERATED.clear();
    }

    private static void softHole(NativeImage image) {
        radial(image, 0.18F, 0.5F);
    }

    private static void hardHole(NativeImage image) {
        radial(image, 0.488F, 0.5F);
    }

    private static void radial(NativeImage image, float inner, float outer) {
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                float dx = (x + 0.5F) / 256.0F - 0.5F;
                float dy = (y + 0.5F) / 256.0F - 0.5F;
                float r = (float) Math.sqrt(dx * dx + dy * dy);
                float v = Mth.clamp((r - inner) / (outer - inner), 0.0F, 1.0F);
                v = v * v * (3.0F - 2.0F * v);
                image.setPixelRGBA(x, y, Math.round(v * 255.0F) << 24 | 0xFFFFFF);
            }
        }
    }

    private static float valueNoise(float x, float y, int seed) {
        int xi = Mth.floor(x);
        int yi = Mth.floor(y);
        float fx = x - xi;
        float fy = y - yi;
        float a = hash(xi, yi, seed);
        float b = hash(xi + 1, yi, seed);
        float c = hash(xi, yi + 1, seed);
        float d = hash(xi + 1, yi + 1, seed);
        float u = fx * fx * (3.0F - 2.0F * fx);
        float v = fy * fy * (3.0F - 2.0F * fy);
        return Mth.lerp(v, Mth.lerp(u, a, b), Mth.lerp(u, c, d));
    }

    private static float hash(int x, int y, int seed) {
        int h = x * 374761393 + y * 668265263 + seed * 982451653;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0xFFFFFF;
    }

    private static float fbm(float x, float y, int seed) {
        float sum = 0.0F;
        float amp = 0.5F;
        for (int i = 0; i < 5; i++) {
            sum += valueNoise(x, y, seed + i * 31) * amp;
            x *= 2.03F;
            y *= 2.03F;
            amp *= 0.5F;
        }
        return sum;
    }

    private static void frost(NativeImage image) {
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                float u = x / 255.0F;
                float v = y / 255.0F;
                float edge = Math.min(Math.min(u, 1.0F - u), Math.min(v, 1.0F - v));
                float n = fbm(u * 9.0F, v * 9.0F, 7);
                float crystals = Math.abs(fbm(u * 24.0F, v * 24.0F, 19) - 0.5F) * 2.0F;
                float reach = 0.08F + n * 0.22F;
                float a = Mth.clamp((reach - edge) / reach, 0.0F, 1.0F);
                a = (float) Math.pow(a, 0.8) * (0.65F + 0.35F * (1.0F - crystals));
                int shade = Math.round(225.0F + 30.0F * crystals);
                image.setPixelRGBA(x, y, Math.round(Mth.clamp(a, 0.0F, 1.0F) * 235.0F) << 24 | shade << 16 | shade << 8 | shade);
            }
        }
    }

    private static void blood(NativeImage image) {
        Random random = new Random(1337L);
        float[] alpha = new float[256 * 256];
        for (int i = 0; i < 26; i++) {
            float cx;
            float cy;
            int side = random.nextInt(4);
            float along = random.nextFloat();
            float depth = random.nextFloat() * 0.12F;
            switch (side) {
                case 0 -> { cx = along; cy = depth; }
                case 1 -> { cx = along; cy = 1.0F - depth; }
                case 2 -> { cx = depth; cy = along; }
                default -> { cx = 1.0F - depth; cy = along; }
            }
            float radius = 0.04F + random.nextFloat() * 0.1F;
            for (int y = 0; y < 256; y++) {
                for (int x = 0; x < 256; x++) {
                    float u = x / 255.0F;
                    float v = y / 255.0F;
                    float dx = u - cx;
                    float dy = v - cy;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d > radius + 0.07F) {
                        continue;
                    }
                    float wobble = fbm(u * 14.0F, v * 14.0F, i) * 0.06F;
                    float a = Mth.clamp((radius + wobble - d) / 0.015F, 0.0F, 1.0F);
                    alpha[y * 256 + x] = Math.max(alpha[y * 256 + x], a);
                }
            }
            if (side == 0) {
                int dripX = Math.round(cx * 255.0F);
                float length = radius + 0.1F + random.nextFloat() * 0.25F;
                for (int y = 0; y < Math.min(256, Math.round(length * 255.0F)); y++) {
                    float width = 2.5F * (1.0F - y / (length * 255.0F)) + 1.0F;
                    for (int x = Math.max(0, Math.round(dripX - width)); x <= Math.min(255, Math.round(dripX + width)); x++) {
                        alpha[y * 256 + x] = 1.0F;
                    }
                }
            }
        }
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                float a = alpha[y * 256 + x];
                float shade = 0.7F + 0.3F * fbm(x / 40.0F, y / 40.0F, 3);
                int r = Math.round(255.0F * shade);
                image.setPixelRGBA(x, y, Math.round(a * 230.0F) << 24 | r << 16 | r << 8 | r);
            }
        }
    }

    private static int argb(float alpha, int rgb) {
        return (Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F) << 24) | (rgb & 0xFFFFFF);
    }
}
