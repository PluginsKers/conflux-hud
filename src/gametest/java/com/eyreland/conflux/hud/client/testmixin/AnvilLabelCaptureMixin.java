package com.eyreland.conflux.hud.client.testmixin;

import com.eyreland.conflux.hud.client.AnvilClientGameTest;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class AnvilLabelCaptureMixin {
    @Inject(method = "width(Lnet/minecraft/network/chat/FormattedText;)I", at = @At("HEAD"))
    private void captureAnvilLabel(FormattedText text, CallbackInfoReturnable<Integer> ci) {
        AnvilClientGameTest.capture(text);
    }
}
