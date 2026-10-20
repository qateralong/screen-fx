package com.qateralong.screenfx;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FxPayload(String effect, String key, int durationMs, int fadeInMs, int fadeOutMs, float intensity, int color,
                        Map<String, String> params) implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "effect"));
    public static final String CLEAR = "clear";
    public static final int MAX_PARAMS = 64;
    public static final StreamCodec<FriendlyByteBuf, FxPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.effect, 128);
                buf.writeUtf(payload.key, 128);
                buf.writeVarInt(payload.durationMs);
                buf.writeVarInt(payload.fadeInMs);
                buf.writeVarInt(payload.fadeOutMs);
                buf.writeFloat(payload.intensity);
                buf.writeInt(payload.color);
                buf.writeVarInt(Math.min(MAX_PARAMS, payload.params.size()));
                payload.params.entrySet().stream().limit(MAX_PARAMS).forEach(entry -> {
                    buf.writeUtf(entry.getKey(), 64);
                    buf.writeUtf(entry.getValue(), 1024);
                });
            },
            buf -> {
                String effect = buf.readUtf(128);
                String key = buf.readUtf(128);
                int duration = buf.readVarInt();
                int fadeIn = buf.readVarInt();
                int fadeOut = buf.readVarInt();
                float intensity = buf.readFloat();
                int color = buf.readInt();
                int count = Math.min(MAX_PARAMS, buf.readVarInt());
                Map<String, String> params = new LinkedHashMap<>();
                for (int i = 0; i < count; i++) {
                    params.put(buf.readUtf(64), buf.readUtf(1024));
                }
                return new FxPayload(effect, key, duration, fadeIn, fadeOut, intensity, color, params);
            });

    public static FxPayload clear(String target) {
        return new FxPayload(CLEAR, target, 0, 0, 0, 0.0F, 0, Map.of());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
