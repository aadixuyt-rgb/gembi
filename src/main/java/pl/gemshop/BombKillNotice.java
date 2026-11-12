package pl.gemshop;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/** MineStar-style local kill notification. */
public final class BombKillNotice {
    private BombKillNotice() {}

    public static void register() {
        HudRenderCallback.EVENT.register((context, tickCounter) -> render(context));
    }

    private static void render(net.minecraft.client.gui.DrawContext context) {
        if (!BombDecoy.noticeActive()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

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
}
