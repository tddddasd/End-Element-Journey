package org.tdddd.eej.impl.datagen.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.concurrent.CompletableFuture;


public class EejRecipeData extends RecipeProvider {

    public EejRecipeData(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    
    public static class Runner extends RecipeProvider.Runner {
        public Runner(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
            super(output, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new EejRecipeData(registries, output);
        }

        @Override
        public String getName() {
            return "eej Recipes";
        }
    }

    private static ResourceKey<Recipe<?>> id(String path) {
        return ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE,
                Identifier.fromNamespaceAndPath(eej.MODID, path));
    }

    @Override
    protected void buildRecipes() {
        
        
        
        
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, EejItems.PACKED_MUD_PEDESTAL.get())
                .define('P', Blocks.PACKED_MUD)
                .pattern("PPP")
                .pattern(" P ")
                .pattern("PPP")
                .unlockedBy("has_packed_mud", has(Blocks.PACKED_MUD))
                .save(this.output, id("packed_mud_pedestal_crafting_shaped"));

        
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, EejItems.PACKED_MUD_ALTAR_STONE.get(), 4)
                .define('P', Blocks.PACKED_MUD)
                .pattern("PPP")
                .pattern("PPP")
                .pattern("PPP")
                .unlockedBy("has_packed_mud", has(Blocks.PACKED_MUD))
                .save(this.output, id("packed_mud_altar_stone_crafting_shaped"));

        
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, EejItems.SMALL_ITEM_FRAME.get(), 4)
                .define('I', Items.STICK)
                .define('C', Items.LEATHER)
                .pattern(" I ")
                .pattern("ICI")
                .pattern(" I ")
                .unlockedBy("has_leather", has(Items.LEATHER))
                .save(this.output, id("small_item_frame_crafting_shaped_0"));

        
        ShapedRecipeBuilder.shaped(this.items, RecipeCategory.TOOLS, EejItems.SMALL_ITEM_FRAME.get())
                .define('C', EejItems.SMALL_ITEM_FRAME.get())
                .pattern("C  ")
                .unlockedBy("has_small_item_frame", has(EejItems.SMALL_ITEM_FRAME.get()))
                .save(this.output, id("small_item_frame_crafting_shaped_1"));
    }
}
