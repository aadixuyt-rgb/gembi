package pl.gemshop;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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

    public static void activate() {
        clear();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        List<ItemStack> rewards = new ArrayList<>();
        List<Item> armor = new ArrayList<>(List.of(
                Items.NETHERITE_BOOTS, Items.NETHERITE_LEGGINGS,
                Items.NETHERITE_CHESTPLATE, Items.NETHERITE_HELMET));
        Collections.shuffle(armor);
        rewards.add(new ItemStack(armor.get(0)));
        rewards.add(new ItemStack(armor.get(1)));
        add(rewards, Items.FIREWORK_ROCKET, 64, 64, 64);
        add(rewards, Items.WHITE_WOOL, 36, 64, 23);
        add(rewards, Items.GOLDEN_APPLE, 64, 36);
        add(rewards, Items.RED_BANNER, 16, 7);
        add(rewards, Items.BLAZE_ROD, 13);
        add(rewards, Items.IRON_AXE, 1);
        add(rewards, Items.SHEARS, 1);
        add(rewards, Items.COBWEB, 45);
        add(rewards, Items.ELYTRA, 1);
        add(rewards, Items.NETHERITE_AXE, 1);

        PlayerInventory inventory = mc.player.getInventory();
        int rewardIndex = 0;
        // Fill only empty player inventory slots; existing real items are untouched.
        for (int i = 0; i < inventory.size() && rewardIndex < rewards.size(); i++) {
            if (i < 0 || i > 35 || !inventory.getStack(i).isEmpty()) continue;
            ITEMS.put(i, rewards.get(rewardIndex++));
        }
        active = !ITEMS.isEmpty();
    }

    private static void add(List<ItemStack> out, Item item, int... counts) {
        for (int count : counts) out.add(new ItemStack(item, count));
    }

    public static ItemStack getFake(Slot slot) {
        if (!active || slot == null) return ItemStack.EMPTY;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !(mc.currentScreen instanceof InventoryScreen)
                || slot.inventory != mc.player.getInventory()) return ItemStack.EMPTY;
        return ITEMS.getOrDefault(slot.getIndex(), ItemStack.EMPTY);
    }

    public static boolean consumeDrop(Slot slot, SlotActionType actionType) {
        if (slot == null || actionType != SlotActionType.THROW || getFake(slot).isEmpty()) return false;
        ITEMS.remove(slot.getIndex());
        if (ITEMS.isEmpty()) active = false;
        return true;
    }

    public static void render(DrawContext context, Slot slot) {
        ItemStack fake = getFake(slot);
        if (fake.isEmpty()) return;
        context.drawItem(fake, slot.x, slot.y);
        context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, fake, slot.x, slot.y);
    }
}
