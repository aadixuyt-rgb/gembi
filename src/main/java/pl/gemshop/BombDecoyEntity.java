package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

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
        setCustomName(net.minecraft.text.Text.literal(profile.getName()));
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

    /**
     * Keep the visual copy moving with the real marked player. The original
     * entity is hidden locally, but its live position/rotation/pose remains
     * the source of truth so the copy does not look frozen in place.
     */
    public void followSource(AbstractClientPlayerEntity source) {
        setPosition(source.getX(), source.getY(), source.getZ());
        setYaw(source.getYaw());
        setPitch(source.getPitch());
        setHeadYaw(source.getHeadYaw());
        setBodyYaw(source.getBodyYaw());
        setVelocity(source.getVelocity());
        setSneaking(source.isSneaking());
        setSprinting(source.isSprinting());

        if (source.isGliding() && !isGliding()) startGliding();
        else if (!source.isGliding() && isGliding()) stopGliding();

        if (source.isOnGround()) setOnGround(true);
        else setOnGround(false);

        // Keep the same visible pose/size while the player moves.
        if (source.isGliding()) {
            double x = getX(), y = getY(), z = getZ();
            setBoundingBox(new Box(x - 0.3, y, z - 0.3, x + 0.3, y + 0.6, z + 0.3));
        }
    }

    @Override
    public SkinTextures getSkinTextures() {
        return capturedSkin;
    }
}
