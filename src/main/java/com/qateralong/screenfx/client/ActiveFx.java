package com.qateralong.screenfx.client;

import com.qateralong.screenfx.FxType;
import net.minecraft.Util;

public final class ActiveFx {
    public final FxType type;
    public final long start;
    public final int duration;
    public final int fade;
    public final float intensity;
    public final int color;
    private long releaseAt = -1L;

    public ActiveFx(FxType type, int duration, int fade, float intensity, int color) {
        this.type = type;
        this.start = Util.getMillis();
        this.duration = duration;
        this.fade = fade;
        this.intensity = intensity;
        this.color = color;
    }

    public void release() {
        long now = Util.getMillis();
        if (this.releaseAt < 0 || this.releaseAt > now) {
            this.releaseAt = now;
        }
    }

    public float age() {
        return (Util.getMillis() - this.start) / 1000.0F;
    }

    public boolean finished() {
        long now = Util.getMillis();
        long end = this.end();
        return end >= 0 && now >= end + this.fadeOut();
    }

    private long end() {
        long natural = this.duration > 0 ? this.start + this.duration : -1L;
        if (this.releaseAt >= 0) {
            return natural < 0 ? this.releaseAt : Math.min(natural, this.releaseAt);
        }
        return natural;
    }

    private int fadeIn() {
        return this.type == FxType.FLASH ? 0 : this.fade;
    }

    private int fadeOut() {
        if (this.type == FxType.FLASH) {
            return Math.max(this.fade, 1);
        }
        return this.fade;
    }

    public float envelope() {
        long now = Util.getMillis();
        float value = 1.0F;
        int in = this.fadeIn();
        if (in > 0 && now - this.start < in) {
            value = (now - this.start) / (float) in;
        }
        long end = this.end();
        if (end >= 0 && now >= end) {
            int out = this.fadeOut();
            value = out <= 0 ? 0.0F : Math.min(value, 1.0F - (now - end) / (float) out);
        }
        value = Math.max(0.0F, Math.min(1.0F, value));
        return value * value * (3.0F - 2.0F * value);
    }

    public float strength() {
        return this.envelope() * this.intensity;
    }
}
