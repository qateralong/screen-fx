package com.qateralong.screenfx.client.mixin;

import com.qateralong.screenfx.client.Overlays;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void screenfx$underHud(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        Overlays.renderUnderHud(graphics);
    }
}
