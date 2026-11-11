package pl.gemshop.mixin;

import net.minecraft.client.render.entity.model.BipedEntityModel;
import org.spongepowered.asm.mixin.Mixin;

/** Keeps the Bomb copy's limbs in Minecraft's normal pose without custom flailing. */
@Mixin(BipedEntityModel.class)
public abstract class BombPlayerHurtModelMixin {
}
