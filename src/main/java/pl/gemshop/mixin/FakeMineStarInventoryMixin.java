package pl.gemshop.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.FakeMineStarInventory;

/** Draws and drops MineStar preview items without changing real slot stacks. */
@Mixin(HandledScreen.class)
public abstract class FakeMineStarInventoryMixin {
    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void gembi$drawFakeMineStarItem(DrawContext context, Slot slot, CallbackInfo ci) {
        FakeMineStarInventory.render(context, slot);
    }

    @Inject(method = "onMouseClick", at = @At("HEAD"), cancellable = true)
    private void gembi$dropFakeMineStarItem(Slot slot, int slotId, int button,
                                            SlotActionType actionType, CallbackInfo ci) {
        if (FakeMineStarInventory.consumeDrop(slot, actionType)) ci.cancel();
    }
}
