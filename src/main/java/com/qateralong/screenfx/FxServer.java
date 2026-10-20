package com.qateralong.screenfx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FxServer {
    public record Scheduled(long delayTicks, FxPayload payload) {
    }

    private record Pending(long due, UUID player, FxPayload payload, String group) {
    }

    private record Active(FxPayload payload, long started, String group) {
        long end() {
            return this.payload.durationMs() <= 0 ? Long.MAX_VALUE : this.started + this.payload.durationMs();
        }

        boolean matches(String target) {
            String key = this.payload.key();
            return target.isEmpty() || key.equals(target) || this.payload.effect().equals(target) || this.group.equals(target) || key.startsWith(target + "#");
        }
    }

    private static final List<Pending> PENDING = new ArrayList<>();
    private static final Map<UUID, Map<String, Active>> ACTIVE = new HashMap<>();
    private static final Map<UUID, Long> RESTORE = new HashMap<>();
    private static long serverTick;

    private FxServer() {
    }

    public static boolean canReceive(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, FxPayload.TYPE);
    }

    static synchronized void tick(MinecraftServer server) {
        serverTick++;
        if (serverTick % 200 == 0) {
            long now = Util.getMillis();
            ACTIVE.values().forEach(map -> map.values().removeIf(active -> active.end() < now));
            ACTIVE.values().removeIf(Map::isEmpty);
        }
        if (!RESTORE.isEmpty()) {
            RESTORE.entrySet().removeIf(entry -> {
                if (entry.getValue() > serverTick) {
                    return false;
                }
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    restore(player);
                }
                return true;
            });
        }
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> iterator = PENDING.iterator();
        List<Pending> due = new ArrayList<>();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (pending.due <= serverTick) {
                iterator.remove();
                due.add(pending);
            }
        }
        for (Pending pending : due) {
            remember(pending.player, pending.payload, pending.group);
            ServerPlayer player = server.getPlayerList().getPlayer(pending.player);
            if (player != null && canReceive(player)) {
                ServerPlayNetworking.send(player, pending.payload);
            }
        }
    }

    static synchronized void reset() {
        PENDING.clear();
        ACTIVE.clear();
        RESTORE.clear();
    }

    public static synchronized void send(ServerPlayer player, FxPayload payload, long delayTicks, String group) {
        if (delayTicks <= 0L) {
            remember(player.getUUID(), payload, group);
            if (canReceive(player)) {
                ServerPlayNetworking.send(player, payload);
            }
        } else {
            PENDING.add(new Pending(serverTick + delayTicks, player.getUUID(), payload, group));
        }
    }

    public static synchronized void sendAll(ServerPlayer player, List<Scheduled> layers, String group) {
        for (Scheduled layer : layers) {
            send(player, layer.payload(), layer.delayTicks(), group);
        }
    }

    public static synchronized void clear(ServerPlayer player, String target) {
        String match = target.toLowerCase(Locale.ROOT);
        PENDING.removeIf(pending -> pending.player.equals(player.getUUID())
                && (match.isEmpty() || pending.group.equals(match) || pending.payload.key().equals(match) || pending.payload.effect().equals(match)
                || pending.payload.key().startsWith(match + "#")));
        Map<String, Active> map = ACTIVE.get(player.getUUID());
        if (map != null) {
            map.values().removeIf(active -> active.matches(match));
        }
        if (canReceive(player)) {
            ServerPlayNetworking.send(player, FxPayload.clear(match));
        }
    }

    private static void remember(UUID player, FxPayload payload, String group) {
        Map<String, Active> map = ACTIVE.computeIfAbsent(player, uuid -> new LinkedHashMap<>());
        map.put(payload.key(), new Active(payload, Util.getMillis(), group));
    }

    static synchronized void restoreLater(ServerPlayer player) {
        if (ACTIVE.containsKey(player.getUUID())) {
            RESTORE.put(player.getUUID(), serverTick + 20L);
        }
    }

    static synchronized void restore(ServerPlayer player) {
        Map<String, Active> map = ACTIVE.get(player.getUUID());
        if (map == null || map.isEmpty() || !canReceive(player)) {
            return;
        }
        long now = Util.getMillis();
        for (Active active : List.copyOf(map.values())) {
            long end = active.end();
            if (end <= now) {
                map.remove(active.payload.key());
                continue;
            }
            FxPayload original = active.payload;
            int remaining = end == Long.MAX_VALUE ? 0 : (int) Math.max(1L, end - now);
            ServerPlayNetworking.send(player, new FxPayload(original.effect(), original.key(), remaining, Math.min(300, original.fadeInMs()),
                    original.fadeOutMs(), original.intensity(), original.color(), original.params()));
        }
    }

    public static FxPayload effect(FxType type, float seconds, Float intensity, Float fadeIn, Float fadeOut, Integer color, Map<String, String> params) {
        Map<String, String> extra = new LinkedHashMap<>(params);
        String key = extra.remove("key");
        Float in = number(extra.remove("in"));
        Float out = number(extra.remove("out"));
        Integer paramColor = extra.containsKey("color") ? ScreenFx.parseColor(extra.remove("color")) : null;
        float useFadeIn = in != null ? in : type == FxType.FLASH ? 0.0F : fadeIn != null ? fadeIn : type.fade;
        float useFadeOut = out != null ? out : fadeOut != null ? fadeOut : type.fade;
        int useColor = paramColor != null ? paramColor : color != null ? color : type.color;
        return new FxPayload(type.id(), key == null ? type.id() : key.toLowerCase(Locale.ROOT), Math.round(seconds * 1000.0F),
                Math.round(useFadeIn * 1000.0F), Math.round(useFadeOut * 1000.0F), intensity == null ? type.intensity : intensity, useColor, extra);
    }

    public static List<Scheduled> preset(String name, FxPresets.Preset preset, Float seconds, Float intensity, Float fade, Integer color, Map<String, String> params) {
        float length = preset.length();
        float scale = seconds != null && seconds > 0.0F && length > 0.0F ? seconds / length : 1.0F;
        boolean hold = seconds != null && seconds == 0.0F;
        float intensityScale = intensity == null ? 1.0F : intensity;
        List<Scheduled> layers = new ArrayList<>();
        for (int i = 0; i < preset.layers.size(); i++) {
            FxPresets.Layer layer = preset.layers.get(i);
            FxType layerType = FxType.byId(layer.effect);
            float layerIntensity = (layer.intensity == null ? layerType.intensity : layer.intensity) * intensityScale;
            Float fadeIn = fade != null ? fade : layer.fadeIn == null ? null : layer.fadeIn * scale;
            Float fadeOut = fade != null ? fade : layer.fadeOut == null ? null : layer.fadeOut * scale;
            Integer layerColor = color != null ? color : layer.color == null ? null : ScreenFx.parseColor(layer.color);
            Map<String, String> layerParams = new LinkedHashMap<>(layer.params == null ? Map.of() : layer.params);
            layerParams.putAll(params);
            if (!layerParams.containsKey("key")) {
                layerParams.put("key", layer.key != null ? layer.key : name + "#" + i);
            }
            FxPayload payload = effect(layerType, hold ? 0.0F : layer.seconds * scale, layerIntensity, fadeIn, fadeOut, layerColor, layerParams);
            layers.add(new Scheduled(Math.round(layer.delay * scale * 20.0F), payload));
        }
        return layers;
    }

    private static Float number(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Math.max(0.0F, Float.parseFloat(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
