package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.function.Consumer;


public class EejRecipeData extends RecipeProvider {

    public EejRecipeData(PackOutput output) {
        super(output);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(eej.MODID, path);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        
        
        
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EejItems.PACKED_MUD_PEDESTAL.get())
                .define('P', Blocks.PACKED_MUD)
                .pattern("PPP")
                .pattern(" P ")
                .pattern("PPP")
                .unlockedBy("has_packed_mud", has(Blocks.PACKED_MUD))
                .save(consumer, id("packed_mud_pedestal_crafting_shaped"));

        
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EejItems.PACKED_MUD_ALTAR_STONE.get(), 4)
                .define('P', Blocks.PACKED_MUD)
                .pattern("PPP")
                .pattern("PPP")
                .pattern("PPP")
                .unlockedBy("has_packed_mud", has(Blocks.PACKED_MUD))
                .save(consumer, id("packed_mud_altar_stone_crafting_shaped"));

        
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EejItems.SMALL_ITEM_FRAME.get(), 4)
                .define('I', Items.STICK)
                .define('C', Items.LEATHER)
                .pattern(" I ")
                .pattern("ICI")
                .pattern(" I ")
                .unlockedBy("has_leather", has(Items.LEATHER))
                .save(consumer, id("small_item_frame_crafting_shaped_0"));

        
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EejItems.SMALL_ITEM_FRAME.get())
                .define('C', EejItems.SMALL_ITEM_FRAME.get())
                .pattern("C  ")
                .unlockedBy("has_small_item_frame", has(EejItems.SMALL_ITEM_FRAME.get()))
                .save(consumer, id("small_item_frame_crafting_shaped_1"));
    }
}
