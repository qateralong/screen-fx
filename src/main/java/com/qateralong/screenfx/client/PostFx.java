package com.qateralong.screenfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qateralong.screenfx.FxType;
import com.qateralong.screenfx.ScreenFx;
import com.qateralong.screenfx.client.mixin.PostChainAccessor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

public final class PostFx {
    private static final Map<ResourceLocation, PostChain> CHAINS = new HashMap<>();
    private static final Map<ResourceLocation, long[]> SIZES = new HashMap<>();
    private static final Set<ResourceLocation> FAILED = new HashSet<>();

    private PostFx() {
    }

    public static void process(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        int w = minecraft.getWindow().getWidth();
        int h = minecraft.getWindow().getHeight();
        float phase = (Util.getMillis() % 1_000_000L) / 1000.0F;
        for (ActiveFx fx : FxManager.current()) {
            if (fx.type.kind != FxType.Kind.POST) {
                continue;
            }
            float strength = fx.strength();
            if (strength <= 0.001F) {
                continue;
            }
            ResourceLocation location = location(fx);
            if (location == null) {
                continue;
            }
            PostChain chain = chain(location, minecraft);
            if (chain == null) {
                continue;
            }
            long[] size = SIZES.get(location);
            if (size[0] != w || size[1] != h) {
                chain.resize(w, h);
                size[0] = w;
                size[1] = h;
            }
            float r = ((fx.color >> 16) & 0xFF) / 255.0F;
            float g = ((fx.color >> 8) & 0xFF) / 255.0F;
            float b = (fx.color & 0xFF) / 255.0F;
            for (PostPass pass : ((PostChainAccessor) chain).screenfx$passes()) {
                var effect = pass.getEffect();
                effect.safeGetUniform("Amount").set(Math.min(4.0F, strength));
                effect.safeGetUniform("Phase").set(phase * fx.speed());
                effect.safeGetUniform("FxTime").set(fx.time());
                effect.safeGetUniform("Speed").set(fx.speed());
                effect.safeGetUniform("Seed").set(fx.seed);
                effect.safeGetUniform("Color").set(r, g, b);
                if (fx.type == FxType.BLUR) {
                    effect.safeGetUniform("Radius").set(Math.min(40.0F, strength * 16.0F));
                }
                for (Map.Entry<String, String> entry : fx.params.entrySet()) {
                    if (entry.getKey().startsWith("u.")) {
                        float[] values = floats(entry.getValue());
                        if (values != null) {
                            effect.safeGetUniform(entry.getKey().substring(2)).set(values);
                        }
                    }
                }
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(partialTick);
        }
    }

    private static float[] floats(String text) {
        String[] parts = text.split(",");
        float[] values = new float[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Float.parseFloat(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return values;
    }

    private static ResourceLocation location(ActiveFx fx) {
        if (fx.type == FxType.BLUR) {
            return ResourceLocation.withDefaultNamespace("shaders/post/blur.json");
        }
        if (fx.type == FxType.SHADER) {
            ResourceLocation id = ResourceLocation.tryParse(fx.text("id", ""));
            if (id == null || fx.text("id", "").isEmpty()) {
                return null;
            }
            String path = id.getPath();
            if (!path.startsWith("shaders/post/")) {
                path = "shaders/post/" + path;
            }
            if (!path.endsWith(".json")) {
                path = path + ".json";
            }
            return id.withPath(path);
        }
        return ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "shaders/post/" + fx.type.id() + ".json");
    }

    private static PostChain chain(ResourceLocation location, Minecraft minecraft) {
        PostChain chain = CHAINS.get(location);
        if (chain != null || FAILED.contains(location)) {
            return chain;
        }
        try {
            chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), location);
            chain.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            CHAINS.put(location, chain);
            SIZES.put(location, new long[]{minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight()});
        } catch (Exception e) {
            FAILED.add(location);
            ScreenFxClient.LOGGER.error("Failed to load screen effect {}", location, e);
        }
        return chain;
    }

    public static void reset() {
        for (PostChain chain : CHAINS.values()) {
            chain.close();
        }
        CHAINS.clear();
        SIZES.clear();
        FAILED.clear();
    }
}
