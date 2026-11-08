package pl.gemshop.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.BombDecoy;

@Mixin(EntityRenderDispatcher.class)
public abstract class BombShadowMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void gembi$hideOriginalBombShadow(MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, Entity entity, float opacity,
            float tickDelta, World world, float radius, CallbackInfo ci) {
        if (BombDecoy.shouldHide(entity)) {
            ci.cancel();
        }
    }
}
