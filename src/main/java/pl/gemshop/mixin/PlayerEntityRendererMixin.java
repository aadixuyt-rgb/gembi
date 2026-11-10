package pl.gemshop.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.BombDecoy;
import pl.gemshop.BombHideFlag;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(
            method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void gembi$markBombSourceHidden(AbstractClientPlayerEntity player,
                                            PlayerEntityRenderState state,
                                            float tickDelta,
                                            CallbackInfo ci) {
        BombHideFlag flag = (BombHideFlag) state;
        flag.gembi$setBombHidden(BombDecoy.shouldHide(player));
        flag.gembi$setBombHurtAnimation(BombDecoy.hurtAnimationProgress(player));
    }
}
