package org.tdddd.eej.impl.soulfire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.tdddd.eej.impl.registry.EejRecipeTypes;

/**
 * A data driven soul fire purification recipe, serialized as {@code eej:soul_fire_purification}.
 *
 * <p>The recipe is a pure data rule: it is never crafted and never shown in the vanilla recipe book. It is
 * loaded by the vanilla recipe manager like any other recipe, which is what lets the mechanic stay completely
 * data driven (the mod that owns the input items just ships JSON files).
 *
 * <p>JSON shape:
 * <pre>
 * {
 *   "type": "eej:soul_fire_purification",
 *   "ingredient": { "item": "examplemod:infested_ore" },
 *   "results": [ { "chance": 0.15, "item": "minecraft:raw_iron", "count": 2, "count_max": 3 } ],
 *   "burn_chance": 0.85,
 *   "explode": { "power": 4.0, "radius": 8.0, "damage": 14.0, "knockback": 0.5, "blindness_ticks": 100 }
 * }
 * </pre>
 * {@code results} and {@code explode} are independent: an explosive recipe normally has no results at all
 * (the item detonates instead of purifying), while a purifying recipe has no {@code explode} block.
 */
public class SoulFirePurificationRecipe implements Recipe<SingleRecipeInput> {
    public static final MapCodec<SoulFirePurificationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(recipe -> recipe.ingredient),
            SoulFireOutput.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(recipe -> recipe.results),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("burn_chance", 0.0F).forGetter(recipe -> recipe.burnChance),
            SoulFireExplosion.CODEC.optionalFieldOf("explode").forGetter(recipe -> recipe.explosion)
    ).apply(instance, SoulFirePurificationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoulFirePurificationRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, recipe -> recipe.ingredient,
            SoulFireOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.results,
            ByteBufCodecs.FLOAT, recipe -> recipe.burnChance,
            ByteBufCodecs.optional(SoulFireExplosion.STREAM_CODEC), recipe -> recipe.explosion,
            SoulFirePurificationRecipe::new);

    private final Recipe.CommonInfo commonInfo;
    private final Ingredient ingredient;
    private final List<SoulFireOutput> results;
    private final float burnChance;
    private final Optional<SoulFireExplosion> explosion;
    private PlacementInfo placementInfo;

    public SoulFirePurificationRecipe(Recipe.CommonInfo commonInfo, Ingredient ingredient,
                                      List<SoulFireOutput> results, float burnChance,
                                      Optional<SoulFireExplosion> explosion) {
        this.commonInfo = commonInfo;
        this.ingredient = ingredient;
        this.results = List.copyOf(results);
        this.burnChance = burnChance;
        this.explosion = explosion;
    }

    public Recipe.CommonInfo commonInfo() {
        return commonInfo;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public List<SoulFireOutput> results() {
        return results;
    }

    /** Informational: the chance that nothing at all is produced ("burned away"). */
    public float burnChance() {
        return burnChance;
    }

    public Optional<SoulFireExplosion> explosion() {
        return explosion;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        if (results.isEmpty()) {
            return ItemStack.EMPTY;
        }
        SoulFireOutput output = results.get(0);
        List<SoulFireFixedDrop> drops = output.drops();
        return drops.isEmpty() ? ItemStack.EMPTY : new ItemStack(drops.get(0).item(), drops.get(0).count());
    }

    @Override
    public boolean showNotification() {
        return commonInfo.showNotification();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return EejRecipeTypes.SOUL_FIRE_PURIFICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return EejRecipeTypes.SOUL_FIRE_PURIFICATION.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(ingredient);
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        // The recipe is never shown in the recipe book (display() is empty); a vanilla category is enough to
        // satisfy the interface without registering another registry entry.
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
