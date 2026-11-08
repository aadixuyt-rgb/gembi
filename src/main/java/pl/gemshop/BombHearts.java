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

    private BombHearts() {}

    public static void reset() {
        FALLING.clear();
    }

    public static void tick(BombDecoyEntity decoy) {
        if (decoy == null || decoy.isRemoved()) {
            FALLING.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<FallingHeart> it = FALLING.iterator();
        while (it.hasNext()) {
            if (now - it.next().startedAt() > 850L) it.remove();
        }
    }

    public static void onDamage(BombDecoyEntity decoy, float previousHealth) {
        if (decoy == null) return;
        float lost = Math.max(0.0f, previousHealth - decoy.getHealth());
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

        int healthIcons = (int) Math.ceil(Math.max(0.0f, decoy.getHealth()) / 2.0f);
        int absorptionIcons = (int) Math.ceil(Math.max(0.0f, decoy.getAbsorptionAmount()) / 2.0f);
        int total = healthIcons + absorptionIcons;
        int perRow = Math.min(10, Math.max(1, total));
        int rows = Math.max(1, (total + 9) / 10);
        int heartWidth = perRow * 9;
        int startX = sx - heartWidth / 2;
        int yTop = sy - rows * 10 - 3;

        for (int i = 0; i < total; i++) {
            int row = i / 10;
            int column = i % 10;
            int x = sx - Math.min(10, total - row * 10) * 9 / 2 + column * 9;
            int y = yTop + row * 10;
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, CONTAINER, x, y, 9, 9);

            if (i < healthIcons) {
                float remaining = decoy.getHealth() - i * 2.0f;
                ctx.drawGuiTexture(RenderLayer::getGuiTextured,
                        remaining >= 2.0f ? FULL : HALF, x, y, 9, 9);
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
            ctx.getMatrices().push();
            ctx.getMatrices().translate(px, py, 0);
            ctx.getMatrices().scale(0.75f, 0.75f, 1.0f);
            ctx.drawGuiTexture(RenderLayer::getGuiTextured, FULL, -4, -4, 9, 9);
            ctx.getMatrices().pop();
        }
    }
}
