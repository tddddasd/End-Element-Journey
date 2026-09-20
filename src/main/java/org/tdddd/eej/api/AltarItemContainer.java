package org.tdddd.eej.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;


public interface AltarItemContainer {
    boolean hasItem();

    ItemStack getItem();

    void setItem(ItemStack stack);

    void clearItem();

    List<String> getFilterData();

    void setFilterData(List<String> data);

    boolean isMainPedestal();

    void setMainPedestal(boolean main);

    
    static boolean matchesFilter(ItemStack stack, List<String> filters) {
        if (filters == null || filters.isEmpty()) return true;
        ResourceLocation stackId = stack.getItem().builtInRegistryHolder().key().location();
        for (String filter : filters) {
            if (filter == null || filter.isEmpty()) continue;
            try {
                if (stackId.equals(new ResourceLocation(filter))) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
