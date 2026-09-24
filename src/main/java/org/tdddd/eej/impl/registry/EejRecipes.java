package org.tdddd.eej.impl.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.recipe.SoulFirePurificationRecipe;


/** Recipe types and serializers owned by eej. */
public final class EejRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, eej.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, eej.MODID);

    /** Datapack recipe type {@code eej:soul_fire_purification}. */
    public static final RegistryObject<RecipeType<SoulFirePurificationRecipe>> SOUL_FIRE_PURIFICATION_TYPE =
            RECIPE_TYPES.register("soul_fire_purification", () -> RecipeType.simple(
                    eej.asResource("soul_fire_purification")));

    public static final RegistryObject<RecipeSerializer<SoulFirePurificationRecipe>> SOUL_FIRE_PURIFICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("soul_fire_purification", SoulFirePurificationRecipe.Serializer::new);

    private EejRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
