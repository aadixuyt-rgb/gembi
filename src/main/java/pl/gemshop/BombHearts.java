package pl.gemshop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Health icons rendered only above the local Bomb snapshot. Damage creates
 * short falling-heart particles; ordinary server players are never decorated.
 */
public final class BombHearts {
    private static final Identifier CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier ABSORB_FULL = Identifier.ofVanilla("hud/heart/absorbing_full");
    private static final Identifier ABSORB_HALF = Identifier.ofVanilla("hud/heart/absorbing_half");

    private record FallingHeart(float offsetX, float offsetY, long startedAt) {}
    private static final List<FallingHeart> FALLING = new ArrayList<>();
    private static BombDecoyEntity trackedDecoy;
    private static float visualHealth;
    private static float damageTrailHealth;
    private static float damageTrailStartHealth;
    private static long damageTrailStartedAt;

    private BombHearts() {}

    public static void reset() {
        FALLING.clear();
        trackedDecoy = null;
        visualHealth = 0.0f;
        damageTrailHealth = 0.0f;
        damageTrailStartHealth = 0.0f;
        damageTrailStartedAt = 0L;
    }

    private static void track(BombDecoyEntity decoy) {
        if (decoy != null && trackedDecoy != decoy) {
            trackedDecoy = decoy;
            visualHealth = decoy.getHealth();
            damageTrailHealth = visualHealth;
            damageTrailStartHealth = visualHealth;
            damageTrailStartedAt = 0L;
        }
    }

    /** Health shown in the local visual label; never changes entity/server health. */
    public static float visualHealth(BombDecoyEntity decoy) {
        track(decoy);
        return trackedDecoy == decoy ? Math.max(0.0f, visualHealth) : 0.0f;
    }

    public static void tick(BombDecoyEntity decoy) {
        if (decoy == null || decoy.isRemoved()) {
            FALLING.clear();
            return;
        }
        track(decoy);
        long now = System.currentTimeMillis();
        Iterator<FallingHeart> it = FALLING.iterator();
        while (it.hasNext()) {
            if (now - it.next().startedAt() > 850L) it.remove();
        }
    }

    public static void onDamage(BombDecoyEntity decoy, float previousHealth) {
        if (decoy == null) return;
        // Initialize from the PRE-hit health. This callback runs after the
        // entity health has already been reduced, so track() must not seed from
        // decoy.getHealth() on the first hit.
        if (trackedDecoy != decoy) {
            trackedDecoy = decoy;
            visualHealth = previousHealth;
            damageTrailHealth = previousHealth;
        }
        float lost = Math.max(0.0f, previousHealth - decoy.getHealth());
        if (lost <= 0.0f) return;
        damageTrailStartHealth = Math.max(visualHealth, previousHealth);
        damageTrailHealth = damageTrailStartHealth;
        damageTrailStartedAt = System.currentTimeMillis();
        visualHealth = decoy.getHealth();
        int count = Math.min(8, Math.max(1, (int) Math.ceil(lost / 2.0f)));
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            float x = (i - (count - 1) / 2.0f) * 5.0f;
            float y = -((i % 2) * 2.5f);
            FALLING.add(new FallingHeart(x, y, now + i * 22L));
        }
    }

    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        BombDecoyEntity decoy = BombDecoy.getDecoy();
        if (mc.player == null || mc.world == null || decoy == null
                || mc.options.hudHidden || mc.currentScreen != null) return;

        if (!Esp.isProjectionValid()) return;
        float partial = mc.getRenderTickCounter().getTickDelta(true);
        Vec3d pos = decoy.getLerpedPos(partial);
        Vector4f projected = new Vector4f(
                (float) (pos.x - Esp.cameraPosition().x),
                (float) (decoy.getBoundingBox().maxY + 0.55 - Esp.cameraPosition().y),
                (float) (pos.z - Esp.cameraPosition().z),
                1.0f
        );
        Esp.viewProjection().transform(projected);
        if (projected.w <= 0.05f) return;

        int sw = mc.getWindow().getScaledWidth();
        int sx = Math.round((projected.x / projected.w * 0.5f + 0.5f) * sw);
        int sy = Math.round((1.0f - (projected.y / projected.w * 0.5f + 0.5f))
                * mc.getWindow().getScaledHeight());

        float shownHealth = visualHealth(decoy);
        // The red trailing section smoothly drains toward the real health value.
        float trailHealth = shownHealth;
        if (damageTrailStartedAt > 0L) {
            float t = Math.min(1.0f, (System.currentTimeMillis() - damageTrailStartedAt) / 520.0f);
            t = t * t * (3.0f - 2.0f * t);
            trailHealth = damageTrailStartHealth + (shownHealth - damageTrailStartHealth) * t;
            damageTrailHealth = trailHealth;
            if (t >= 1.0f) damageTrailStartedAt = 0L;
        }
        // Puste kontenery pokazują pełne MAX_HEALTH kopii, a wypełnienie jej aktualne HP.
        int healthIcons = (int) Math.ceil(Math.max(0.0f, decoy.getMaxHealth()) / 2.0f);
        int absorptionIcons = (int) Math.ceil(Math.max(0.0f, decoy.getAbsorptionAmount()) / 2.0f);
        int total = healthIcons + absorptionIcons;
        int rows = Math.max(1, (total + 9) / 10);
        int yTop = sy - rows * 10 - 3;

        for (int i = 0; i < total; i++) {
            int row = i / 10;
            int column = i % 10;
            int x = sx - Math.min(10, total - row * 10) * 9 / 2 + column * 9;
            int y = yTop + row * 10;
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, CONTAINER, x, y, 9, 9);

            if (i < healthIcons) {
                float remaining = shownHealth - i * 2.0f;
                if (remaining >= 2.0f) {
                    ctx.drawGuiTexture(RenderLayer::getGuiTextured, FULL, x, y, 9, 9);
                } else if (remaining > 0.0f) {
                    ctx.drawGuiTexture(RenderLayer::getGuiTextured, HALF, x, y, 9, 9);
                }
                float redRemaining = trailHealth - i * 2.0f;
                if (redRemaining > Math.max(0.0f, remaining)) {
                    // Red translucent overlay marks the HP that was just lost.
                    ctx.fill(x + 1, y + 1, x + 8, y + 8, 0x99FF263D);
                }
            } else {
                float remaining = decoy.getAbsorptionAmount() - (i - healthIcons) * 2.0f;
                ctx.drawGuiTexture(RenderLayer::getGuiTextured,
                        remaining >= 2.0f ? ABSORB_FULL : ABSORB_HALF, x, y, 9, 9);
            }
        }

        long now = System.currentTimeMillis();
        Iterator<FallingHeart> it = FALLING.iterator();
        while (it.hasNext()) {
            FallingHeart heart = it.next();
            long age = now - heart.startedAt();
            if (age < 0) continue;
            if (age > 850L) {
                it.remove();
                continue;
            }
            float fade = 1.0f - age / 850.0f;
            int alpha = Math.round(255.0f * fade);
            int color = (alpha << 24) | 0x00FF435E;
            int px = sx + Math.round(heart.offsetX() + (float) Math.sin(age * 0.012 + heart.offsetX()) * 3.0f);
            int py = sy - 5 + Math.round(heart.offsetY() + age * 0.035f);
            Gfx.text(ctx, mc.textRenderer, "♥", px - 4, py - 4, color, 0.85f, true);
        }
    }
}
