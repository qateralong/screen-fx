package com.qateralong.screenfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;

public final class FxPresets {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("screenfx").resolve("presets");
    private static final Map<String, Preset> PRESETS = new TreeMap<>();

    public static class Preset {
        public String description = "";
        public List<Layer> layers = new ArrayList<>();

        public float length() {
            float length = 0.0F;
            for (Layer layer : this.layers) {
                length = Math.max(length, layer.delay + layer.seconds);
            }
            return length;
        }
    }

    public static class Layer {
        public String effect = "darken";
        public String key;
        public float delay;
        public float seconds = 3.0F;
        public Float intensity;
        public Float fadeIn;
        public Float fadeOut;
        public String color;
        public Map<String, String> params = new LinkedHashMap<>();

        Layer() {
        }

        Layer(String effect, float delay, float seconds, Float intensity, Float fadeIn, Float fadeOut, String color, String... params) {
            this.effect = effect;
            this.delay = delay;
            this.seconds = seconds;
            this.intensity = intensity;
            this.fadeIn = fadeIn;
            this.fadeOut = fadeOut;
            this.color = color;
            for (int i = 0; i + 1 < params.length; i += 2) {
                this.params.put(params[i], params[i + 1]);
            }
        }
    }

    private FxPresets() {
    }

    public static synchronized Preset get(String name) {
        return PRESETS.get(name.toLowerCase(Locale.ROOT));
    }

    public static synchronized List<String> names() {
        return new ArrayList<>(PRESETS.keySet());
    }

