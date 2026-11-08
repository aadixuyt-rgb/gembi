package pl.gemshop.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.BombDecoy;

/**
 * Routes attacks against the local Bomb snapshot to local health only.
 * The vanilla method is cancelled, so no attack packet is sent to the server.
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class BombAttackMixin {
    @Inject(
            method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void gembi$attackBombSnapshot(PlayerEntity attacker, Entity target, CallbackInfo ci) {
        if (BombDecoy.attack(attacker, target)) {
            if (attacker == net.minecraft.client.MinecraftClient.getInstance().player) {
                attacker.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
            ci.cancel();
        }
    }
}
