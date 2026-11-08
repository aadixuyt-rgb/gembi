package pl.gemshop.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.WorldView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.BombHideFlag;

/**
 * Suppresses the vanilla shadow for the hidden original player.
 * The decoy has its own render state without this flag, so its normal
 * Minecraft shadow is left untouched.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class BombShadowMixin {
    @Inject(
            method = "renderShadow(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/render/entity/state/EntityRenderState;FFLnet/minecraft/world/WorldView;F)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void gembi$hideOriginalBombShadow(MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, EntityRenderState state,
            float opacity, float tickDelta, WorldView world, float radius,
            CallbackInfo ci) {
        if (state instanceof BombHideFlag flag && flag.gembi$isBombHidden()) {
            ci.cancel();
        }
    }
}
