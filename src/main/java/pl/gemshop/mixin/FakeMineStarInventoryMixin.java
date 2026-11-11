package pl.gemshop.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.gemshop.FakeMineStarInventory;

/** Draws and drops MineStar preview items without changing real slot stacks. */
@Mixin(HandledScreen.class)
public abstract class FakeMineStarInventoryMixin {
    @Shadow protected Slot focusedSlot;

    @Inject(method = "drawMouseoverTooltip(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("HEAD"), cancellable = true)
    private void gembi$showFakeMineStarTooltip(DrawContext context, int mouseX, int mouseY,
                                               CallbackInfo ci) {
        ItemStack fake = FakeMineStarInventory.getFake(focusedSlot);
        if (!fake.isEmpty()) {
            // Use the full copied stack for tooltip text: custom names, enchantments,
            // dye color and item components are identical to the defeated copy's armor.
            context.drawItemTooltip(MinecraftClient.getInstance().textRenderer, fake, mouseX, mouseY);
            ci.cancel();
        }
    }

    @Inject(method = "drawSlot", at = @At("TAIL"))
    private void gembi$drawFakeMineStarItem(DrawContext context, Slot slot, CallbackInfo ci) {
        FakeMineStarInventory.render(context, slot);
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;Lnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"), cancellable = true)
    private void gembi$handleFakeMineStarItemClick(Slot slot,
                                                   SlotActionType actionType,
                                                   CallbackInfo ci) {
        // This overload is used by both mouse actions and the Q drop-key path.
        if (FakeMineStarInventory.handleClick(slot, actionType)) ci.cancel();
    }
}
