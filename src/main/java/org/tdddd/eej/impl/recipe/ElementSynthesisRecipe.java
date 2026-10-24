package org.tdddd.eej.impl.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.tdddd.eej.impl.element.ElementStack;
import org.tdddd.eej.impl.registry.EejRecipeTypes;

import java.util.ArrayList;
import java.util.List;

/**
 * Element synthesis: a main altar consumes the elements in the plate on top of it plus
 * mana from that plate and produces one result.
 *
 * <p>Framework only: no data files ship with it yet. A data file looks like
 * {@code {"elements": ["eej:water"], "result": "eej:ice"}}.</p>
 */
public class ElementSynthesisRecipe implements Recipe<SingleRecipeInput> {

    public static final MapCodec<ElementSynthesisRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            Identifier.CODEC.listOf().fieldOf("elements").forGetter(recipe -> recipe.elements),
            Codec.INT.optionalFieldOf("mana", 0).forGetter(recipe -> recipe.mana),
            ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
    ).apply(instance, ElementSynthesisRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElementSynthesisRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.elements,
                    ByteBufCodecs.VAR_INT, recipe -> recipe.mana,
                    ItemStack.STREAM_CODEC, recipe -> recipe.result,
                    ElementSynthesisRecipe::new);

    private final Recipe.CommonInfo commonInfo;
    private final List<Identifier> elements;
    private final int mana;
    private final ItemStack result;

    public ElementSynthesisRecipe(Recipe.CommonInfo commonInfo, List<Identifier> elements, int mana,
                                  ItemStack result) {
        this.commonInfo = commonInfo;
        this.elements = List.copyOf(elements);
        this.mana = mana;
        this.result = result;
    }

    public List<Identifier> elements() {
        return this.elements;
    }

    public int mana() {
        return this.mana;
    }

    public ItemStack result() {
        return this.result;
    }

    /** Order independent match: the plate must hold exactly the multiset the recipe asks for. */
    public boolean matchesElements(List<Identifier> present) {
        if (present.size() != this.elements.size()) {
            return false;
        }
        List<Identifier> remaining = new ArrayList<>(present);
        for (Identifier required : this.elements) {
            if (!remaining.remove(required)) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    public Recipe.CommonInfo commonInfo() {
        return this.commonInfo;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return false; // matched through matchesElements instead of a container
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input) {
        return this.result.copy();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<SingleRecipeInput>> getSerializer() {
        return EejRecipeTypes.ELEMENT_SYNTHESIS_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return EejRecipeTypes.ELEMENT_SYNTHESIS.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return null;
    }
}
