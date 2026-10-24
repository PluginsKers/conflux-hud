package com.eyreland.conflux.hud.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AnvilScreen.class)
public abstract class AnvilScreenMixin {
    @ModifyExpressionValue(method = "renderLabels", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/player/LocalPlayer;hasInfiniteMaterials()Z"))
    private boolean conflux$showAvailableResultCost(boolean creative) {
        // A server-provided result means the server permits this cost.
        return creative || ((AnvilScreen) (Object) this).getMenu().getSlot(2).hasItem();
    }
}
