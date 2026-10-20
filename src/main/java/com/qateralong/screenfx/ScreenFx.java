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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
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

    private record Args(Float seconds, Float intensity, Float fade, String color, String params) {
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(FxPayload.TYPE, FxPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> register(dispatcher));
        ServerLifecycleEvents.SERVER_STARTING.register(server -> FxPresets.load());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> FxServer.reset());
        ServerTickEvents.END_SERVER_TICK.register(FxServer::tick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> FxServer.restoreLater(handler.player));
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
            FxPayload payload = FxServer.effect(type, args.seconds == null ? 3.0F : args.seconds, args.intensity, args.fade, args.fade, color, params);
            return report(context, "Эффект " + id, targets, player -> FxServer.send(player, payload, 0L, id));
        }
        FxPresets.Preset preset = FxPresets.get(id);
        if (preset == null) {
            context.getSource().sendFailure(Component.literal("Неизвестный эффект или пресет: " + id + ". Список: /screenfx list"));
            return 0;
        }
        List<FxServer.Scheduled> layers = FxServer.preset(id, preset, args.seconds, args.intensity, args.fade, color, params);
        return report(context, "Пресет " + id, targets, player -> FxServer.sendAll(player, layers, id));
    }

    private static int clear(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String target = name.toLowerCase(Locale.ROOT);
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        return report(context, target.isEmpty() ? "Все эффекты сняты" : "Снято: " + target, targets, player -> FxServer.clear(player, target));
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
