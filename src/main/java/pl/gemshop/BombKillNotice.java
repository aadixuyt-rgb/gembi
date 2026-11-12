package pl.gemshop;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** Local kill notices for MineStar and Duele MS. */
public final class BombKillNotice {
    private BombKillNotice() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(net.minecraft.client.gui.DrawContext context) {
        if (!BombDecoy.noticeActive()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        if (BombDecoy.noticeMode() == 2) {
            renderDuele(context, mc, BombDecoy.noticeName());
        } else {
            renderMineStar(context, mc);
        }
    }

    private static void renderMineStar(net.minecraft.client.gui.DrawContext context, MinecraftClient mc) {
        int centerX = context.getScaledWindowWidth() / 2;
        int top = Math.max(8, context.getScaledWindowHeight() / 5);
        String name = BombDecoy.noticeName();
        int bonus = BombDecoy.noticeScore();
        int rankGain = BombDecoy.noticeRankGain();

        Text title = Text.literal("ZABÓJSTWO").styled(s -> s.withColor(0xFF3B35).withBold(true));
        Text details = Text.literal(name).styled(s -> s.withColor(0xFFFFFF))
                .append(Text.literal("  │  ").styled(s -> s.withColor(0xA7A7A7)))
                .append(Text.literal("+" + bonus).styled(s -> s.withColor(0x55FF55).withBold(true)))
                .append(Text.literal("  │  ").styled(s -> s.withColor(0xA7A7A7)))
                .append(Text.literal("762pkt").styled(s -> s.withColor(0xFFD54A).withBold(true)))
                .append(Text.literal("  │  ").styled(s -> s.withColor(0xA7A7A7)))
                .append(Text.literal("↑" + rankGain).styled(s -> s.withColor(0x55FF55).withBold(true)))
                .append(Text.literal("  │  ").styled(s -> s.withColor(0xA7A7A7)))
                .append(Text.literal("Pozycja: 4636").styled(s -> s.withColor(0xFFFFFF)));

        context.drawCenteredTextWithShadow(mc.textRenderer, title, centerX, top, 0xFFFFFF);
        context.drawCenteredTextWithShadow(mc.textRenderer, details, centerX, top + 13, 0xFFFFFF);
    }

    private static void renderDuele(net.minecraft.client.gui.DrawContext context, MinecraftClient mc, String name) {
        int centerX = context.getScaledWindowWidth() / 2;
        int top = Math.max(8, context.getScaledWindowHeight() / 5);
        int green = 0x55FF55;
        Text title = Text.literal("WYGRANA").styled(s -> s.withColor(green).withBold(true));
        Text subtitle = Text.literal("Pokonałeś gracza ").styled(s -> s.withColor(0xFFFFFF))
                .append(Text.literal(name).styled(s -> s.withColor(0xAAFF33).withBold(true)));

        int titleWidth = mc.textRenderer.getWidth(title);
        int left = centerX - titleWidth / 2;
        drawCup(context, left - 15, top + 1, green);
        drawCup(context, left - 7, top - 3, 0x88FF88);
        drawCup(context, left + titleWidth + 5, top - 3, 0x88FF88);
        drawCup(context, left + titleWidth + 13, top + 1, green);

        context.drawCenteredTextWithShadow(mc.textRenderer, title, centerX, top, 0xFFFFFF);
        context.drawCenteredTextWithShadow(mc.textRenderer, subtitle, centerX, top + 14, 0xFFFFFF);
    }

    /** Tiny pixel trophy icon, drawn without relying on an emoji font. */
    private static void drawCup(net.minecraft.client.gui.DrawContext g, int x, int y, int color) {
        g.fill(x + 2, y, x + 7, y + 2, color);
        g.fill(x + 1, y + 2, x + 8, y + 5, color);
        g.fill(x + 2, y + 5, x + 7, y + 7, color);
        g.fill(x - 1, y + 2, x + 1, y + 5, color);
        g.fill(x + 8, y + 2, x + 10, y + 5, color);
        g.fill(x + 4, y + 7, x + 5, y + 9, color);
        g.fill(x + 2, y + 9, x + 7, y + 10, color);
    }
}
