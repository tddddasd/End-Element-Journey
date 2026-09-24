package org.tdddd.eej.impl.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.recipe.SoulFireOutput;
import org.tdddd.eej.impl.recipe.SoulFirePurificationRecipe;
import org.tdddd.eej.impl.registry.EejRecipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@JeiPlugin
public class EejJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(eej.MODID, "jei_plugin");
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
        RecipeManager manager = Minecraft.getInstance().level.getRecipeManager();
        List<CraftingRecipe> recipes = manager.getAllRecipesFor(RecipeType.CRAFTING);
        List<AltarCraftingRecipe> wrappers = new ArrayList<>();

        for (CraftingRecipe recipe : recipes) {
            if (recipe.isSpecial()) continue;
            Map<Ingredient, Integer> counts = new LinkedHashMap<>();
            for (Ingredient ing : recipe.getIngredients()) {
                if (!ing.isEmpty())
                    counts.put(ing, counts.getOrDefault(ing, 0) + 1);
            }
            if (counts.isEmpty()) continue;

            List<AltarCraftingRecipe.IngredientEntry> entries = new ArrayList<>();
            for (Map.Entry<Ingredient, Integer> e : counts.entrySet())
                entries.add(new AltarCraftingRecipe.IngredientEntry(e.getKey(), e.getValue()));

            ItemStack output = recipe.getResultItem(Minecraft.getInstance().level.registryAccess());
            if (!output.isEmpty())
                wrappers.add(new AltarCraftingRecipe(entries, output));
        }

        registration.addRecipes(AltarCraftingCategory.TYPE, wrappers);
        registration.addRecipes(SoulFirePurificationCategory.TYPE, buildSoulFireRecipes(manager));
    }

    /**
     * The items one output group can really produce: all candidates when the group picks one at
     * random, otherwise only the first one (mirrors {@code SoulFireOutput#rollStack}).
     */
    private static List<Item> displayCandidates(SoulFireOutput output) {
        List<Item> items = output.items();
        if (items.isEmpty()) {
            return List.of();
        }
        return !output.random() || items.size() == 1 ? List.of(items.get(0)) : items;
    }

    /**
     * Turns every loaded {@code eej:soul_fire_purification} recipe into its JEI view model, so the
     * category is populated purely from the datapack (including recipes shipped by other mods).
     */
    private static List<SoulFirePurificationJeiRecipe> buildSoulFireRecipes(RecipeManager manager) {
        List<SoulFirePurificationJeiRecipe> wrappers = new ArrayList<>();
        try {
            List<SoulFirePurificationRecipe> loaded =
                    manager.getAllRecipesFor(EejRecipes.SOUL_FIRE_PURIFICATION_TYPE.get());
            for (SoulFirePurificationRecipe recipe : loaded) {
                ItemStack[] inputs = recipe.ingredient().getItems();
                if (inputs.length == 0) continue;
                ItemStack input = inputs[0].copy();
                input.setCount(1);

                List<SoulFirePurificationJeiRecipe.Entry> entries = new ArrayList<>();
                if (recipe.usesWeightedAlternative()) {
                    // Weighted groups share ONE roll ("30 % chance to yield one random original
                    // block"), so they belong in a single JEI slot whose candidates cycle.
                    List<ItemStack> stacks = new ArrayList<>();
                    float chance = 0.0F;
                    int min = Integer.MAX_VALUE;
                    int max = 1;
                    for (SoulFireOutput output : recipe.outputs()) {
                        for (Item candidate : displayCandidates(output)) {
                            ItemStack display = new ItemStack(candidate);
                            display.setCount(Math.max(1, output.countMin()));
                            stacks.add(display);
                        }
                        chance = Math.max(chance, output.chance());
                        min = Math.min(min, Math.max(1, output.countMin()));
                        max = Math.max(max, Math.max(1, output.countMax()));
                    }
                    if (!stacks.isEmpty()) {
                        entries.add(new SoulFirePurificationJeiRecipe.Entry(stacks, chance, min, max));
                    }
                } else {
                    // Independent rolls: one slot per group, each with its own probability.
                    for (SoulFireOutput output : recipe.outputs()) {
                        List<ItemStack> stacks = new ArrayList<>();
                        for (Item candidate : displayCandidates(output)) {
                            ItemStack display = new ItemStack(candidate);
                            display.setCount(Math.max(1, output.countMin()));
                            stacks.add(display);
                        }
                        if (stacks.isEmpty()) continue;
                        entries.add(new SoulFirePurificationJeiRecipe.Entry(
                                stacks, output.chance(), output.countMin(), output.countMax()));
                    }
                }

                // Recipes that can never produce an item - the explosive coal entries and the
                // "destroyed" ones - are left out of the JEI list entirely: a recipe browser should only
                // offer transformations that yield something.
                if (entries.isEmpty()) continue;

                float explosionPower = 0.0F;
                boolean explosionFire = false;
                int blindnessTicks = 0;
                if (recipe.explosion().isPresent()) {
                    SoulFirePurificationRecipe.ExplosionSpec spec = recipe.explosion().get();
                    explosionPower = spec.power();
                    explosionFire = spec.fire();
                    blindnessTicks = spec.blindnessTicks();
                }

                wrappers.add(new SoulFirePurificationJeiRecipe(input, entries, recipe.burnChance(),
                        recipe.isExplosive(), recipe.isDestroyOnly(), explosionPower, explosionFire,
                        blindnessTicks));
            }
        } catch (RuntimeException exception) {
            // A broken recipe must never take the whole JEI plugin down.
            eej.LOGGER.warn("Failed to build soul fire purification JEI entries", exception);
        }
        return wrappers;
    }
}
