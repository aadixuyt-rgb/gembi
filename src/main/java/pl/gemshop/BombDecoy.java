package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/**
 * Manages a local-only frozen copy for the Bomb visual.
 *
 * This copy exists only in the local ClientWorld. The server does not know it
 * exists, so hitting it changes only its local health and never grants a kill,
 * ranking, or server-side reward.
 */
public final class BombDecoy {
    private static final int FIRST_LOCAL_ENTITY_ID = -1_000_000_000;
    private static int nextLocalEntityId = FIRST_LOCAL_ENTITY_ID;

    private static UUID sourceUuid;
    private static ClientWorld ownerWorld;
    private static BombDecoyEntity decoy;
    private static boolean snapshotCreated;

    private BombDecoy() {}

    public static void reset() {
        removeDecoy();
        sourceUuid = null;
        ownerWorld = null;
        snapshotCreated = false;
    }

    public static void tick() {
        MinecraftClient client = MinecraftClient.getInstance();

        if (Mark.id == null || System.currentTimeMillis() >= Mark.end) {
            reset();
            return;
        }

        if (sourceUuid == null || !sourceUuid.equals(Mark.id)) {
            reset();
            sourceUuid = Mark.id;
        }

        if (client.world == null || client.player == null) return;

        if (ownerWorld != null && ownerWorld != client.world) {
            removeDecoy();
            ownerWorld = null;
            snapshotCreated = false;
        }

        // Mark.active() becomes true after the configured three-second delay.
        if (!Mark.active() || snapshotCreated) return;

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getUuid().equals(sourceUuid)) {
                spawnSnapshot(client.world, player);
                break;
            }
        }
    }

    private static void spawnSnapshot(ClientWorld world, AbstractClientPlayerEntity source) {
        GameProfile sourceProfile = source.getGameProfile();
        GameProfile localProfile = new GameProfile(UUID.randomUUID(), sourceProfile.getName());

        BombDecoyEntity copy = new BombDecoyEntity(world, localProfile, source);
        copy.setId(nextLocalEntityId++);
        world.addEntity(copy);

        ownerWorld = world;
        decoy = copy;
        snapshotCreated = true;
    }

    public static boolean shouldHide(AbstractClientPlayerEntity player) {
        return Mark.active()
                && snapshotCreated
                && sourceUuid != null
                && sourceUuid.equals(player.getUuid());
    }

    public static boolean shouldHide(Entity entity) {
        return entity instanceof AbstractClientPlayerEntity player && shouldHide(player);
    }

    public static boolean isDecoy(Entity entity) {
        return decoy != null && entity == decoy && !decoy.isRemoved();
    }

    public static BombDecoyEntity getDecoy() {
        return decoy != null && !decoy.isRemoved() ? decoy : null;
    }

    /**
     * Applies a cosmetic hit to the local copy after vanilla processes the
     * click. The network packet contains the copy's client-only ID and cannot
     * damage the real player on the server.
     */
    public static void applyLocalHit(PlayerEntity attacker, Entity target) {
        if (!isDecoy(target) || attacker == null) return;

        float cooldown = attacker.getAttackCooldownProgress(0.5f);
        double attackAttribute = attacker.getAttributeValue(EntityAttributes.ATTACK_DAMAGE);
        float damage = (float) (attackAttribute * (0.2f + cooldown * cooldown * 0.8f));
        damage = Math.max(0.5f, damage);

        decoy.setHealth(Math.max(0.0f, decoy.getHealth() - damage));
        decoy.animateDamage(attacker.getYaw());

        if (decoy.getHealth() <= 0.0f) {
            removeDecoy();
            // snapshotCreated stays true: the real player remains hidden locally
            // until Mark's 60-second visual timer expires.
        }
    }

    private static void removeDecoy() {
        if (ownerWorld != null && decoy != null && !decoy.isRemoved()) {
            ownerWorld.removeEntity(decoy.getId(), Entity.RemovalReason.DISCARDED);
        }
        decoy = null;
    }
}
