package com.qateralong.screenfx;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FxPayload(String effect, int durationMs, int fadeMs, float intensity, int color) implements CustomPacketPayload {
    public static final Type<FxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ScreenFx.MOD_ID, "effect"));
    public static final String CLEAR = "clear";
    public static final StreamCodec<FriendlyByteBuf, FxPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.effect, 64);
                buf.writeVarInt(payload.durationMs);
                buf.writeVarInt(payload.fadeMs);
                buf.writeFloat(payload.intensity);
                buf.writeInt(payload.color);
            },
            buf -> new FxPayload(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
