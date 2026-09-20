package org.tdddd.eej.impl.compat.jei;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;


public class AltarCraftingRecipe {
    private final List<IngredientEntry> inputs;
    private final ItemStack output;

    public AltarCraftingRecipe(List<IngredientEntry> inputs, ItemStack output) {
        this.inputs = inputs;
        this.output = output;
    }

    public List<IngredientEntry> getInputs() { return inputs; }
    public ItemStack getOutput() { return output; }

    public static class IngredientEntry {
        private final List<ItemStack> displayStacks;
        private final int count;

        public IngredientEntry(Ingredient ingredient, int count) {
            this.count = count;
            this.displayStacks = new ArrayList<>();
            for (Holder<Item> holder : ingredient.items().toList()) {
                ItemStack copy = new ItemStack(holder);
                copy.setCount(count);
                displayStacks.add(copy);
            }
        }

        public List<ItemStack> getDisplayStacks() {
            return displayStacks;
        }

        public int getCount() {
            return count;
        }
    }
}
