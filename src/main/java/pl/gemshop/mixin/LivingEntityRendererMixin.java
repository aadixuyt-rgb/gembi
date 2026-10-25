package pl.gemshop.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.DecoyRenderer;
import pl.gemshop.Mark;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void gemshop$hideMarkedPlayer(LivingEntity entity, LivingEntityRenderState state,
                                          float tickDelta, CallbackInfo ci) {
        // The decoy uses the same profile/UUID, so do not hide it while rendering the copy.
        if (!DecoyRenderer.isRenderingDecoy() && Mark.active() && Mark.id != null
                && Mark.id.equals(entity.getUuid())) {
            state.invisible = true;
        }
    }
}
