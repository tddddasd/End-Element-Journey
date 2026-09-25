package org.tdddd.eej.impl.element;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Server-side game rules for element items.
 *
 * <p>Two rules, both driven from {@code ServerTickEvent.Post}:
 * <ul>
 *     <li>every {@link #SWEEP_INTERVAL_TICKS} server ticks every non-creative player loses each element item in
 *     their inventory (all container slots, which in 26.1.2 includes armor, offhand, body armor and saddle);
 *     creative and spectator players keep theirs,</li>
 *     <li>every tick any dropped element item entity is discarded, in every game mode.</li>
 * </ul>
 *
 * <p>Only player inventories are swept: element items inside chests or other containers are left alone, and the
 * dropped-item rule never runs a container scan.
 */
public final class ElementEvents {
    /** How often (in server ticks) the player-inventory sweep runs. */
    public static final int SWEEP_INTERVAL_TICKS = 20;

    private int sweepTimer;

    public ElementEvents() {
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        discardDroppedElements(event);
        if (++this.sweepTimer < SWEEP_INTERVAL_TICKS) {
            return;
        }
        this.sweepTimer = 0;
        sweepPlayerInventories(event);
    }

    /**
     * Element items must never exist as item entities, so this runs every tick rather than only on entity join:
     * an item entity can be spawned directly by a command, a hopper or another mod without an entity-join hook
     * ever seeing an element stack.
     */
    private static void discardDroppedElements(ServerTickEvent.Post event) {
        EntityTypeTest<Entity, ItemEntity> itemEntities = EntityTypeTest.forClass(ItemEntity.class);
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ItemEntity itemEntity : level.getEntities(itemEntities, item -> true)) {
                if (ElementStack.isElement(itemEntity.getItem())) {
                    itemEntity.discard();
                }
            }
        }
    }

    private static void sweepPlayerInventories(ServerTickEvent.Post event) {
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isCreative() || player.isSpectator()) {
                continue;
            }
            Inventory inventory = player.getInventory();
            int size = inventory.getContainerSize();
            for (int slot = 0; slot < size; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && ElementStack.isElement(stack)) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }
}
