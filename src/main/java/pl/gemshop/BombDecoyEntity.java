package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.math.Vec3d;

/** Client-only visual copy that mirrors the marked player's live movement and pose. */
public final class BombDecoyEntity extends OtherClientPlayerEntity {
    private final SkinTextures capturedSkin;
    private final AbstractClientPlayerEntity source;

    public BombDecoyEntity(ClientWorld world, GameProfile profile,
                           AbstractClientPlayerEntity source) {
        super(world, profile);
        this.source = source;
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
        syncFromSource();
    }

    /** Mirrors position, rotations, movement pose and elytra state without teleport effects. */
    private void syncFromSource() {
        if (source == null || source.isRemoved()) return;
        setPosition(source.getX(), source.getY(), source.getZ());
        setYaw(source.getYaw());
        setPitch(source.getPitch());
        setHeadYaw(source.getHeadYaw());
        setBodyYaw(source.getBodyYaw());
        setVelocity(source.getVelocity());
        setVelocityClient(source.getVelocity().x, source.getVelocity().y, source.getVelocity().z);
        setPose(source.getPose());
        setSneaking(source.isSneaking());
        setSprinting(source.isSprinting());
        setGliding(source.isGliding());
        setNoGravity(true);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return capturedSkin;
    }

    @Override
    public void tick() {
        super.tick();
        syncFromSource();
    }
}
