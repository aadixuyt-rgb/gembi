package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.math.Box;

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
        // Initialize interpolation history to the captured transform as well.
        // Otherwise the renderer can interpolate from the entity's constructor
        // position/rotation (often near the world origin), looking like a teleport
        // and a sudden head turn on the first rendered frames.
        prevYaw = source.getYaw();
        prevPitch = source.getPitch();
        lastRenderX = source.getX();
        lastRenderY = source.getY();
        lastRenderZ = source.getZ();
        setHeadYaw(source.getHeadYaw());
        setBodyYaw(source.getBodyYaw());
        prevHeadYaw = source.getHeadYaw();
        prevBodyYaw = source.getBodyYaw();
        setVelocity(0.0, 0.0, 0.0);
        setSneaking(source.isSneaking());
        setSprinting(source.isSprinting());

        if (source.isGliding()) {
            startGliding();
            // Fall-flying player collision box is compact. LivingEntity#getDimensions
            // is final in this Minecraft version, so set the snapshot box directly.
            double x = getX();
            double y = getY();
            double z = getZ();
            setBoundingBox(new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 0.6, z + 0.3));
        }
        setNoGravity(true);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return capturedSkin;
    }
}
