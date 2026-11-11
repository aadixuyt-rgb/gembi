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
            // Keep the visual copy synchronized every client tick in both modes.
            // This avoids the old stationary snapshot / one-second teleport behavior.
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

        // Capture the cooldown before changing preview health or resetting the attack timer.
        float cooldown = attacker.getAttackCooldownProgress(0.5F);

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

            // Vanilla Java special attacks require at least 84.8% cooldown.
            // The previous 0.9 threshold incorrectly played the weak-hit sound
            // for valid critical hits in the 84.8%-90% cooldown range.
            attacker.resetLastAttackedTicks();

            boolean livingTarget = decoy instanceof net.minecraft.entity.LivingEntity;
            boolean critical = livingTarget
                    && cooldown >= 0.848F
                    && attacker.fallDistance > 0.0F
                    && !attacker.isOnGround()
                    && !attacker.isClimbing()
                    && !attacker.isTouchingWater()
                    && !attacker.isSwimming()
                    && !attacker.isGliding()
                    && !attacker.hasVehicle()
                    && !attacker.isSprinting()
                    && !attacker.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.BLINDNESS)
                    && !attacker.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.SLOW_FALLING)
                    && !attacker.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.LEVITATION);
            boolean sprintKnockback = livingTarget
                    && cooldown >= 0.848F
                    && attacker.isSprinting();
            boolean sweeping = livingTarget
                    && !critical
                    && !sprintKnockback
                    && cooldown >= 0.848F
                    && attacker.isOnGround()
                    && !attacker.isSprinting()
                    && attacker.getMainHandStack().isIn(net.minecraft.registry.tag.ItemTags.SWORDS)
                    && attacker.getVelocity().horizontalLengthSquared() < 0.1D;

            net.minecraft.sound.SoundEvent attackSound;
            if (critical) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT;
                attacker.addCritParticles(decoy);
            } else if (sprintKnockback) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK;
            } else if (sweeping) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP;
            } else if (cooldown < 0.848F) {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_WEAK;
            } else {
                attackSound = SoundEvents.ENTITY_PLAYER_ATTACK_STRONG;
            }

            // Keep vanilla pitch at 1.0. Random sound variants are selected by
            // the sound event itself, just like in Minecraft.
            Random attackRandom = Random.create();
            client.getSoundManager().play(new PositionedSoundInstance(
                    attackSound,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F,
                    attackRandom,
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ()
            ));

            // Do not layer ENTITY_PLAYER_HURT over the attack sound. The copy's
            // local damage animation is already triggered by clientDamage above;
            // the extra hurt audio made critical hits sound like two stacked hits.
        }
        BombHearts.onDamage(decoy, previousHealth);

        if (decoy.getHealth() <= 0.0f) {
            // Survival has no fake kill message or score. MineStar preview keeps its local reward UI.
            if (GemShopMod.cfg.bombServer == 1) {
                defeatedName = Mark.name == null ? decoy.getNameForScoreboard() : Mark.name;
                localScore = 3 + (int) (Math.random() * 13);
                FakeMineStarInventory.activate(decoy);
                noticeUntil = System.currentTimeMillis() + 2_000L;
            } else {
                defeatedName = null;
                localScore = 0;
                noticeUntil = 0L;
            }
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

    private static void removeDecoy() {
        if (ownerWorld != null && decoy != null && !decoy.isRemoved()) {
            ownerWorld.removeEntity(decoy.getId(), Entity.RemovalReason.DISCARDED);
        }
        decoy = null;
    }
}
