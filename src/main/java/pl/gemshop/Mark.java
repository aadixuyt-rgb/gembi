package pl.gemshop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;

import java.util.UUID;

/** Lokalny, wizualny efekt Bomby. Nie zmienia zdrowia ani ruchu gracza na serwerze. */
public final class Mark {
    public static final long DELAY = 2_000L;
    public static final long DURATION = 60_000L;
    public static UUID id;
    public static String name;
    public static long startAt;
    public static long end;
    public static int fakePing = 0;
    private static long nextHitAt;
    private static int fakeHealth = 20;
    private static long timeoutUntil;

    private Mark() {}

    public static boolean cooldownActive() {
        long now = System.currentTimeMillis();
        return id != null && (now < end || now < timeoutUntil);
    }

    public static boolean active() {
        long now = System.currentTimeMillis();
        return id != null && now >= startAt && now < end;
    }

    public static long remaining() {
        return Math.max(0, end - Math.max(System.currentTimeMillis(), startAt));
    }

    public static long delayRemaining() {
        return Math.max(0, startAt - System.currentTimeMillis());
    }

    public static long elapsed() {
        return active() ? Math.max(0, System.currentTimeMillis() - startAt) : 0;
    }

    public static int fakeHealth() { return fakeHealth; }

    public static void start(UUID i, String n) {
        long now = System.currentTimeMillis();
        id = i;
        name = n;
        startAt = now + DELAY;
        end = now + DURATION;
        fakePing = 500 + (int) (Math.random() * 2001);
        fakeHealth = 20;
        timeoutUntil = 0L;
        nextHitAt = startAt + 1_000L;
    }

    public static void tick(MinecraftClient mc) {
        if (id == null) return;
        long now = System.currentTimeMillis();
        if (now >= startAt && now < end && now >= nextHitAt) {
            fakeHealth = Math.max(0, fakeHealth - 1);
            nextHitAt = now + 2_500L;
            if (mc.player != null) mc.player.playSound(SoundEvents.ENTITY_PLAYER_HURT, 0.55f, 0.85f + (float) Math.random() * 0.25f);
        }
        if (now >= end && timeoutUntil == 0L) {
            fakePing = 0;
            timeoutUntil = now + 1_500L;
            if (mc.player != null) mc.player.playSound(SoundEvents.ENTITY_ITEM_BREAK, 0.55f, 1.0f);
        }
        if (timeoutUntil > 0L && now >= timeoutUntil) {
            id = null;
            name = null;
            timeoutUntil = 0L;
        }
    }

    public static String fmt(long ms) {
        long sec = (ms + 999) / 1000;
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }
}
