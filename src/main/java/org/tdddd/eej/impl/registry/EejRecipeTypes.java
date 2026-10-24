package org.tdddd.eej.impl.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.recipe.ElementAggregationRecipe;
import org.tdddd.eej.impl.recipe.ElementSynthesisRecipe;
import org.tdddd.eej.impl.soulfire.SoulFirePurificationRecipe;

/** Registry entries owned by eej, including the data driven soul fire purification recipe type. */
public final class EejRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, eej.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, eej.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SoulFirePurificationRecipe>> SOUL_FIRE_PURIFICATION =
            RECIPE_TYPES.register("soul_fire_purification",
                    () -> RecipeType.simple(eej.asResource("soul_fire_purification")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SoulFirePurificationRecipe>>
            SOUL_FIRE_PURIFICATION_SERIALIZER = RECIPE_SERIALIZERS.register("soul_fire_purification",
                    () -> new RecipeSerializer<>(SoulFirePurificationRecipe.MAP_CODEC,
                            SoulFirePurificationRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElementAggregationRecipe>> ELEMENT_AGGREGATION =
            RECIPE_TYPES.register("element_aggregation",
                    () -> RecipeType.simple(eej.asResource("element_aggregation")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElementAggregationRecipe>>
            ELEMENT_AGGREGATION_SERIALIZER = RECIPE_SERIALIZERS.register("element_aggregation",
                    () -> new RecipeSerializer<>(ElementAggregationRecipe.MAP_CODEC,
                            ElementAggregationRecipe.STREAM_CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElementSynthesisRecipe>> ELEMENT_SYNTHESIS =
            RECIPE_TYPES.register("element_synthesis",
                    () -> RecipeType.simple(eej.asResource("element_synthesis")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElementSynthesisRecipe>>
            ELEMENT_SYNTHESIS_SERIALIZER = RECIPE_SERIALIZERS.register("element_synthesis",
                    () -> new RecipeSerializer<>(ElementSynthesisRecipe.MAP_CODEC,
                            ElementSynthesisRecipe.STREAM_CODEC));

    private EejRecipeTypes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
