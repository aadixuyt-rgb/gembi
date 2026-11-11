package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * A frozen, client-only snapshot of a player. It is not a server entity and
 * cannot create server-side damage, kill credit, or score events.
 */
public final class BombDecoyEntity extends OtherClientPlayerEntity {
    private final SkinTextures capturedSkin;
    private double snapshotX;
    private double snapshotY;
    private double snapshotZ;
    private float snapshotYaw;
    private float snapshotPitch;
    private float snapshotHeadYaw;
    private float snapshotBodyYaw;

    public BombDecoyEntity(ClientWorld world, GameProfile profile,
                           AbstractClientPlayerEntity source) {
        super(world, profile);
        this.capturedSkin = source.getSkinTextures();
        this.snapshotX = source.getX();
        this.snapshotY = source.getY();
        this.snapshotZ = source.getZ();
        this.snapshotYaw = source.getYaw();
        this.snapshotPitch = source.getPitch();
        this.snapshotHeadYaw = source.getHeadYaw();
        this.snapshotBodyYaw = source.getBodyYaw();

        setCustomName(source.getDisplayName());
        setCustomNameVisible(true);
        setNoGravity(true);
        setVelocity(Vec3d.ZERO);
        setPosition(snapshotX, snapshotY, snapshotZ);
        setYaw(snapshotYaw);
        setPitch(snapshotPitch);
        setHeadYaw(snapshotHeadYaw);
        setBodyYaw(snapshotBodyYaw);
        setPose(source.getPose());
        setSneaking(source.isSneaking());
        setSprinting(source.isSprinting());

        var maxHealth = getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(source.getMaxHealth());
        }
        setHealth(source.getHealth());
        setAbsorptionAmount(source.getAbsorptionAmount());

        for (int slot = 0; slot < source.getInventory().size(); slot++) {
            getInventory().setStack(slot, source.getInventory().getStack(slot).copy());
        }
        getInventory().selectedSlot = source.getInventory().selectedSlot;
    }

    /** Change the local freeze point without moving or notifying the server. */
    public void freezeAt(AbstractClientPlayerEntity source) {
        freezeAt(source, source.getX(), source.getY(), source.getZ());
    }

    /** Update the snapshot orientation while placing it at a locally limited position. */
    public void freezeAt(AbstractClientPlayerEntity source, double x, double y, double z) {
        snapshotX = x;
        snapshotY = y;
        snapshotZ = z;
        snapshotYaw = source.getYaw();
        snapshotPitch = source.getPitch();
        snapshotHeadYaw = source.getHeadYaw();
        snapshotBodyYaw = source.getBodyYaw();
        setPosition(snapshotX, snapshotY, snapshotZ);
        setYaw(snapshotYaw);
        setPitch(snapshotPitch);
        setHeadYaw(snapshotHeadYaw);
        setBodyYaw(snapshotBodyYaw);
        setVelocity(Vec3d.ZERO);
        setVelocityClient(0.0, 0.0, 0.0);
    }

    @Override
    public SkinTextures getSkinTextures() {
        return capturedSkin;
    }

    /** Keep the snapshot suspended at its captured position and immune to knockback. */
    @Override
    public void tick() {
        super.tick();
        setPosition(snapshotX, snapshotY, snapshotZ);
        setVelocity(Vec3d.ZERO);
        setVelocityClient(0.0, 0.0, 0.0);
        setYaw(snapshotYaw);
        setPitch(snapshotPitch);
        setHeadYaw(snapshotHeadYaw);
        setBodyYaw(snapshotBodyYaw);
        setNoGravity(true);
    }
}
