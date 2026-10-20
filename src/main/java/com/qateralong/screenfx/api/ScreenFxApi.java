package com.qateralong.screenfx.api;

import com.qateralong.screenfx.FxPresets;
import com.qateralong.screenfx.FxServer;
import com.qateralong.screenfx.FxType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

public final class ScreenFxApi {
    private ScreenFxApi() {
    }

    public static boolean hasClient(ServerPlayer player) {
        return FxServer.canReceive(player);
    }

    public static List<String> effects() {
        return FxType.ids();
    }

    public static List<String> presets() {
        return FxPresets.names();
    }

    public static Effect effect(String id) {
        FxType type = FxType.byId(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown screen effect: " + id);
        }
        return new Effect(type);
    }

    public static boolean preset(ServerPlayer player, String name) {
        return preset(List.of(player), name, null, null);
    }

    public static boolean preset(Collection<ServerPlayer> players, String name, Float seconds, Float intensity) {
        String id = name.toLowerCase(Locale.ROOT);
        FxPresets.Preset preset = FxPresets.get(id);
        if (preset == null) {
            return false;
        }
        List<FxServer.Scheduled> layers = FxServer.preset(id, preset, seconds, intensity, null, null, Map.of());
        for (ServerPlayer player : players) {
            FxServer.sendAll(player, layers, id);
        }
        return true;
    }

    public static void clear(ServerPlayer player) {
        FxServer.clear(player, "");
    }

    public static void clear(ServerPlayer player, String target) {
        FxServer.clear(player, target);
    }

    public static final class Effect {
        private final FxType type;
        private float seconds = 3.0F;
        private Float intensity;
        private Float fadeIn;
        private Float fadeOut;
        private Integer color;
        private long delayTicks;
        private final Map<String, String> params = new LinkedHashMap<>();

        private Effect(FxType type) {
            this.type = type;
        }

        public Effect seconds(float value) {
            this.seconds = Math.max(0.0F, value);
            return this;
        }

        public Effect hold() {
            this.seconds = 0.0F;
            return this;
        }

        public Effect intensity(float value) {
            this.intensity = value;
            return this;
        }

        public Effect fade(float value) {
            this.fadeIn = value;
            this.fadeOut = value;
            return this;
        }

        public Effect fadeIn(float value) {
            this.fadeIn = value;
            return this;
        }

        public Effect fadeOut(float value) {
            this.fadeOut = value;
            return this;
        }

        public Effect color(int rgb) {
            this.color = rgb & 0xFFFFFF;
            return this;
        }

        public Effect key(String key) {
            this.params.put("key", key);
            return this;
        }

        public Effect param(String name, Object value) {
            this.params.put(name.toLowerCase(Locale.ROOT), String.valueOf(value));
            return this;
        }

        public Effect delay(long ticks) {
            this.delayTicks = Math.max(0L, ticks);
            return this;
        }

        public void send(ServerPlayer player) {
            FxServer.send(player, FxServer.effect(this.type, this.seconds, this.intensity, this.fadeIn, this.fadeOut, this.color, this.params),
                    this.delayTicks, this.type.id());
        }

        public void send(Collection<ServerPlayer> players) {
            for (ServerPlayer player : players) {
                this.send(player);
            }
        }
    }
}
