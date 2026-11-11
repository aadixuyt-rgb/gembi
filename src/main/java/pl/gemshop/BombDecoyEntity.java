package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.SkinTextures;

/**
 * Client-only frozen snapshot. All player rotations and pose are copied before
 * the entity is inserted into the world, so it appears already facing correctly.
 */
public final class BombDecoyEntity extends OtherClientPlayerEntity {
    private final SkinTextures capturedSkin;

    public BombDecoyEntity(ClientWorld world, GameProfile profile,
                           AbstractClientPlayerEntity source) {
        super(world, profile);
        this.capturedSkin = source.getSkinTextures();
        setCustomName(source.getDisplayName());
        setCustomNameVisible(true);
        setNoGravity(true);

        for (int slot = 0; slot < source.getInventory().size(); slot++) {
            getInventory().setStack(slot, source.getInventory().getStack(slot).copy());
        }
        getInventory().selectedSlot = source.getInventory().selectedSlot;

        var maxHealth = getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(source.getMaxHealth());
        setHealth(source.getHealth());
        setAbsorptionAmount(source.getAbsorptionAmount());

        // Capture exact orientation once, before spawn: no post-spawn tracking/teleporting.
        refreshPositionAndAngles(source.getX(), source.getY(), source.getZ(),
                source.getYaw(), source.getPitch());
        setHeadYaw(source.getHeadYaw());
        setBodyYaw(source.getBodyYaw());
        setVelocity(0.0, 0.0, 0.0);
        setSneaking(source.isSneaking());
        setSprinting(source.isSprinting());
        if (source.isGliding()) startGliding();
        setNoGravity(true);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return capturedSkin;
    }
}
