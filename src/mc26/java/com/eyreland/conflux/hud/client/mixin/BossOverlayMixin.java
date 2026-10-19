package com.eyreland.conflux.hud.client.mixin;

import com.eyreland.conflux.hud.client.ConfluxHudClient;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import java.util.Collection;

@Mixin(BossHealthOverlay.class)
public abstract class BossOverlayMixin {
    @ModifyExpressionValue(method = "extractRenderState", at = @At(value = "INVOKE", target = "Ljava/util/Map;values()Ljava/util/Collection;"))
    private Collection<LerpingBossEvent> conflux$visibleBars(Collection<LerpingBossEvent> bars) {
        return bars.stream().filter(bar -> ConfluxHudClient.territory(bar) == null).toList();
    }
}
