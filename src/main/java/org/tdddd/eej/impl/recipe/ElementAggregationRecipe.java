package org.tdddd.eej.impl.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.tdddd.eej.impl.registry.EejRecipes;

import java.util.ArrayList;
import java.util.List;

/**
 * Element aggregation: a multiset of element ids inside an element aggregation plate turns into one
 * result item.
 *
 * <p>This is only the framework: no datapack files ship with it yet, so no recipe matches in game.
 * Add {@code data/<namespace>/element_aggregation/<name>.json} files shaped like
 * {@code {"elements": ["eej:fire", "eej:fire"], "result": {"item": "minecraft:torch"}}}.</p>
 */
public class ElementAggregationRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final List<ResourceLocation> elements;
    /** The element produced: aggregation only ever yields element items. */
    private final ResourceLocation resultElement;

    public ElementAggregationRecipe(ResourceLocation id, List<ResourceLocation> elements,
                                    ResourceLocation resultElement) {
        this.id = id;
        this.elements = List.copyOf(elements);
        this.resultElement = resultElement;
    }

    public ResourceLocation resultElement() {
        return this.resultElement;
    }

    public List<ResourceLocation> elements() {
        return this.elements;
    }

    /** Order independent match: the plate must hold exactly the multiset the recipe asks for. */
    public boolean matchesElements(List<ResourceLocation> present) {
        if (present.size() != this.elements.size()) {
            return false;
        }
        List<ResourceLocation> remaining = new ArrayList<>(present);
        for (ResourceLocation required : this.elements) {
            if (!remaining.remove(required)) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false; // matched through matchesElements instead of a container
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return org.tdddd.eej.impl.element.ElementStack.create(this.resultElement);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return org.tdddd.eej.impl.element.ElementStack.create(this.resultElement);
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EejRecipes.ELEMENT_AGGREGATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return EejRecipes.ELEMENT_AGGREGATION.get();
    }

    /** Serializer shared by the datapack loader and the network copy. */
    public static class Serializer implements RecipeSerializer<ElementAggregationRecipe> {

        @Override
        public ElementAggregationRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<ResourceLocation> elements = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("elements");
            for (int i = 0; i < array.size(); i++) {
                elements.add(new ResourceLocation(array.get(i).getAsString()));
            }
            ResourceLocation result = new ResourceLocation(json.get("result").getAsString());
            return new ElementAggregationRecipe(id, elements, result);
        }

        @Override
        public ElementAggregationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<ResourceLocation> elements = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                elements.add(buffer.readResourceLocation());
            }
            return new ElementAggregationRecipe(id, elements, buffer.readResourceLocation());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ElementAggregationRecipe recipe) {
            buffer.writeVarInt(recipe.elements.size());
            for (ResourceLocation element : recipe.elements) {
                buffer.writeResourceLocation(element);
            }
            buffer.writeResourceLocation(recipe.resultElement);
        }
    }
}
