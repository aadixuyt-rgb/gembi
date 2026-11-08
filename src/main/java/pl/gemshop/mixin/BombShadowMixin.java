package pl.gemshop.mixin;

import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.BombDecoy;

@Mixin(EntityRenderDispatcher.class)
public abstract class BombShadowMixin {
    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void gembi$hideOriginalBombShadow(Entity entity, CallbackInfo ci) {
        if (BombDecoy.shouldHide(entity)) {
            ci.cancel();
        }
    }
}
