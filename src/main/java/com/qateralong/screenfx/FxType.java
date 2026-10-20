package com.qateralong.screenfx;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum FxType {
    BLACKOUT(Kind.OVERLAY, 1.0F, 0.5F, 0x000000),
    WHITEOUT(Kind.OVERLAY, 1.0F, 0.5F, 0xFFFFFF),
    FLASH(Kind.OVERLAY, 1.0F, 1.5F, 0xFFFFFF),
    FADE(Kind.OVERLAY, 1.0F, 1.0F, 0x000000),
    DARKEN(Kind.OVERLAY, 0.6F, 0.5F, 0x000000),
    BRIGHTEN(Kind.OVERLAY, 0.5F, 0.5F, 0xFFFFFF),
    TINT(Kind.OVERLAY, 0.35F, 0.5F, 0xFF2020),
    VIGNETTE(Kind.OVERLAY, 0.85F, 0.5F, 0x000000),
    HEARTBEAT(Kind.OVERLAY, 0.8F, 0.3F, 0xC00000),
    LETTERBOX(Kind.OVERLAY, 1.0F, 0.7F, 0x000000),
    STATIC(Kind.OVERLAY, 0.45F, 0.3F, 0xFFFFFF),
    GRAYSCALE(Kind.POST, 1.0F, 0.5F, 0),
    SEPIA(Kind.POST, 1.0F, 0.5F, 0),
    INVERT(Kind.POST, 1.0F, 0.5F, 0),
    BLUR(Kind.POST, 1.0F, 0.5F, 0),
    VIVID(Kind.POST, 1.0F, 0.5F, 0),
    PIXELATE(Kind.POST, 0.5F, 0.5F, 0),
    GLITCH(Kind.POST, 1.0F, 0.2F, 0),
    WAVE(Kind.POST, 1.0F, 0.6F, 0),
    SHAKE(Kind.CAMERA, 1.0F, 0.2F, 0),
    DRUNK(Kind.CAMERA, 1.0F, 1.0F, 0),
    SPIN(Kind.CAMERA, 1.0F, 0.5F, 0),
    UPSIDEDOWN(Kind.CAMERA, 1.0F, 0.6F, 0),
    ZOOM(Kind.CAMERA, 0.5F, 0.4F, 0),
    WIDE(Kind.CAMERA, 0.5F, 0.4F, 0);

    public enum Kind { OVERLAY, POST, CAMERA }

    public final Kind kind;
    public final float intensity;
    public final float fade;
    public final int color;

    FxType(Kind kind, float intensity, float fade, int color) {
        this.kind = kind;
        this.intensity = intensity;
        this.fade = fade;
        this.color = color;
    }

    public String id() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static FxType byId(String id) {
        for (FxType type : values()) {
            if (type.id().equals(id.toLowerCase(Locale.ROOT))) {
                return type;
            }
        }
        return null;
    }

    public static List<String> ids() {
        return Arrays.stream(values()).map(FxType::id).toList();
    }
}
