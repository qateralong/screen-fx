package com.qateralong.screenfx.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.qateralong.screenfx.FxType;
import com.qateralong.screenfx.ScreenFx;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;

public final class PostFx {
    private static final Map<FxType, PostChain> CHAINS = new EnumMap<>(FxType.class);
    private static final Set<FxType> FAILED = new HashSet<>();
    private static int width;
    private static int height;

    private PostFx() {
    }

    public static void process(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        int w = minecraft.getWindow().getWidth();
        int h = minecraft.getWindow().getHeight();
        boolean resized = w != width || h != height;
        width = w;
        height = h;
        float phase = (Util.getMillis() % 1_000_000L) / 1000.0F;
        for (FxType type : FxType.values()) {
            if (type.kind != FxType.Kind.POST) {
                continue;
            }
            float strength = FxManager.strength(type);
            if (strength <= 0.001F) {
                continue;
            }
            PostChain chain = chain(type, minecraft);
            if (chain == null) {
                continue;
            }
            if (resized) {
                chain.resize(w, h);
            }
            if (type == FxType.BLUR) {
                chain.setUniform("Radius", Math.min(40.0F, strength * 16.0F));
            } else {
                chain.setUniform("Amount", Math.min(4.0F, strength));
                chain.setUniform("Phase", phase);
            }
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(partialTick);
        }
    }

    private static PostChain chain(FxType type, Minecraft minecraft) {
        PostChain chain = CHAINS.get(type);
        if (chain != null || FAILED.contains(type)) {
            return chain;
        }
        ResourceLocation location = type == FxType.BLUR
                ? ResourceLocation.withDefaultNamespace("shaders/post/blur.json")
                : ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "shaders/post/" + type.id() + ".json");
        try {
            chain = new PostChain(minecraft.getTextureManager(), minecraft.getResourceManager(), minecraft.getMainRenderTarget(), location);
            chain.resize(minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
            CHAINS.put(type, chain);
        } catch (Exception e) {
            FAILED.add(type);
            ScreenFxClient.LOGGER.error("Failed to load screen effect {}", location, e);
        }
        return chain;
    }

    public static void reset() {
        for (PostChain chain : CHAINS.values()) {
            chain.close();
        }
        CHAINS.clear();
        FAILED.clear();
    }
}
