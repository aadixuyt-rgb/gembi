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
 * Updates health on the local visual copy after a normal client attack.
 * The copy has a client-only entity ID, so the server cannot award a kill.
 */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class BombAttackMixin {
    @Inject(
            method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V",
            at = @At("TAIL")
    )
    private void gembi$damageLocalBombCopy(PlayerEntity attacker, Entity target, CallbackInfo ci) {
        BombDecoy.applyLocalHit(attacker, target);
    }
}
