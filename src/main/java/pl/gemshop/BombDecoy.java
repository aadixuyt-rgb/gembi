package pl.gemshop;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.random.Random;

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
    private static long spawnedAt;
    private static int lagStage;
    private static long nextLagUpdateAt;
    private static int localScore;

    private BombDecoy() {}

    public static void reset() {
        removeDecoy();
        FakeMineStarInventory.clear();
        sourceUuid = null;
        ownerWorld = null;
        snapshotCreated = false;
        defeatedName = null;
        noticeUntil = 0L;
        spawnedAt = 0L;
        lagStage = 0;
        nextLagUpdateAt = 0L;
        localScore = 0;
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
            FakeMineStarInventory.clear();
            ownerWorld = null;
            snapshotCreated = false;
            BombHearts.reset();
        }

        // The internal 3-second delay is not shown. The public HUD begins at 0:57.
        if (!Mark.active()) return;
        if (snapshotCreated) {
            if (GemShopMod.cfg.bombMode == 1 && decoy != null && !decoy.isRemoved()) {
                long now = System.currentTimeMillis();
                long age = now - spawnedAt;
                AbstractClientPlayerEntity liveSource = findSource(client.world);
                if (liveSource != null && now >= nextLagUpdateAt) {
                    // In Laging mode the copy catches up in one-second steps,
                    // moving no more than four blocks on each update.
                    double dx = liveSource.getX() - decoy.getX();
                    double dy = liveSource.getY() - decoy.getY();
                    double dz = liveSource.getZ() - decoy.getZ();
                    double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    double step = Math.min(4.0D, distance);
                    if (distance > 1.0E-6D) {
                        double scale = step / distance;
                        decoy.freezeAt(liveSource,
                                decoy.getX() + dx * scale,
                                decoy.getY() + dy * scale,
                                decoy.getZ() + dz * scale);
                    } else {
                        decoy.freezeAt(liveSource, liveSource.getX(), liveSource.getY(), liveSource.getZ());
                    }
                    nextLagUpdateAt = now + 1_000L;
                }
            }
            BombHearts.tick(decoy);
            return;
        }

        AbstractClientPlayerEntity source = findSource(client.world);
        if (source != null) spawnSnapshot(client.world, source);
        BombHearts.tick(decoy);
    }

    private static AbstractClientPlayerEntity findSource(ClientWorld world) {
        for (AbstractClientPlayerEntity player : world.getPlayers()) {
            if (player.getUuid().equals(sourceUuid)) return player;
        }
        return null;
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
        spawnedAt = System.currentTimeMillis();
        lagStage = 0;
        nextLagUpdateAt = spawnedAt + 1_000L;
        playCopySpawnSound();
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

    /** The marked visual is the real player during the 3-second delay, then the local copy. */
    public static boolean isMarkedVisual(AbstractClientPlayerEntity player) {
        if (player == null) return false;
        if (isDecoy(player)) return true;
        return !snapshotCreated && sourceUuid != null && sourceUuid.equals(player.getUuid());
    }

    public static BombDecoyEntity getDecoy() {
        return decoy != null && !decoy.isRemoved() ? decoy : null;
    }

    /**
     * Consumes a local attack on the visual copy and applies client-side HP
     * loss. Returning true tells the mixin to cancel vanilla's network packet.
     */
    public static boolean attack(PlayerEntity attacker, Entity target) {
        if (attacker == null || attacker != MinecraftClient.getInstance().player
                || !Mark.active()) {
            return false;
        }

        // If a client still reports the hidden source as the crosshair target,
        // route that attack to the visible local snapshot instead.
        Entity resolvedTarget = target;
        if (shouldHide(target)) {
            resolvedTarget = getDecoy();
        }
        if (!isDecoy(resolvedTarget)) {
            return false;
        }

        // Wyłącznie lokalny podgląd: suwak podaje serca, Minecraft HP.
        float damage = Math.max(0.5f, Math.min(10.0f, GemShopMod.cfg.previewDamageHearts)) * 2.0f;
        float previousHealth = decoy.getHealth();
        float resultingHealth = Math.max(0.0f, previousHealth - damage);
        // Trigger the normal local hurt response, then restore the exact preview
        // health value so clientDamage cannot apply the configured damage twice.
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            decoy.clientDamage(client.world.getDamageSources().playerAttack(attacker));
            decoy.setHealth(resultingHealth);

            // Mirror the main vanilla player-attack sound families for the
            // local preview: critical, sprint knockback, sword sweep, weak and strong.
            float cooldown = attacker.getAttackCooldownProgress(0.5F);
            boolean critical = attacker.fallDistance > 0.0F
                    && !attacker.isOnGround()
                    && !attacker.isClimbing()
                    && !attacker.isTouchingWater()
                    && !attacker.hasVehicle()
                    && !attacker.isSprinting();
            boolean sweeping = !critical
                    && cooldown > 0.9F
                    && attacker.isOnGround()
                    && !attacker.isSprinting()
                    && attacker.getMainHandStack().isIn(net.minecraft.registry.tag.ItemTags.SWORDS)
                    && attacker.getVelocity().horizontalLengthSquared() < 0.1D;

            net.minecraft.sound.SoundEvent attackSound;
            if (critical) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
            } else if (attacker.isSprinting()) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK;
            } else if (sweeping) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP;
            } else if (cooldown < 0.9F) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
            } else {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
            }

            // Submit the sound directly to the client SoundManager.
            // This avoids relying on ClientWorld's entity/world sound routing.
            float attackPitch = critical ? 1.0F : (sweeping ? 0.9F : (cooldown < 0.9F ? 0.75F : 1.0F));
            client.getSoundManager().play(new PositionedSoundInstance(
                    attackSound,
                    SoundCategory.PLAYERS,
                    1.0F,
                    attackPitch,
                    Random.create(),
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ()
            ));

            // Play the target's hurt response locally at the copy's position.
            // The vanilla attack packet is cancelled by BombAttackMixin, so this
            // explicit sound is needed for every successful hit.
            client.getSoundManager().play(new PositionedSoundInstance(
                    SoundEvents.ENTITY_PLAYER_HURT,
                    SoundCategory.PLAYERS,
                    0.9F,
                    1.0F,
                    Random.create(),
                    decoy.getX(),
                    decoy.getY(),
                    decoy.getZ()
            ));
        }
        BombHearts.onDamage(decoy, previousHealth);

        if (decoy.getHealth() <= 0.0f) {
            defeatedName = Mark.name == null ? decoy.getNameForScoreboard() : Mark.name;
            localScore = 3 + (int) (Math.random() * 13);
            if (GemShopMod.cfg.bombServer == 1) FakeMineStarInventory.activate();
            noticeUntil = System.currentTimeMillis() + 2_000L;
            removeDecoy();
            // Keep the real player's model hidden until the original Bomb timer expires.
        }

        return true;
    }

    public static int noticeScore() { return localScore; }

    public static boolean noticeActive() {
        return defeatedName != null && System.currentTimeMillis() < noticeUntil;
    }

    public static String noticeName() {
        return defeatedName == null ? "" : defeatedName;
    }

    public static long noticeRemaining() {
        return Math.max(0L, noticeUntil - System.currentTimeMillis());
    }

    private static void playCopySpawnSound() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        client.getSoundManager().play(new PositionedSoundInstance(
                SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.75F, 1.15F,
                Random.create(), client.player.getX(), client.player.getY(), client.player.getZ()
        ));
    }

    private static void removeDecoy() {
        if (ownerWorld != null && decoy != null && !decoy.isRemoved()) {
            ownerWorld.removeEntity(decoy.getId(), Entity.RemovalReason.DISCARDED);
        }
        decoy = null;
    }
}
