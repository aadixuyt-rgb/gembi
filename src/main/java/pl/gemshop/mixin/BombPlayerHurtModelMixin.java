package pl.gemshop.mixin;

import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.BombHideFlag;

/**
 * Adds a brief local-only arm and leg flail to the Bomb decoy when hit.
 *
 * The limb fields are declared by BipedEntityModel, not PlayerEntityModel.
 * Targeting the declaring class avoids the inherited-field shadow failure
 * that previously prevented the client from starting.
 */
@Mixin(BipedEntityModel.class)
public abstract class BombPlayerHurtModelMixin {
    @Shadow @Final public ModelPart rightArm;
    @Shadow @Final public ModelPart leftArm;
    @Shadow @Final public ModelPart rightLeg;
    @Shadow @Final public ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("TAIL"))
    private void gembi$animateBombDecoyHurt(BipedEntityRenderState state, CallbackInfo ci) {
        // Only player render states carry this marker; leave all other bipeds untouched.
        if (!(state instanceof BombHideFlag flag)) return;

        float intensity = flag.gembi$getBombHurtAnimation();
        if (intensity <= 0.0f) return;

        float wave = (float) Math.sin(state.age * 2.8f);
        float crossWave = (float) Math.cos(state.age * 2.8f);
        rightArm.pitch += wave * 0.95f * intensity;
        leftArm.pitch -= wave * 0.95f * intensity;
        rightArm.roll += crossWave * 0.62f * intensity;
        leftArm.roll -= crossWave * 0.62f * intensity;
        rightLeg.pitch -= wave * 0.48f * intensity;
        leftLeg.pitch += wave * 0.48f * intensity;
    }
}
