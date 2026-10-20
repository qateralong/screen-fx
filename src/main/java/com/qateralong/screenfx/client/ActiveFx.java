package com.qateralong.screenfx.client;

import com.qateralong.screenfx.FxPayload;
import com.qateralong.screenfx.FxType;
import java.util.Map;
import net.minecraft.Util;

public final class ActiveFx {
    public final FxType type;
    public final String key;
    public final long start;
    public final int duration;
    public final int fadeIn;
    public final int fadeOut;
    public final float intensity;
    public final int color;
    public final Map<String, String> params;
    public final float seed;
    private long releaseAt = -1L;

    public ActiveFx(FxType type, FxPayload payload) {
        this.type = type;
        this.key = payload.key().isEmpty() ? type.id() : payload.key();
        this.start = Util.getMillis();
        this.duration = payload.durationMs();
        this.fadeIn = payload.fadeInMs();
        this.fadeOut = payload.fadeOutMs();
        this.intensity = payload.intensity();
        this.color = payload.color();
        this.params = Map.copyOf(payload.params());
        this.seed = (float) (Math.random() * 1000.0);
    }

    public void release() {
        long now = Util.getMillis();
        if (this.releaseAt < 0 || this.releaseAt > now) {
            this.releaseAt = now;
        }
    }

    public boolean matches(String target) {
        return target.isEmpty() || this.key.equals(target) || this.type.id().equals(target) || this.key.startsWith(target + "#");
    }

    public float age() {
        return (Util.getMillis() - this.start) / 1000.0F;
    }

    public boolean finished() {
        long end = this.end();
        return end >= 0 && Util.getMillis() >= end + this.fadeOut;
    }

    private long end() {
        long natural = this.duration > 0 ? this.start + this.duration : -1L;
        if (this.releaseAt >= 0) {
            return natural < 0 ? this.releaseAt : Math.min(natural, this.releaseAt);
        }
        return natural;
    }

    public float envelope() {
        long now = Util.getMillis();
        float value = 1.0F;
        if (this.fadeIn > 0 && now - this.start < this.fadeIn) {
            value = (now - this.start) / (float) this.fadeIn;
        }
        long end = this.end();
        if (end >= 0 && now >= end) {
            value = this.fadeOut <= 0 ? 0.0F : Math.min(value, 1.0F - (now - end) / (float) this.fadeOut);
        }
        value = Math.max(0.0F, Math.min(1.0F, value));
        return value * value * (3.0F - 2.0F * value);
    }

    public float strength() {
        return this.envelope() * this.intensity;
    }

    public float speed() {
        return this.number("speed", 1.0F);
    }

    public float time() {
        return this.age() * this.speed();
    }

    public float number(String name, float fallback) {
        String value = this.params.get(name);
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public String text(String name, String fallback) {
        return this.params.getOrDefault(name, fallback);
    }

    public int colorParam(String name, int fallback) {
        String value = this.params.get(name);
        if (value == null) {
            return fallback;
        }
        String hex = value.startsWith("#") ? value.substring(1) : value;
        try {
            return Integer.parseInt(hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public boolean worldLayer() {
        return "world".equals(this.params.get("layer"));
    }
}
