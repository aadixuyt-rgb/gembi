package pl.gemshop.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
}
