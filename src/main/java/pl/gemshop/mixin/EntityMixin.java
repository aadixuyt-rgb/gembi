package pl.gemshop.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gemshop.BombDecoy;
import pl.gemshop.GemShopMod;
import pl.gemshop.Mark;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void gemshop$silenceMarkedPlayerFootsteps(net.minecraft.util.math.BlockPos pos,
                                                       net.minecraft.block.BlockState state,
                                                       CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (Mark.active() && Mark.id != null && self instanceof PlayerEntity
                && Mark.id.equals(self.getUuid())) {
            ci.cancel();
        }
    }

    @Inject(method = "pushAwayFrom", at = @At("HEAD"), cancellable = true)
    private void gemshop$disablePlayerPush(Entity other, CallbackInfo ci) {
        if (GemShopMod.cfg == null || !GemShopMod.cfg.noPush) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Entity self = (Entity) (Object) this;

        // Wyłącza odpychanie tylko między lokalnym graczem a innym graczem.
        // Nie zmienia kolizji ze ścianami, blokami ani mobami.
        if (client.player != null
                && self instanceof PlayerEntity
                && other instanceof PlayerEntity
                && (self == client.player || other == client.player)) {
            ci.cancel();
        }
    }

    // AdixuFX draws its visual overlay from Entity#getBoundingBox().
    // Return an empty box only for the hidden Bomb source, so no overlay is drawn.
    @Inject(method = "getBoundingBox()Lnet/minecraft/util/math/Box;", at = @At("RETURN"), cancellable = true)
    private void gemshop$hideBombSourceHitbox(CallbackInfoReturnable<Box> cir) {
        if (BombDecoy.shouldHide((Entity) (Object) this)) {
            cir.setReturnValue(Box.empty());
        }
    }
}
