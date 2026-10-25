package pl.gemshop.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.Mark;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void gemshop$hideMarkedPlayer(LivingEntity entity, float yaw, float tickDelta,
                                          MatrixStack matrices, VertexConsumerProvider consumers,
                                          int light, CallbackInfo ci) {
        if (Mark.active() && Mark.id != null && Mark.id.equals(entity.getUuid())) {
            ci.cancel();
        }
    }
}
