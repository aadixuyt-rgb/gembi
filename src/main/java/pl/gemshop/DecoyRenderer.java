package pl.gemshop;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

/**
 * Renders a client-side copy of the marked player's player model.
 * The copy is never added to the world; it is only rendered while the bomb is active.
 */
public final class DecoyRenderer {
    private static boolean renderingDecoy;

    private DecoyRenderer() {}

    public static boolean isRenderingDecoy() {
        return renderingDecoy;
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !Mark.active() || Mark.id == null) return;
        if (ctx.matrixStack() == null || ctx.consumers() == null) return;

        AbstractClientPlayerEntity target = null;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (Mark.id.equals(player.getUuid())) {
                target = player;
                break;
            }
        }
        if (target == null) return;

        // Reuse the target profile/UUID so the normal player renderer resolves the same skin.
        OtherClientPlayerEntity decoy = new OtherClientPlayerEntity(mc.world, target.getGameProfile());
        decoy.copyPositionAndRotation(target);
        decoy.setBodyYaw(target.getBodyYaw());
        decoy.setHeadYaw(target.getHeadYaw());
        decoy.setSneaking(target.isSneaking());
        decoy.setPose(target.getPose());
        decoy.setOnGround(target.isOnGround());
        decoy.setInvisible(false);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getEquippedStack(slot);
            decoy.equipStack(slot, stack.copy());
        }

        Vec3d camera = ctx.camera().getPos();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);

        renderingDecoy = true;
        matrices.push();
        try {
            mc.getEntityRenderDispatcher().render(
                decoy,
                decoy.getX() - camera.x,
                decoy.getY() - camera.y,
                decoy.getZ() - camera.z,
                decoy.getYaw(),
                tickDelta,
                matrices,
                consumers,
                0x00F000F0
            );
        } finally {
            matrices.pop();
            renderingDecoy = false;
        }
    }
}
