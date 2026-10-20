package com.qateralong.screenfx;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ScreenFx implements ModInitializer {
    public static final String MOD_ID = "screenfx";
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("black", 0x000000), Map.entry("white", 0xFFFFFF), Map.entry("red", 0xFF2020), Map.entry("dark_red", 0x8B0000),
            Map.entry("green", 0x20D020), Map.entry("dark_green", 0x0B6623), Map.entry("blue", 0x2050FF), Map.entry("dark_blue", 0x0A1A6A),
            Map.entry("yellow", 0xFFE020), Map.entry("orange", 0xFF8C1A), Map.entry("purple", 0x8A2BE2), Map.entry("pink", 0xFF69B4),
            Map.entry("cyan", 0x20E0E0), Map.entry("aqua", 0x55FFFF), Map.entry("gold", 0xFFAA00), Map.entry("gray", 0x808080),
            Map.entry("lime", 0x80FF20), Map.entry("magenta", 0xFF20FF), Map.entry("brown", 0x7B4A1E));

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(FxPayload.TYPE, FxPayload.CODEC);
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("screenfx")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("list").executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("Эффекты: " + String.join(", ", FxType.ids())), false);
                    return FxType.values().length;
                }))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> send(context, FxPayload.CLEAR, "", 0, 0, 0, 0))
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(FxType.ids(), builder))
                                        .executes(context -> send(context, FxPayload.CLEAR, StringArgumentType.getString(context, "effect"), 0, 0, 0, 0)))))
                .then(Commands.argument("effect", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(FxType.ids(), builder))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> apply(context, 3.0F, -1.0F, -1.0F, null))
                                .then(Commands.argument("seconds", FloatArgumentType.floatArg(0.0F, 3600.0F))
                                        .executes(context -> apply(context, FloatArgumentType.getFloat(context, "seconds"), -1.0F, -1.0F, null))
                                        .then(intensity())))));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> intensity() {
        return Commands.argument("intensity", FloatArgumentType.floatArg(0.0F, 10.0F))
                .executes(context -> apply(context, FloatArgumentType.getFloat(context, "seconds"), FloatArgumentType.getFloat(context, "intensity"), -1.0F, null))
                .then(Commands.argument("fade", FloatArgumentType.floatArg(0.0F, 60.0F))
                        .executes(context -> apply(context, FloatArgumentType.getFloat(context, "seconds"), FloatArgumentType.getFloat(context, "intensity"),
                                FloatArgumentType.getFloat(context, "fade"), null))
                        .then(Commands.argument("color", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(COLORS.keySet(), builder))
                                .executes(context -> apply(context, FloatArgumentType.getFloat(context, "seconds"), FloatArgumentType.getFloat(context, "intensity"),
                                        FloatArgumentType.getFloat(context, "fade"), StringArgumentType.getString(context, "color")))));
    }

    private static int apply(CommandContext<CommandSourceStack> context, float seconds, float intensity, float fade, String colorName) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "effect");
        FxType type = FxType.byId(id);
        if (type == null) {
            context.getSource().sendFailure(Component.literal("Неизвестный эффект: " + id + ". Список: /screenfx list"));
            return 0;
        }
        int color = type.color;
        if (colorName != null) {
            Integer parsed = parseColor(colorName);
            if (parsed == null) {
                context.getSource().sendFailure(Component.literal("Неизвестный цвет: " + colorName + " (имя или #RRGGBB)"));
                return 0;
            }
            color = parsed;
        }
        float useIntensity = intensity < 0.0F ? type.intensity : intensity;
        float useFade = fade < 0.0F ? type.fade : fade;
        return send(context, type.id(), "", Math.round(seconds * 1000.0F), Math.round(useFade * 1000.0F), useIntensity, color);
    }

    private static Integer parseColor(String name) {
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

    private static int send(CommandContext<CommandSourceStack> context, String effect, String clearTarget, int durationMs, int fadeMs, float intensity, int color) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        FxPayload payload = effect.equals(FxPayload.CLEAR)
                ? new FxPayload(FxPayload.CLEAR, 0, 0, 0.0F, 0) : new FxPayload(effect, durationMs, fadeMs, intensity, color);
        if (effect.equals(FxPayload.CLEAR) && !clearTarget.isEmpty()) {
            payload = new FxPayload(FxPayload.CLEAR + ":" + clearTarget.toLowerCase(Locale.ROOT), 0, 0, 0.0F, 0);
        }
        int sent = 0;
        int missing = 0;
        for (ServerPlayer player : targets) {
            if (ServerPlayNetworking.canSend(player, FxPayload.TYPE)) {
                ServerPlayNetworking.send(player, payload);
                sent++;
            } else {
                missing++;
            }
        }
        int done = sent;
        String what = effect.equals(FxPayload.CLEAR) ? "Эффекты сняты" : "Эффект " + effect + " применён";
        context.getSource().sendSuccess(() -> Component.literal(what + " — игроков: " + done), true);
        if (missing > 0) {
            int lacking = missing;
            context.getSource().sendFailure(Component.literal("У " + lacking + " игроков не установлен мод ScreenFX"));
        }
        return sent;
    }
}
