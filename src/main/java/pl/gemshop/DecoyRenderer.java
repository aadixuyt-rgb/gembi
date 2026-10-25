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

/** Renders a client-side player copy fixed at the position where Mark becomes active. */
public final class DecoyRenderer {
    private static boolean renderingDecoy;
    private static java.util.UUID capturedId;
    private static double fixedX, fixedY, fixedZ;
    private static float fixedYaw, fixedBodyYaw, fixedHeadYaw, fixedPitch;
    private static boolean fixedSneaking;

    private DecoyRenderer() {}

    public static boolean isRenderingDecoy() { return renderingDecoy; }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null || !Mark.active() || Mark.id == null) {
            capturedId = null;
            return;
        }
        if (ctx.matrixStack() == null || ctx.consumers() == null) return;

        AbstractClientPlayerEntity target = null;
        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (Mark.id.equals(player.getUuid())) { target = player; break; }
        }
        if (target == null) return;

        // Capture once after the 2-second delay. Never follow the moving target afterwards.
        if (!Mark.id.equals(capturedId)) {
            capturedId = Mark.id;
            fixedX = target.getX(); fixedY = target.getY(); fixedZ = target.getZ();
            fixedYaw = target.getYaw(); fixedPitch = target.getPitch();
            fixedBodyYaw = target.getBodyYaw(); fixedHeadYaw = target.getHeadYaw();
            fixedSneaking = target.isSneaking();
        }

        OtherClientPlayerEntity decoy = new OtherClientPlayerEntity(mc.world, target.getGameProfile());
        decoy.setPosition(fixedX, fixedY, fixedZ);
        decoy.setYaw(fixedYaw);
        decoy.setPitch(fixedPitch);
        decoy.setBodyYaw(fixedBodyYaw);
        decoy.setHeadYaw(fixedHeadYaw);
        decoy.setSneaking(fixedSneaking);
        decoy.setPose(target.getPose());
        decoy.setOnGround(true);
        decoy.setInvisible(false);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = target.getEquippedStack(slot);
            decoy.equipStack(slot, stack.copy());
        }

        Vec3d camera = ctx.camera().getPos();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        renderingDecoy = true;
        matrices.push();
        try {
            mc.getEntityRenderDispatcher().render(decoy,
                fixedX - camera.x, fixedY - camera.y, fixedZ - camera.z,
                fixedYaw, matrices, consumers, 0x00F000F0);
        } finally {
            matrices.pop();
            renderingDecoy = false;
        }
    }
}
