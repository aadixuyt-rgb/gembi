package pl.gemshop.mixin;

import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.model.Model;

/** No custom limb animation is applied to the Bomb copy. */
@Mixin(Model.class)
public abstract class BombPlayerHurtModelMixin {
}
