package org.tdddd.eej.impl.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.recipe.ElementAggregationRecipe;
import org.tdddd.eej.impl.recipe.SoulFirePurificationRecipe;
import org.tdddd.eej.impl.recipe.ElementSynthesisRecipe;

/**
 * Recipe types and serializers for the two element recipes.
 *
 * <p>Both are registered but no data files ship with the mod yet, so nothing matches in game. Adding a
 * file under {@code data/<namespace>/element_aggregation/} or {@code .../element_synthesis/} is enough
 * to make a recipe live.</p>
 */
public class EejRecipes {

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, eej.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, eej.MODID);

    public static final RegistryObject<RecipeType<ElementAggregationRecipe>> ELEMENT_AGGREGATION =
            RECIPE_TYPES.register("element_aggregation", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return eej.MODID + ":element_aggregation";
                }
            });

    public static final RegistryObject<RecipeType<ElementSynthesisRecipe>> ELEMENT_SYNTHESIS =
            RECIPE_TYPES.register("element_synthesis", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return eej.MODID + ":element_synthesis";
                }
            });

    public static final RegistryObject<RecipeSerializer<ElementAggregationRecipe>> ELEMENT_AGGREGATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("element_aggregation", ElementAggregationRecipe.Serializer::new);

    public static final RegistryObject<RecipeSerializer<ElementSynthesisRecipe>> ELEMENT_SYNTHESIS_SERIALIZER =
            RECIPE_SERIALIZERS.register("element_synthesis", ElementSynthesisRecipe.Serializer::new);

    /** Soul fire purification, which already shipped before the element recipes. */
    public static final RegistryObject<RecipeType<SoulFirePurificationRecipe>> SOUL_FIRE_PURIFICATION_TYPE =
            RECIPE_TYPES.register("soul_fire_purification", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return eej.MODID + ":soul_fire_purification";
                }
            });

    public static final RegistryObject<RecipeSerializer<SoulFirePurificationRecipe>> SOUL_FIRE_PURIFICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("soul_fire_purification", SoulFirePurificationRecipe.Serializer::new);

    /** Registers both deferred registers with the mod event bus. */
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
