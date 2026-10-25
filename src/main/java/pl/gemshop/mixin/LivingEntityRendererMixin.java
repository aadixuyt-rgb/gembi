package pl.gemshop.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.DecoyRenderer;
import pl.gemshop.Mark;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    private static EntityRenderState gemshop$markedState;

    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void gemshop$rememberMarkedState(LivingEntity entity, LivingEntityRenderState state,
                                             float tickDelta, CallbackInfo ci) {
        if (DecoyRenderer.isRenderingDecoy()) {
            // The fake has the same UUID, but must still be rendered.
            if (state == gemshop$markedState) gemshop$markedState = null;
            return;
        }
        if (Mark.active() && Mark.id != null && Mark.id.equals(entity.getUuid())) {
            gemshop$markedState = state;
        } else if (state == gemshop$markedState) {
            gemshop$markedState = null;
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void gemshop$hideOriginal(EntityRenderState state, MatrixStack matrices,
                                      VertexConsumerProvider consumers, int light, CallbackInfo ci) {
        if (!DecoyRenderer.isRenderingDecoy() && state == gemshop$markedState && Mark.active()) {
            ci.cancel();
        }
    }
}
