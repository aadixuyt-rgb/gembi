package pl.gemshop.mixin;

import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pl.gemshop.BombHideFlag;

@Mixin(PlayerEntityRenderState.class)
public abstract class PlayerEntityRenderStateMixin implements BombHideFlag {
    @Unique private boolean gembi$bombHidden;
    @Unique private float gembi$bombHurtAnimation;

    @Override
    public boolean gembi$isBombHidden() {
        return gembi$bombHidden;
    }

    @Override
    public void gembi$setBombHidden(boolean hidden) {
        gembi$bombHidden = hidden;
    }

    @Override
    public float gembi$getBombHurtAnimation() { return gembi$bombHurtAnimation; }

    @Override
    public void gembi$setBombHurtAnimation(float value) {
        gembi$bombHurtAnimation = Math.max(0.0f, Math.min(1.0f, value));
    }
}
