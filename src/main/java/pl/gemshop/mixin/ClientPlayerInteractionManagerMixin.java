package pl.gemshop.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.gemshop.BlockGhosts;
import pl.gemshop.Mark;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {
    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void gemshop$rememberMinedBlock(BlockPos pos, Direction direction,
                                             CallbackInfoReturnable<Boolean> cir) {
        if (!Mark.active()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        BlockState state = client.world.getBlockState(pos);
        BlockGhosts.remember(pos, state);
    }
}
