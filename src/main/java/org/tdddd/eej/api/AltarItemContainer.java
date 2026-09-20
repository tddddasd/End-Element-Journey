package org.tdddd.eej.api;

import net.minecraft.resources.Identifier;
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

    
    default boolean acceptsInsertion(ItemStack stack) {
        if (isMainPedestal()) return true;
        List<String> filters = getFilterData();
        return filters == null || filters.isEmpty() || matchesFilter(stack, filters);
    }

    
    static boolean matchesFilter(ItemStack stack, List<String> filters) {
        if (filters == null || filters.isEmpty()) return true;
        Identifier stackId = stack.getItem().builtInRegistryHolder().key().identifier();
        for (String filter : filters) {
            if (filter == null || filter.isEmpty()) continue;
            try {
                if (stackId.equals(Identifier.parse(filter))) return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }
}
