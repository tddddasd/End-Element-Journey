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
 * Element aggregation: a multiset of element ids inside an element aggregation plate turns
 * into one result element.
 *
 * <p>Framework only: no data files ship with it yet. A data file looks like
 * {@code {"elements": ["eej:fire", "eej:fire"], "result": "eej:spirit"}}.</p>
 */
public class ElementAggregationRecipe implements Recipe<SingleRecipeInput> {

    public static final MapCodec<ElementAggregationRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(recipe -> recipe.commonInfo),
            Identifier.CODEC.listOf().fieldOf("elements").forGetter(recipe -> recipe.elements),
            Identifier.CODEC.fieldOf("result").forGetter(recipe -> recipe.resultElement)
    ).apply(instance, ElementAggregationRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ElementAggregationRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    Recipe.CommonInfo.STREAM_CODEC, recipe -> recipe.commonInfo,
                    Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()), recipe -> recipe.elements,
                    Identifier.STREAM_CODEC, recipe -> recipe.resultElement,
                    ElementAggregationRecipe::new);

    private final Recipe.CommonInfo commonInfo;
    private final List<Identifier> elements;
    private final Identifier resultElement;

    public ElementAggregationRecipe(Recipe.CommonInfo commonInfo, List<Identifier> elements,
                     Identifier resultElement) {
        this.commonInfo = commonInfo;
        this.elements = List.copyOf(elements);
        this.resultElement = resultElement;
    }

    public List<Identifier> elements() {
        return this.elements;
    }

    public Identifier resultElement() {
        return this.resultElement;
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
        return ElementStack.create(this.resultElement);
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
        return EejRecipeTypes.ELEMENT_AGGREGATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<SingleRecipeInput>> getType() {
        return EejRecipeTypes.ELEMENT_AGGREGATION.get();
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
