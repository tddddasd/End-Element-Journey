package org.tdddd.eej.impl.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
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


/**
 * JEI plugin of eej: the altar crafting and soul fire purification categories.
 *
 * <h2>Why recipes are published twice</h2>
 * 26.1.2 sends the recipe list in {@code RecipeContentPayload}, which NeoForge re-exposes as
 * {@code RecipesReceivedEvent}. JEI waits for exactly that event before it starts and calls
 * {@link #registerRecipes}, and the mod-bus delivers the event to every listener - <b>including
 * {@link EejClientRecipeCache}, whose cache this plugin reads</b> - in an order that is not specified. Whenever
 * JEI's own listener runs first, {@link EejClientRecipeCache#synced()} is still empty here and both categories
 * would silently stay empty. The plugin therefore remembers whether it handed over a non-empty list and
 * republishes through {@link IJeiRuntime#getRecipeManager()} once the recipe sync has arrived (see
 * {@link #onRecipesSynced()}), which covers either order and JEI restarts alike.
 */
@JeiPlugin
public class EejJeiPlugin implements IModPlugin {
    /** The runtime, available between {@code onRuntimeAvailable} and {@code onRuntimeUnavailable}. */
    private static IJeiRuntime runtime;
    /** True once this runtime received a non-empty recipe list for our categories. */
    private static boolean categoriesPublished;

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
        categoriesPublished = false;
        Collection<RecipeHolder<?>> recipes = availableRecipes();
        List<AltarCraftingRecipe> altar = buildSafely("altar crafting", () -> buildAltarCrafting(recipes));
        List<SoulFirePurificationJeiRecipe> soulFire =
                buildSafely("soul fire purification", () -> buildSoulFirePurification(recipes));

        if (!altar.isEmpty()) {
            registration.addRecipes(AltarCraftingCategory.TYPE, altar);
        }
        if (!soulFire.isEmpty()) {
            registration.addRecipes(SoulFirePurificationCategory.TYPE, soulFire);
        }
        categoriesPublished = !altar.isEmpty() || !soulFire.isEmpty();
        eej.LOGGER.info("[eej-jei] registered {} altar and {} soul fire recipes ({} synced recipes available)",
                altar.size(), soulFire.size(), recipes.size());
    }

    /**
     * Runs one category's build and swallows a failure, so a broken entry can never take the other category
     * (or the whole plugin) down with it. 26.1.2 vanilla ships crafting recipes that throw when assembled from
     * an empty grid ({@code ImbueRecipe} indexes the input directly and raises
     * {@code ArrayIndexOutOfBoundsException} on an empty 3x3 grid); an unguarded exception there aborted JEI's
     * entire recipe registration, which is exactly how both eej categories ended up empty.
     */
    private static <T> List<T> buildSafely(String what, java.util.function.Supplier<List<T>> builder) {
        try {
            return builder.get();
        } catch (RuntimeException exception) {
            eej.LOGGER.error("[eej-jei] failed to build the {} entries; that category stays empty", what,
                    exception);
            return List.of();
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        publishPendingRecipes();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        categoriesPublished = false;
    }

    /**
     * Called by {@link EejClientRecipeCache} after every recipe sync.
     *
     * <p>The call site is guarded by a JEI presence check there, because this class references JEI API types and
     * must not be loaded on a client without JEI.</p>
     */
    public static void onRecipesSynced() {
        publishPendingRecipes();
    }

    /**
     * Hands the categories to JEI through the runtime when the registration ran before the recipe sync reached
     * {@link EejClientRecipeCache}. Adding a second time would duplicate every entry (JEI appends), hence the
     * {@link #categoriesPublished} guard.
     */
    private static void publishPendingRecipes() {
        if (categoriesPublished || runtime == null) {
            return;
        }
        Collection<RecipeHolder<?>> recipes = availableRecipes();
        List<AltarCraftingRecipe> altar = buildAltarCrafting(recipes);
        List<SoulFirePurificationJeiRecipe> soulFire = buildSoulFirePurification(recipes);
        if (altar.isEmpty() && soulFire.isEmpty()) {
            return;
        }
        IRecipeManager manager = runtime.getRecipeManager();
        if (!altar.isEmpty()) {
            manager.addRecipes(AltarCraftingCategory.TYPE, altar);
        }
        if (!soulFire.isEmpty()) {
            manager.addRecipes(SoulFirePurificationCategory.TYPE, soulFire);
        }
        categoriesPublished = true;
        eej.LOGGER.info("[eej-jei] published {} altar and {} soul fire recipes after a late recipe sync",
                altar.size(), soulFire.size());
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

    private static List<AltarCraftingRecipe> buildAltarCrafting(Collection<RecipeHolder<?>> recipes) {
        List<AltarCraftingRecipe> wrappers = new ArrayList<>();
        for (RecipeHolder<?> holder : recipes) {
            if (!(holder.value() instanceof CraftingRecipe recipe)) continue;
            if (recipe.isSpecial()) continue;

            try {
                Map<Ingredient, Integer> counts = new LinkedHashMap<>();
                for (Ingredient ing : recipe.placementInfo().ingredients()) {
                    if (!ing.isEmpty())
                        counts.put(ing, counts.getOrDefault(ing, 0) + 1);
                }
                if (counts.isEmpty()) continue;

                List<AltarCraftingRecipe.IngredientEntry> entries = new ArrayList<>();
                for (Map.Entry<Ingredient, Integer> e : counts.entrySet())
                    entries.add(new AltarCraftingRecipe.IngredientEntry(e.getKey(), e.getValue()));

                // Not every crafting recipe survives an empty grid (see buildSafely), so a failure here only
                // skips this one recipe instead of the whole category.
                ItemStack output = recipe.assemble(emptyCraftingInput());
                if (!output.isEmpty())
                    wrappers.add(new AltarCraftingRecipe(entries, output));
            } catch (RuntimeException exception) {
                eej.LOGGER.debug("[eej-jei] skipping crafting recipe {} for the altar category: {}",
                        holder.id(), exception.toString());
            }
        }
        return wrappers;
    }

    private static List<SoulFirePurificationJeiRecipe> buildSoulFirePurification(
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
        return wrappers;
    }

    private static net.minecraft.world.item.crafting.CraftingInput emptyCraftingInput() {
        List<ItemStack> empty = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            empty.add(ItemStack.EMPTY);
        }
        return net.minecraft.world.item.crafting.CraftingInput.of(3, 3, empty);
    }
}
