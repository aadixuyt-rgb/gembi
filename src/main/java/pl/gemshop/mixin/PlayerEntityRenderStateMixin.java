package pl.gemshop.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pl.gemshop.BombHideFlag;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements BombHideFlag {
    @Unique private boolean gembi$bombHidden;

    @Override
    public boolean gembi$isBombHidden() {
        return gembi$bombHidden;
    }

    @Override
    public void gembi$setBombHidden(boolean hidden) {
        gembi$bombHidden = hidden;
    }
}
