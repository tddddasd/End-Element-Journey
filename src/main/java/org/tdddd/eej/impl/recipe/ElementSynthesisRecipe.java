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
 * Element synthesis: a main altar consumes the items on it plus mana from the plate underneath and
 * produces one result.
 *
 * <p>Framework only - no datapack files ship with it yet. A data file looks like
 * {@code {"elements": ["eej:water"], "mana": 4, "result": {"item": "minecraft:ice"}}}.</p>
 */
public class ElementSynthesisRecipe implements Recipe<Container> {

    private final ResourceLocation id;
    private final List<ResourceLocation> elements;
    private final int mana;
    private final ItemStack result;

    public ElementSynthesisRecipe(ResourceLocation id, List<ResourceLocation> elements, int mana,
                                  ItemStack result) {
        this.id = id;
        this.elements = List.copyOf(elements);
        this.mana = mana;
        this.result = result;
    }

    public List<ResourceLocation> elements() {
        return this.elements;
    }

    /** Mana drawn from the element aggregation plate below the altar. */
    public int mana() {
        return this.mana;
    }

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
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess access) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return this.result;
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EejRecipes.ELEMENT_SYNTHESIS_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return EejRecipes.ELEMENT_SYNTHESIS.get();
    }

    public static class Serializer implements RecipeSerializer<ElementSynthesisRecipe> {

        @Override
        public ElementSynthesisRecipe fromJson(ResourceLocation id, JsonObject json) {
            List<ResourceLocation> elements = new ArrayList<>();
            JsonArray array = json.getAsJsonArray("elements");
            for (int i = 0; i < array.size(); i++) {
                elements.add(new ResourceLocation(array.get(i).getAsString()));
            }
            int mana = json.has("mana") ? json.get("mana").getAsInt() : 0;
            ItemStack result = new ItemStack(net.minecraft.world.item.crafting.ShapedRecipe
                    .itemFromJson(json.getAsJsonObject("result")));
            return new ElementSynthesisRecipe(id, elements, mana, result);
        }

        @Override
        public ElementSynthesisRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            int count = buffer.readVarInt();
            List<ResourceLocation> elements = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                elements.add(buffer.readResourceLocation());
            }
            int mana = buffer.readVarInt();
            return new ElementSynthesisRecipe(id, elements, mana, buffer.readItem());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ElementSynthesisRecipe recipe) {
            buffer.writeVarInt(recipe.elements.size());
            for (ResourceLocation element : recipe.elements) {
                buffer.writeResourceLocation(element);
            }
            buffer.writeVarInt(recipe.mana);
            buffer.writeItem(recipe.result);
        }
    }
}
