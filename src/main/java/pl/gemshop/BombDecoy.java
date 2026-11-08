package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.UUID;

/**
 * Manages a local-only frozen player snapshot for the Bomb visual.
 * The snapshot is client-side only and never sends attack packets to the server.
 */
public final class BombDecoy {
    private static final int FIRST_LOCAL_ENTITY_ID = -1_000_000_000;
    private static int nextLocalEntityId = FIRST_LOCAL_ENTITY_ID;

    private static UUID sourceUuid;
    private static ClientWorld ownerWorld;
    private static BombDecoyEntity decoy;
    private static boolean snapshotCreated;
    private static String defeatedName;
    private static long noticeUntil;

    private BombDecoy() {}

    public static void reset() {
        removeDecoy();
        sourceUuid = null;
        ownerWorld = null;
        snapshotCreated = false;
        defeatedName = null;
        noticeUntil = 0L;
        BombHearts.reset();
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
            BombHearts.reset();
        }

        // The internal 3-second delay is not shown. The public HUD begins at 0:57.
        if (!Mark.active() || snapshotCreated) {
            BombHearts.tick(decoy);
            return;
        }

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getUuid().equals(sourceUuid)) {
                spawnSnapshot(client.world, player);
                break;
            }
        }
        BombHearts.tick(decoy);
    }

    private static void spawnSnapshot(ClientWorld world, AbstractClientPlayerEntity source) {
        GameProfile sourceProfile = source.getGameProfile();
        GameProfile localProfile = new GameProfile(UUID.randomUUID(), sourceProfile.getName());

        BombDecoyEntity copy = new BombDecoyEntity(world, localProfile, source);
        String decoratedName = source.getDisplayName().getString();
        if (decoratedName != null && !decoratedName.isBlank()) {
            Mark.name = decoratedName;
        }
        copy.setId(nextLocalEntityId--);
        world.addEntity(copy);

        ownerWorld = world;
        decoy = copy;
        snapshotCreated = true;
        defeatedName = null;
        noticeUntil = 0L;
        BombHearts.reset();
        BombHearts.tick(copy);
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
     * Consumes a local attack on the visual copy and applies client-side HP
     * loss. Returning true tells the mixin to cancel vanilla's network packet.
     */
    public static boolean attack(PlayerEntity attacker, Entity target) {
        if (!isDecoy(target) || attacker == null
                || attacker != MinecraftClient.getInstance().player) {
            return false;
        }

        // Wyłącznie lokalny podgląd: suwak podaje serca, Minecraft HP.
        float damage = Math.max(0.5f, Math.min(10.0f, GemShopMod.cfg.previewDamageHearts)) * 2.0f;
        float previousHealth = decoy.getHealth();
        decoy.setHealth(Math.max(0.0f, previousHealth - damage));
        // Status 2 uruchamia standardowy czerwony hurt flash i dźwięk encji.
        decoy.handleStatus((byte) 2);
        // Status encji nie zawsze odtwarza dźwięk dla lokalnego OtherClientPlayerEntity,
        // dlatego hurt sound odtwarzamy jawnie tylko dla kopii.
        decoy.playSound(SoundEvents.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        BombHearts.onDamage(decoy, previousHealth);

        if (decoy.getHealth() <= 0.0f) {
            defeatedName = Mark.name == null ? decoy.getNameForScoreboard() : Mark.name;
            noticeUntil = System.currentTimeMillis() + 2_000L;
            removeDecoy();
            // Keep the real player's model hidden until the original Bomb timer expires.
        }

        return true;
    }

    public static boolean noticeActive() {
        return defeatedName != null && System.currentTimeMillis() < noticeUntil;
    }

    public static String noticeName() {
        return defeatedName == null ? "" : defeatedName;
    }

    public static long noticeRemaining() {
        return Math.max(0L, noticeUntil - System.currentTimeMillis());
    }

    private static void removeDecoy() {
        if (ownerWorld != null && decoy != null && !decoy.isRemoved()) {
            ownerWorld.removeEntity(decoy.getId(), Entity.RemovalReason.DISCARDED);
        }
        decoy = null;
    }
}
