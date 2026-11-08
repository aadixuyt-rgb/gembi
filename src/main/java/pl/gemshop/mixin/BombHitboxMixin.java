package pl.gemshop.mixin;

import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gemshop.BombDecoy;

@Mixin(Entity.class)
public abstract class BombHitboxMixin {
    @Inject(method = "canHit()Z", at = @At("HEAD"), cancellable = true)
    private void gembi$skipHiddenBombSourceTargeting(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (BombDecoy.shouldHide(entity)) {
            cir.setReturnValue(false);
        }
    }

    // canHit() only affects targeting. isCollidable() also excludes the
    // invisible source from entity collision checks while the decoy is active.
    @Inject(method = "isCollidable()Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void gembi$skipHiddenBombSourceCollision(CallbackInfoReturnable<Boolean> cir) {
        Entity entity = (Entity) (Object) this;
        if (BombDecoy.shouldHide(entity)) {
            cir.setReturnValue(false);
        }
    }
}