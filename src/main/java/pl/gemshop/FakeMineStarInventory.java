package pl.gemshop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.*;

/**
 * MineStar reward preview. ItemStacks live only in this client-side map;
 * they are never inserted into PlayerInventory or sent to the server.
 */
public final class FakeMineStarInventory {
    private static final Map<Integer, ItemStack> ITEMS = new LinkedHashMap<>();
    private static boolean active;

    private FakeMineStarInventory() {}

    public static void clear() {
        ITEMS.clear();
        active = false;
    }

    public static void activate(BombDecoyEntity defeatedCopy) {
        clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        List<ItemStack> rewards = new ArrayList<>();

        // Preserve the exact armor worn by the defeated copy, including dyed leather,
        // trims, enchantments and other item components.
        if (defeatedCopy != null) {
            for (int armorSlot = 36; armorSlot <= 39; armorSlot++) {
                ItemStack worn = defeatedCopy.getInventory().getStack(armorSlot);
                if (!worn.isEmpty() && !isBannedBania(worn)) {
                    rewards.add(worn.copy());
                }
            }
        }
        // The only head reward is the exact head item worn in armor slot 39 above.
        // Never generate a generic player head when the copy was not wearing one.
        add(rewards, Items.FIREWORK_ROCKET, 64, 64, 64);
        add(rewards, Items.WHITE_WOOL, 36, 64, 23);
        add(rewards, Items.GOLDEN_APPLE, 64, 36);
        add(rewards, Items.RED_BANNER, 16, 7);
        add(rewards, Items.BLAZE_ROD, 13);
        add(rewards, Items.SHEARS, 1);
        add(rewards, Items.COBWEB, 45);
        add(rewards, Items.ELYTRA, 1);

        PlayerInventory inventory = mc.player.getInventory();
        int rewardIndex = 0;
        // Inventory-screen order: main rows first (9-35), then the hotbar (0-8).
        // Only empty slots are used; existing real items are never replaced.
        for (int pass = 0; pass < 2 && rewardIndex < rewards.size(); pass++) {
            int first = pass == 0 ? 9 : 0;
            int last = pass == 0 ? 35 : 8;
            for (int i = first; i <= last && rewardIndex < rewards.size(); i++) {
                if (!inventory.getStack(i).isEmpty()) continue;
                ITEMS.put(i, rewards.get(rewardIndex++));
            }
        }
        active = !ITEMS.isEmpty();
    }

    private static void add(List<ItemStack> out, Item item, int... counts) {
        ItemStack stack = new ItemStack(item);
        if (isBannedBania(stack)) return;
        for (int count : counts) out.add(new ItemStack(item, count));
    }

    // "Bania" is excluded from MineStar rewards: pumpkins and mob/player heads.
    private static boolean isBannedBania(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.equals("pumpkin") || path.equals("carved_pumpkin");
    }

    public static ItemStack getFake(Slot slot) {
        if (!active || slot == null) return ItemStack.EMPTY;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !(mc.currentScreen instanceof InventoryScreen)
                || slot.inventory != mc.player.getInventory()) return ItemStack.EMPTY;
        return ITEMS.getOrDefault(slot.getIndex(), ItemStack.EMPTY);
    }

    /**
     * Fake stacks are presentation-only: block every slot operation that could
     * copy or move them into the real cursor, hotbar, or another inventory.
     * THROW is the one supported action and removes the preview stack.
     */
    public static boolean handleClick(Slot slot, SlotActionType actionType) {
        if (slot == null || getFake(slot).isEmpty()) return false;
        if (actionType == SlotActionType.THROW) {
            ITEMS.remove(slot.getIndex());
            if (ITEMS.isEmpty()) active = false;
        }
        return true;
    }

    public static void render(DrawContext context, Slot slot) {
        ItemStack fake = getFake(slot);
        if (fake.isEmpty()) return;
        context.drawItem(fake, slot.x, slot.y);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, fake, slot.x, slot.y);
    }
}
