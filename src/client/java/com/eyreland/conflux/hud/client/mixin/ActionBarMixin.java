package com.eyreland.conflux.hud.client.mixin;

import com.eyreland.conflux.hud.client.ConfluxHudClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class ActionBarMixin {
    @ModifyExpressionValue(method = "setActionBarText", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/protocol/game/ClientboundSetActionBarTextPacket;text()Lnet/minecraft/network/chat/Component;"))
    private Component conflux$claim(Component message) { return ConfluxHudClient.actionBar(message); }
}
