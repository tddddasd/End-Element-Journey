package org.tdddd.eej.impl.element;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab.TabVisibility;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.tdddd.eej.impl.registry.EejCreativeTabs;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.List;

/**
 * Adds the element items to the eej creative tab.
 *
 * <p>The tab content is built from the data pack registry, so a data pack that registers more elements gets a
 * matching stack for free. Ids are read from the event's {@code ItemDisplayParameters} (a
 * {@code HolderLookup.Provider}), which is what makes the registry visible this early; entries are sorted by id
 * so the order does not depend on data pack loading order.
 */
public final class EejElementCreativeTab {
    private EejElementCreativeTab() {
    }

    /** Hooks {@link #onBuildContents} on the mod event bus. */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EejElementCreativeTab::onBuildContents);
    }

    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(EejCreativeTabs.MAIN_TAB.getKey())) {
            return;
        }

        ItemStack plain = new ItemStack(EejItems.ELEMENT.get());
        event.accept(plain, TabVisibility.PARENT_AND_SEARCH_TABS);

        // The provider carried by the tab parameters does not always hold the eej:element datapack registry yet
        // (a tab can be built before those registries are reachable), so an unreadable registry falls back to the
        // ids eej ships itself instead of silently showing the bare element only.
        List<Identifier> ids = EejElements.allIds(event.getParameters().holders());
        if (ids.isEmpty()) {
            ids = ElementStack.shippedIds();
        }
        for (Identifier id : ids) {
            event.accept(ElementStack.create(id), TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
