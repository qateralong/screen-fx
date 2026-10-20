package com.qateralong.screenfx;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScreenFx implements ModInitializer {
    public static final String MOD_ID = "screenfx";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("black", 0x000000), Map.entry("white", 0xFFFFFF), Map.entry("red", 0xFF2020), Map.entry("dark_red", 0x8B0000),
            Map.entry("green", 0x20D020), Map.entry("dark_green", 0x0B6623), Map.entry("blue", 0x2050FF), Map.entry("dark_blue", 0x0A1A6A),
            Map.entry("yellow", 0xFFE020), Map.entry("orange", 0xFF8C1A), Map.entry("purple", 0x8A2BE2), Map.entry("pink", 0xFF69B4),
            Map.entry("cyan", 0x20E0E0), Map.entry("aqua", 0x55FFFF), Map.entry("gold", 0xFFAA00), Map.entry("gray", 0x808080),
            Map.entry("lime", 0x80FF20), Map.entry("magenta", 0xFF20FF), Map.entry("brown", 0x7B4A1E));
    private static final SuggestionProvider<CommandSourceStack> EFFECTS = (context, builder) ->
            SharedSuggestionProvider.suggest(Stream.concat(FxType.ids().stream(), FxPresets.names().stream()), builder);
    private static final List<Pending> PENDING = new ArrayList<>();
    private static long serverTick;

    private record Pending(long due, UUID player, FxPayload payload, String group) {
    }

    private record Args(Float seconds, Float intensity, Float fade, String color, String params) {
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(FxPayload.TYPE, FxPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> FxPresets.load());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
        ServerTickEvents.END_SERVER_TICK.register(ScreenFx::tick);
    }

    private static void tick(MinecraftServer server) {
        serverTick++;
        if (PENDING.isEmpty()) {
            return;
        }
        Iterator<Pending> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            Pending pending = iterator.next();
            if (pending.due > serverTick) {
                continue;
            }
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(pending.player);
            if (player != null && ServerPlayNetworking.canSend(player, FxPayload.TYPE)) {
                ServerPlayNetworking.send(player, pending.payload);
            }
        }
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("screenfx")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(ScreenFx::list))
                .then(Commands.literal("reload").executes(context -> {
                    int count = FxPresets.load();
                    context.getSource().sendSuccess(() -> Component.literal("Пресеты перезагружены: " + count), true);
                    return count;
                }))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clear(context, ""))
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .suggests(EFFECTS)
                                        .executes(context -> clear(context, StringArgumentType.getString(context, "name"))))))
                .then(Commands.argument("effect", StringArgumentType.word())
                        .suggests(EFFECTS)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> apply(context, new Args(null, null, null, null, "")))
                                .then(Commands.argument("seconds", FloatArgumentType.floatArg(0.0F, 3600.0F))
                                        .executes(context -> apply(context, new Args(seconds(context), null, null, null, "")))
                                        .then(intensity())))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> intensity() {
        return Commands.argument("intensity", FloatArgumentType.floatArg(0.0F, 10.0F))
                .executes(context -> apply(context, new Args(seconds(context), FloatArgumentType.getFloat(context, "intensity"), null, null, "")))
                .then(Commands.argument("fade", FloatArgumentType.floatArg(0.0F, 60.0F))
                        .executes(context -> apply(context, new Args(seconds(context), FloatArgumentType.getFloat(context, "intensity"),
                                FloatArgumentType.getFloat(context, "fade"), null, "")))
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Stream.concat(Stream.of("default"), COLORS.keySet().stream()), builder))
                                .executes(context -> apply(context, new Args(seconds(context), FloatArgumentType.getFloat(context, "intensity"),
                                        FloatArgumentType.getFloat(context, "fade"), StringArgumentType.getString(context, "color"), "")))
                                .then(Commands.argument("params", StringArgumentType.greedyString())
                                        .executes(context -> apply(context, new Args(seconds(context), FloatArgumentType.getFloat(context, "intensity"),
                                                FloatArgumentType.getFloat(context, "fade"), StringArgumentType.getString(context, "color"),
                                                StringArgumentType.getString(context, "params")))))));
    }

    private static float seconds(CommandContext<CommandSourceStack> context) {
        return FloatArgumentType.getFloat(context, "seconds");
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        for (FxType.Kind kind : FxType.Kind.values()) {
            List<String> ids = new ArrayList<>();
            for (FxType type : FxType.values()) {
                if (type.kind == kind) {
                    ids.add(type.id());
                }
            }
            String title = switch (kind) {
                case OVERLAY -> "Экран: ";
                case POST -> "Фильтры: ";
                case CAMERA -> "Камера: ";
            };
            context.getSource().sendSuccess(() -> Component.literal(title + String.join(", ", ids)), false);
        }
        List<String> presets = FxPresets.names();
        context.getSource().sendSuccess(() -> Component.literal("Пресеты: " + (presets.isEmpty() ? "нет" : String.join(", ", presets))), false);
        return FxType.values().length + presets.size();
    }

    private static int apply(CommandContext<CommandSourceStack> context, Args args) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "effect").toLowerCase(Locale.ROOT);
        Integer color = null;
        if (args.color != null && !args.color.equalsIgnoreCase("default") && !args.color.equals("-")) {
            color = parseColor(args.color);
            if (color == null) {
                context.getSource().sendFailure(Component.literal("Неизвестный цвет: " + args.color + " (имя, #RRGGBB или default)"));
                return 0;
            }
        }
        Map<String, String> params = parseParams(args.params);
        if (params == null) {
            context.getSource().sendFailure(Component.literal("Параметры пишутся как ключ=значение через пробел"));
            return 0;
        }
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        FxType type = FxType.byId(id);
        if (type != null) {
            FxPayload payload = payload(type, args.seconds == null ? 3.0F : args.seconds, args.intensity, args.fade, args.fade, color, params, id);
            return report(context, "Эффект " + id, targets, player -> send(player, payload, 0L, id));
        }
        FxPresets.Preset preset = FxPresets.get(id);
        if (preset == null) {
            context.getSource().sendFailure(Component.literal("Неизвестный эффект или пресет: " + id + ". Список: /screenfx list"));
            return 0;
        }
        float length = preset.length();
        float scale = args.seconds != null && args.seconds > 0.0F && length > 0.0F ? args.seconds / length : 1.0F;
        boolean hold = args.seconds != null && args.seconds == 0.0F;
        float intensityScale = args.intensity == null ? 1.0F : args.intensity;
        List<Pending> layers = new ArrayList<>();
        for (int i = 0; i < preset.layers.size(); i++) {
            FxPresets.Layer layer = preset.layers.get(i);
            FxType layerType = FxType.byId(layer.effect);
            float intensity = (layer.intensity == null ? layerType.intensity : layer.intensity) * intensityScale;
            Float fadeIn = args.fade != null ? args.fade : layer.fadeIn == null ? null : layer.fadeIn * scale;
            Float fadeOut = args.fade != null ? args.fade : layer.fadeOut == null ? null : layer.fadeOut * scale;
            Integer layerColor = color != null ? color : layer.color == null ? null : parseColor(layer.color);
            Map<String, String> layerParams = new LinkedHashMap<>(layer.params == null ? Map.of() : layer.params);
            layerParams.putAll(params);
            if (!layerParams.containsKey("key")) {
                layerParams.put("key", layer.key != null ? layer.key : id + "#" + i);
            }
            FxPayload payload = payload(layerType, hold ? 0.0F : layer.seconds * scale, intensity, fadeIn, fadeOut, layerColor, layerParams, id);
            layers.add(new Pending(Math.round(layer.delay * scale * 20.0F), null, payload, id));
        }
        return report(context, "Пресет " + id, targets, player -> {
            for (Pending layer : layers) {
                send(player, layer.payload, layer.due, id);
            }
        });
    }

    private static FxPayload payload(FxType type, float seconds, Float intensity, Float fadeIn, Float fadeOut, Integer color,
                                     Map<String, String> params, String group) {
        Map<String, String> extra = new LinkedHashMap<>(params);
        String key = extra.remove("key");
        Float in = number(extra.remove("in"));
        Float out = number(extra.remove("out"));
        Integer paramColor = extra.containsKey("color") ? parseColor(extra.remove("color")) : null;
        float useFadeIn = in != null ? in : type == FxType.FLASH ? 0.0F : fadeIn != null ? fadeIn : type.fade;
        float useFadeOut = out != null ? out : fadeOut != null ? fadeOut : type.fade;
        int useColor = paramColor != null ? paramColor : color != null ? color : type.color;
        return new FxPayload(type.id(), key == null ? type.id() : key.toLowerCase(Locale.ROOT), Math.round(seconds * 1000.0F),
                Math.round(useFadeIn * 1000.0F), Math.round(useFadeOut * 1000.0F), intensity == null ? type.intensity : intensity, useColor, extra);
    }

    private static void send(ServerPlayer player, FxPayload payload, long delayTicks, String group) {
        if (delayTicks <= 0L) {
            ServerPlayNetworking.send(player, payload);
        } else {
            PENDING.add(new Pending(serverTick + delayTicks, player.getUUID(), payload, group));
        }
    }

    private static int clear(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String target = name.toLowerCase(Locale.ROOT);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        FxPayload payload = FxPayload.clear(target);
        return report(context, target.isEmpty() ? "Все эффекты сняты" : "Снято: " + target, targets, player -> {
            PENDING.removeIf(pending -> pending.player.equals(player.getUUID())
                    && (target.isEmpty() || pending.group.equals(target) || pending.payload.key().equals(target) || pending.payload.effect().equals(target)));
            ServerPlayNetworking.send(player, payload);
        });
    }

    private static int report(CommandContext<CommandSourceStack> context, String what, Collection<ServerPlayer> targets,
                              java.util.function.Consumer<ServerPlayer> action) {
        int sent = 0;
        int missing = 0;
        for (ServerPlayer player : targets) {
            if (ServerPlayNetworking.canSend(player, FxPayload.TYPE)) {
                action.accept(player);
                sent++;
            } else {
                missing++;
            }
        }
        int done = sent;
        context.getSource().sendSuccess(() -> Component.literal(what + " — игроков: " + done), true);
        if (missing > 0) {
            int lacking = missing;
            context.getSource().sendFailure(Component.literal("У " + lacking + " игроков не установлен мод ScreenFX"));
        }
        return sent;
    }

    private static Map<String, String> parseParams(String text) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String token : text.trim().split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            int eq = token.indexOf('=');
            if (eq <= 0) {
                return null;
            }
            params.put(token.substring(0, eq).toLowerCase(Locale.ROOT), token.substring(eq + 1));
        }
        return params;
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

    public static Integer parseColor(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (COLORS.containsKey(lower)) {
            return COLORS.get(lower);
        }
        String hex = lower.startsWith("#") ? lower.substring(1) : lower;
        if (hex.matches("[0-9a-f]{6}")) {
            return Integer.parseInt(hex, 16);
        }
        return null;
    }
}
