package pl.gemshop;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/** Draws the Bomb health card over the fixed decoy, not over the moving original. */
public final class DecoyHud {
    private static final Matrix4f VP = new Matrix4f();
    private static Vec3d cameraPos = Vec3d.ZERO;
    private static boolean valid;
    private DecoyHud() {}

    public static void capture(WorldRenderContext ctx) {
        Camera camera = ctx.camera();
        cameraPos = camera.getPos();
        Matrix4f view = new Matrix4f().rotation(camera.getRotation().conjugate(new Quaternionf()));
        new Matrix4f(ctx.projectionMatrix()).mul(view, VP);
        valid = true;
    }

    public static void render(DrawContext g) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!valid || mc.player == null || mc.world == null || !Mark.active() || !DecoyRenderer.hasPosition() || mc.options.hudHidden) return;
        Vec3d point = new Vec3d(DecoyRenderer.x(), DecoyRenderer.y() + 2.15, DecoyRenderer.z()).subtract(cameraPos);
        Vector4f v = new Vector4f((float)point.x, (float)point.y, (float)point.z, 1f);
        VP.transform(v);
        if (v.w <= 0.05f) return;
        int sw = mc.getWindow().getScaledWidth(), sh = mc.getWindow().getScaledHeight();
        float cx = (v.x / v.w * 0.5f + 0.5f) * sw;
        float top = (1f - (v.y / v.w * 0.5f + 0.5f)) * sh;
        int w = 104, h = 27;
        int x = Math.max(2, Math.min(sw - w - 2, Math.round(cx - w / 2f)));
        int y = Math.max(2, Math.min(sh - h - 2, Math.round(top - h)));
        Gfx.rr(g, x, y, w, h, 4, 0xE60B0C12);
        Gfx.outlineRR(g, x, y, w, h, 4, 0xFFFF3B4E);
        Gfx.text(g, mc.textRenderer, mc.textRenderer.trimToWidth(Mark.name == null ? "Player" : Mark.name, w - 8), x + 4, y + 3, 0xFFFFFFFF, 0.75f, true);
        int hp = Math.max(0, Mark.fakeHealth());
        Gfx.text(g, mc.textRenderer, hp + "/20 HP", x + 4, y + 13, 0xFFFF6678, 0.7f, true);
        g.fill(x + 4, y + 23, x + w - 4, y + 25, 0xFF383843);
        if (hp > 0) g.fill(x + 4, y + 23, x + 4 + (w - 8) * hp / 20, y + 25, 0xFFFF4058);
    }
}
