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
import net.minecraft.world.item.crafting.RecipeMap;
import org.tdddd.eej.impl.client.EejClientRecipeCache;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.soulfire.SoulFirePurificationRecipe;

import java.util.ArrayList;
import java.util.Collection;
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
                new AltarCraftingCategory(registration.getJeiHelpers().getGuiHelper()),
                new SoulFirePurificationCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Collection<RecipeHolder<?>> recipes = availableRecipes();
        registerAltarCrafting(registration, recipes);
        registerSoulFirePurification(registration, recipes);
    }

    /**
     * The loaded recipes, taken from the content NeoForge synced to this client.
     *
     * <p>26.1.2 does not send the recipe list in {@code ClientboundUpdateRecipesPacket} any more, so the client
     * level's {@code RecipeAccess} is a {@code ClientRecipeContainer} rather than a {@code RecipeManager}; the
     * real content arrives through {@code RecipeContentPayload} and {@link EejClientRecipeCache}. The old
     * recipe-manager lookup is kept as a fallback for environments where no sync has happened.
     */
    private static Collection<RecipeHolder<?>> availableRecipes() {
        RecipeMap synced = EejClientRecipeCache.synced();
        if (!synced.values().isEmpty()) {
            return synced.values();
        }
        if (Minecraft.getInstance().level != null
                && Minecraft.getInstance().level.recipeAccess() instanceof RecipeManager manager) {
            return manager.getRecipes();
        }
        return List.of();
    }

    private static void registerAltarCrafting(IRecipeRegistration registration,
                                              Collection<RecipeHolder<?>> recipes) {
        List<AltarCraftingRecipe> wrappers = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
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

    private static void registerSoulFirePurification(IRecipeRegistration registration,
                                                     Collection<RecipeHolder<?>> recipes) {
        List<SoulFirePurificationJeiRecipe> wrappers = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof SoulFirePurificationRecipe recipe)) continue;
            SoulFirePurificationJeiRecipe wrapper = SoulFirePurificationJeiRecipe.of(recipe);
            // Recipes that can never produce an item (the explosive entries) are left out of the JEI list
            // entirely: a recipe browser should only offer transformations that yield something.
            if (wrapper == null || wrapper.getEntries().isEmpty()) continue;
            wrappers.add(wrapper);
        }

        registration.addRecipes(SoulFirePurificationCategory.TYPE, wrappers);
    }

    private static net.minecraft.world.item.crafting.CraftingInput emptyCraftingInput() {
        List<ItemStack> empty = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            empty.add(ItemStack.EMPTY);
        }
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, empty);
    }
}
