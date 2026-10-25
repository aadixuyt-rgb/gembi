package pl.gemshop;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Client-only visual copies of blocks mined while the bomb effect is active. */
public final class BlockGhosts {
    private record Ghost(BlockState state, long expiresAt) {}

    private static final Map<BlockPos, Ghost> GHOSTS = new HashMap<>();

    private BlockGhosts() {}

    public static void remember(BlockPos pos, BlockState state) {
        if (!Mark.active() || state == null || state.isAir()) return;
        GHOSTS.put(pos.toImmutable(), new Ghost(state, Mark.end));
    }

    public static void render(WorldRenderContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || mc.player == null) {
            GHOSTS.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<BlockPos, Ghost>> it = GHOSTS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Ghost> entry = it.next();
            if (now >= entry.getValue().expiresAt() || !Mark.cooldownActive()) {
                it.remove();
            }
        }
        if (GHOSTS.isEmpty() || ctx.matrixStack() == null || ctx.consumers() == null) return;

        Vec3d camera = ctx.camera().getPos();
        MatrixStack matrices = ctx.matrixStack();
        VertexConsumerProvider consumers = ctx.consumers();
        matrices.push();
        try {
            for (Map.Entry<BlockPos, Ghost> entry : GHOSTS.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue().state();
                matrices.push();
                matrices.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
                VertexConsumer solid = consumers.getBuffer(RenderLayer.getSolid());
                mc.getBlockRenderManager().renderBlockAsEntity(
                    state, matrices, solid, 0x00F000F0, OverlayTexture.DEFAULT_UV
                );
                matrices.pop();
            }
        } finally {
            matrices.pop();
        }
    }
}