    public static synchronized int load() {
        PRESETS.clear();
        try {
            if (!Files.exists(DIR)) {
                Files.createDirectories(DIR);
                writeDefaults();
            }
            try (Stream<Path> files = Files.list(DIR)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".json")).toList()) {
                    String name = file.getFileName().toString();
                    name = name.substring(0, name.length() - 5).toLowerCase(Locale.ROOT);
                    if (FxType.byId(name) != null || name.equals(FxPayload.CLEAR)) {
                        ScreenFx.LOGGER.warn("Preset {} shadows a built-in effect and is ignored", name);
                        continue;
                    }
                    try (Reader reader = Files.newBufferedReader(file)) {
                        Preset preset = GSON.fromJson(reader, Preset.class);
                        if (preset != null && preset.layers != null) {
                            preset.layers.removeIf(layer -> layer == null || layer.effect == null || FxType.byId(layer.effect) == null);
                            PRESETS.put(name, preset);
                        }
                    } catch (Exception e) {
                        ScreenFx.LOGGER.error("Failed to read screen effect preset {}", file, e);
                    }
                }
            }
        } catch (IOException e) {
            ScreenFx.LOGGER.error("Failed to load screen effect presets", e);
        }
        return PRESETS.size();
    }

    private static void writeDefaults() throws IOException {
        write("faint", "Потеря сознания: мир плывёт, глаза закрываются",
                new Layer("blur", 0.0F, 6.0F, 0.8F, 2.0F, 1.0F, null),
                new Layer("drunk", 0.0F, 6.0F, 0.8F, 1.5F, 1.0F, null),
                new Layer("tunnel", 0.0F, 6.0F, 0.6F, 3.0F, 1.0F, null),
                new Layer("eyes", 2.5F, 3.5F, 1.0F, 3.0F, 1.0F, null));
        write("wakeup", "Пробуждение: глаза медленно открываются",
                new Layer("eyes", 0.0F, 1.0F, 1.0F, 0.0F, 3.0F, null),
                new Layer("blur", 0.0F, 2.5F, 1.0F, 0.0F, 3.0F, null),
                new Layer("blink", 3.0F, 2.5F, 1.0F, 0.2F, 0.2F, null, "speed", "0.8"),
                new Layer("sway", 0.0F, 4.0F, 0.6F, 0.0F, 2.0F, null));
        write("explosion", "Взрыв рядом: вспышка, тряска, оглушение",
                new Layer("flash", 0.0F, 0.1F, 1.0F, 0.0F, 1.5F, null),
                new Layer("kick", 0.0F, 1.0F, 1.5F, 0.0F, 0.0F, null),
                new Layer("shake", 0.0F, 1.2F, 1.8F, 0.0F, 1.0F, null),
                new Layer("blur", 0.0F, 1.5F, 1.0F, 0.0F, 2.0F, null),
                new Layer("chromatic", 0.0F, 1.5F, 1.0F, 0.0F, 1.5F, null));
        write("concussion", "Контузия: двоится в глазах, качает",
                new Layer("doublevision", 0.0F, 6.0F, 1.0F, 0.5F, 2.0F, null),
                new Layer("blur", 0.0F, 6.0F, 0.4F, 0.5F, 2.0F, null),
                new Layer("drunk", 0.0F, 6.0F, 0.7F, 0.5F, 2.0F, null),
                new Layer("grayscale", 0.0F, 6.0F, 0.5F, 0.5F, 2.0F, null));
        write("poison", "Отравление: зелёный оттенок, волны, тошнота",
                new Layer("tint", 0.0F, 8.0F, 0.25F, 1.5F, 2.0F, "#30C040"),
                new Layer("wave", 0.0F, 8.0F, 0.8F, 1.5F, 2.0F, null),
                new Layer("drunk", 0.0F, 8.0F, 0.6F, 1.5F, 2.0F, null),
                new Layer("vignette", 0.0F, 8.0F, 0.7F, 1.5F, 2.0F, "#0B4010"));
        write("horror", "Хоррор: обесцвечивание, помехи, сердцебиение",
                new Layer("grayscale", 0.0F, 10.0F, 0.85F, 2.0F, 2.0F, null),
                new Layer("vignette", 0.0F, 10.0F, 0.95F, 2.0F, 2.0F, null),
                new Layer("heartbeat", 0.0F, 10.0F, 0.7F, 2.0F, 2.0F, null),
                new Layer("grain", 0.0F, 10.0F, 0.3F, 2.0F, 2.0F, null),
                new Layer("static", 2.0F, 0.4F, 0.35F, 0.0F, 0.2F, null));
        write("daydream", "Сон: мягкое свечение, покачивание, светлые края",
                new Layer("dream", 0.0F, 10.0F, 1.0F, 2.0F, 2.0F, null),
                new Layer("vignette", 0.0F, 10.0F, 0.6F, 2.0F, 2.0F, "#FFFFFF"),
                new Layer("sway", 0.0F, 10.0F, 0.5F, 2.0F, 2.0F, null));
        write("hit", "Удар: рывок камеры и красные края",
                new Layer("kick", 0.0F, 0.6F, 1.0F, 0.0F, 0.0F, null),
                new Layer("vignette", 0.0F, 0.2F, 0.9F, 0.0F, 0.6F, "#B00000"),
                new Layer("chromatic", 0.0F, 0.2F, 0.6F, 0.0F, 0.5F, null));
        write("freeze", "Замерзание: иней по краям, холодный оттенок, дрожь",
                new Layer("frost", 0.0F, 8.0F, 1.0F, 3.0F, 2.0F, null),
                new Layer("colorize", 0.0F, 8.0F, 0.35F, 3.0F, 2.0F, "#A0D8FF"),
                new Layer("shake", 0.0F, 8.0F, 0.15F, 3.0F, 2.0F, null));
        write("tv_off", "Выключение старого телевизора",
                new Layer("scanlines", 0.0F, 1.5F, 0.5F, 0.1F, 0.2F, null),
                new Layer("static", 0.0F, 0.5F, 0.6F, 0.0F, 0.3F, null),
                new Layer("iris", 0.4F, 1.6F, 1.0F, 0.8F, 0.6F, null));
        write("cutscene", "Кинорежим: полосы и виньетка, пока не сняты",
                new Layer("letterbox", 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, null),
                new Layer("vignette", 0.0F, 0.0F, 0.5F, 1.0F, 1.0F, null));
        write("blackout_slow", "Медленно уходит в чёрное и медленно возвращается",
                new Layer("fade", 0.0F, 4.0F, 1.0F, 3.0F, 3.0F, "#000000"));
    }

    private static void write(String name, String description, Layer... layers) throws IOException {
        Path file = DIR.resolve(name + ".json");
        if (Files.exists(file)) {
            return;
        }
        Preset preset = new Preset();
        preset.description = description;
        preset.layers = new ArrayList<>(List.of(layers));
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(preset, writer);
        }
    }
}
