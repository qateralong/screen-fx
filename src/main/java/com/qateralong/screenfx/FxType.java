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
    PULSE(Kind.OVERLAY, 0.45F, 0.3F, 0xFF2020),
    STROBE(Kind.OVERLAY, 0.8F, 0.0F, 0xFFFFFF),
    BLINK(Kind.OVERLAY, 1.0F, 0.3F, 0x000000),
    EYES(Kind.OVERLAY, 1.0F, 1.0F, 0x000000),
    TUNNEL(Kind.OVERLAY, 0.7F, 0.8F, 0x000000),
    IRIS(Kind.OVERLAY, 1.0F, 1.0F, 0x000000),
    FROST(Kind.OVERLAY, 0.9F, 1.5F, 0xD8F4FF),
    BLOOD(Kind.OVERLAY, 0.9F, 0.3F, 0x8A0000),
    BURN(Kind.OVERLAY, 0.8F, 0.5F, 0xFF6A00),
    SCANLINES(Kind.OVERLAY, 0.35F, 0.3F, 0x000000),
    GRAIN(Kind.OVERLAY, 0.25F, 0.3F, 0xFFFFFF),
    GRADIENT(Kind.OVERLAY, 0.8F, 0.5F, 0x000000),
    IMAGE(Kind.OVERLAY, 1.0F, 0.5F, 0xFFFFFF),
    GRAYSCALE(Kind.POST, 1.0F, 0.5F, 0),
    SEPIA(Kind.POST, 1.0F, 0.5F, 0),
    INVERT(Kind.POST, 1.0F, 0.5F, 0),
    BLUR(Kind.POST, 1.0F, 0.5F, 0),
    VIVID(Kind.POST, 1.0F, 0.5F, 0),
    PIXELATE(Kind.POST, 0.5F, 0.5F, 0),
    GLITCH(Kind.POST, 1.0F, 0.2F, 0),
    WAVE(Kind.POST, 1.0F, 0.6F, 0),
    CHROMATIC(Kind.POST, 1.0F, 0.4F, 0),
    SHOCKWAVE(Kind.POST, 1.0F, 0.1F, 0),
    FISHEYE(Kind.POST, 1.0F, 0.5F, 0),
    SWIRL(Kind.POST, 1.0F, 0.8F, 0),
    POSTERIZE(Kind.POST, 1.0F, 0.5F, 0),
    NIGHTVISION(Kind.POST, 1.0F, 0.5F, 0x40FF60),
    THERMAL(Kind.POST, 1.0F, 0.5F, 0),
    DREAM(Kind.POST, 1.0F, 1.0F, 0xFFE8F8),
    EDGES(Kind.POST, 1.0F, 0.5F, 0x000000),
    DOUBLEVISION(Kind.POST, 1.0F, 0.6F, 0),
    MIRROR(Kind.POST, 1.0F, 0.3F, 0),
    HUE(Kind.POST, 1.0F, 0.5F, 0),
    CONTRAST(Kind.POST, 1.0F, 0.5F, 0),
    COLORIZE(Kind.POST, 1.0F, 0.5F, 0x60A0FF),
    RADIALBLUR(Kind.POST, 1.0F, 0.4F, 0),
    VHS(Kind.POST, 1.0F, 0.4F, 0),
    THRESHOLD(Kind.POST, 1.0F, 0.4F, 0xFFFFFF),
    DITHER(Kind.POST, 1.0F, 0.4F, 0),
    UNDERWATER(Kind.POST, 1.0F, 0.8F, 0x2070C0),
    SHARPEN(Kind.POST, 1.0F, 0.4F, 0),
    SHADER(Kind.POST, 1.0F, 0.5F, 0xFFFFFF),
    SHAKE(Kind.CAMERA, 1.0F, 0.2F, 0),
    DRUNK(Kind.CAMERA, 1.0F, 1.0F, 0),
    SPIN(Kind.CAMERA, 1.0F, 0.5F, 0),
    UPSIDEDOWN(Kind.CAMERA, 1.0F, 0.6F, 0),
    ZOOM(Kind.CAMERA, 0.5F, 0.4F, 0),
    WIDE(Kind.CAMERA, 0.5F, 0.4F, 0),
    SWAY(Kind.CAMERA, 1.0F, 1.0F, 0),
    TILT(Kind.CAMERA, 1.0F, 0.6F, 0),
    BOB(Kind.CAMERA, 1.0F, 0.5F, 0),
    EARTHQUAKE(Kind.CAMERA, 1.0F, 0.5F, 0),
    KICK(Kind.CAMERA, 1.0F, 0.0F, 0),
    VERTIGO(Kind.CAMERA, 1.0F, 0.8F, 0),
    BREATHE(Kind.CAMERA, 1.0F, 1.0F, 0),
    CAMERA(Kind.CAMERA, 1.0F, 0.5F, 0);

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
