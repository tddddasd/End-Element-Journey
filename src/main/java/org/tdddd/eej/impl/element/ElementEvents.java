package org.tdddd.eej.impl.element;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.tdddd.eej.impl.registry.EejCreativeTabs;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.List;

/**
 * Game rules that keep element items out of reach of survival players and off the ground, plus the
 * creative tab content of the element item.
 *
 * <p>Element items are meant to be handed out by the game itself, never acquired. Therefore:</p>
 * <ul>
 *     <li>every server tick any dropped element {@link ItemEntity} is discarded, in every game
 *     mode, so an element never exists as a dropped item;</li>
 *     <li>every 20 server ticks every element stack is removed from the inventory of every player
 *     that is not in creative mode. Creative players keep theirs, and containers (chests and the
 *     like) are never touched - only player inventories are swept.</li>
 * </ul>
 */
public class ElementEvents {
    /** How many server ticks pass between two inventory sweeps. */
    private static final int INVENTORY_SWEEP_INTERVAL = 20;

    private int tickCounter;

    /**
     * Registers the listeners of this class on the two Forge event buses.
     *
     * @param modEventBus the mod event bus, used for the creative tab content event
     * @param forgeBus    the Forge event bus, used for the server tick event
     */
    public static void register(IEventBus modEventBus, IEventBus forgeBus) {
        ElementEvents instance = new ElementEvents();
        modEventBus.addListener(instance::onBuildCreativeTabContents);
        forgeBus.addListener(instance::onServerTick);
    }

    /**
     * Adds one plain {@code eej:element} stack plus one stack per registered element to the eej
     * creative tab. The registry is read from the event parameters, so elements contributed by
     * data packs or other mods show up here automatically. Ids are sorted so the order is stable.
     *
     * @param event the creative tab content event
     */
    private void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!EejCreativeTabs.MAIN_TAB.getKey().equals(event.getTabKey())) {
            return;
        }
        event.accept(new ItemStack(EejItems.ELEMENT.get()));
        // The provider carried by the tab parameters does not always hold the eej:element datapack registry yet
        // (the tab can be built before those registries are reachable), so an unreadable registry falls back to
        // the ids eej ships itself instead of silently showing the bare element only.
        List<ResourceLocation> ids = EejElements.allIds(event.getParameters().holders());
        if (ids.isEmpty()) {
            ids = ElementStack.shippedIds();
        }
        for (ResourceLocation id : ids) {
            event.accept(ElementStack.create(id));
        }
    }

    /**
     * Discards dropped element items every tick and sweeps the inventories of non-creative players
     * every {@link #INVENTORY_SWEEP_INTERVAL} ticks.
     *
     * @param event the end of a server tick
     */
    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        this.tickCounter++;
        boolean sweepInventories = this.tickCounter % INVENTORY_SWEEP_INTERVAL == 0;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof ItemEntity itemEntity && ElementStack.isElement(itemEntity.getItem())) {
                    itemEntity.discard();
                }
            }
            if (!sweepInventories) {
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (!player.isCreative()) {
                    removeElementsFromInventory(player);
                }
            }
        }
    }

    /**
     * Removes every element stack from a player's inventory, including armor and offhand slots.
     *
     * @param player the player to sweep
     */
    private static void removeElementsFromInventory(Player player) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        for (int slot = 0; slot < size; slot++) {
            if (ElementStack.isElement(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }
}
