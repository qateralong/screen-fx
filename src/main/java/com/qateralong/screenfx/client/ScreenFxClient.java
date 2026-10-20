package com.qateralong.screenfx.client;

import com.qateralong.screenfx.FxPayload;
import com.qateralong.screenfx.ScreenFx;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenFxClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ScreenFx.MOD_ID);

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(FxPayload.TYPE, (payload, context) -> FxManager.handle(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> FxManager.clearAll());
        HudRenderCallback.EVENT.register(Overlays::renderHud);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                ScreenEvents.afterRender(screen).register((s, graphics, mouseX, mouseY, delta) -> Overlays.render(graphics)));
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "post_chains");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                PostFx.reset();
            }
        });
    }
}
