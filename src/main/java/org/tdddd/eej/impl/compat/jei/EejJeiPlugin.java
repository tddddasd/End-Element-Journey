package org.tdddd.eej.impl.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import org.tdddd.eej.impl.eej;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@JeiPlugin
public class EejJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(eej.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new AltarCraftingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        if (Minecraft.getInstance().level == null) return;
        if (!(Minecraft.getInstance().level.recipeAccess() instanceof RecipeManager manager)) {
            
            
            return;
        }

        List<AltarCraftingRecipe> wrappers = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) continue;
            if (recipe.isSpecial()) continue;

            Map<Ingredient, Integer> counts = new LinkedHashMap<>();
            for (Ingredient ing : recipe.placementInfo().ingredients()) {
                if (!ing.isEmpty())
                    counts.put(ing, counts.getOrDefault(ing, 0) + 1);
            }
            if (counts.isEmpty()) continue;

            List<AltarCraftingRecipe.IngredientEntry> entries = new ArrayList<>();
            for (Map.Entry<Ingredient, Integer> e : counts.entrySet())
                entries.add(new AltarCraftingRecipe.IngredientEntry(e.getKey(), e.getValue()));

            ItemStack output = recipe.assemble(emptyCraftingInput());
            if (!output.isEmpty())
                wrappers.add(new AltarCraftingRecipe(entries, output));
        }

        registration.addRecipes(AltarCraftingCategory.TYPE, wrappers);
    }

    private static net.minecraft.world.item.crafting.CraftingInput emptyCraftingInput() {
        List<ItemStack> empty = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            empty.add(ItemStack.EMPTY);
        }
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, empty);
    }
}
