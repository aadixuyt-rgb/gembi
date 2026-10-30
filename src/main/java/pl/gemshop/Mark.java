package pl.gemshop;

import java.util.UUID;

/** Local Bomb marker timer. Visual effects are handled by Esp. */
public final class Mark {
    public static final long DELAY = 2_000L;
    public static final long DURATION = 60_000L;
    public static UUID id;
    public static String name;
    public static long startAt;
    public static long end;
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

    public static void start(UUID i, String n) {
        long now = System.currentTimeMillis();
        id = i;
        name = n;
        startAt = now + DELAY;
        end = now + DURATION;
        timeoutUntil = 0L;
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        if (id != null && now >= end && timeoutUntil == 0L) timeoutUntil = now + 1_500L;
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
