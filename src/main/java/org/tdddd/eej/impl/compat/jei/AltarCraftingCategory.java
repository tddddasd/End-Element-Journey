package org.tdddd.eej.impl.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejBlocks;

import java.util.List;


public class AltarCraftingCategory implements IRecipeCategory<AltarCraftingRecipe> {
    public static final RecipeType<AltarCraftingRecipe> TYPE =
            RecipeType.create(eej.MODID, "altar_crafting", AltarCraftingRecipe.class);

    
    private static final int INPUT_START_X = 10;
    private static final int INPUT_START_Y = 20;
    private static final int SLOT_SIZE = 18;
    
    private static final int MAX_PER_ROW = 6;
    
    private static final int OUTPUT_X = 130;
    private static final int OUTPUT_Y = 20;
    
    private static final int ARROW_X = 108;
    private static final int ARROW_Y = 22;
    
    private static final String HINT_KEY = "jei.eej.altar_crafting.hint";

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable arrow;
    
    private final IDrawable pedestalIcon;

    public AltarCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(160, 60);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(EejBlocks.PACKED_MUD_PEDESTAL.get()));
        this.arrow = guiHelper.getRecipeArrow();
        this.pedestalIcon = guiHelper.createDrawableItemStack(new ItemStack(EejBlocks.PACKED_MUD_PEDESTAL.get()));
    }

    @Override
    public IRecipeType<AltarCraftingRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.eej.altar_crafting");
    }

    @Override
    public int getWidth() {
        return 160;
    }

    @Override
    public int getHeight() {
        return 60;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    
    public IDrawable getBackground() {
        return background;
    }

    
    static int inputX(int index) {
        return INPUT_START_X + (index % MAX_PER_ROW) * SLOT_SIZE;
    }

    static int inputY(int index) {
        return INPUT_START_Y + (index / MAX_PER_ROW) * SLOT_SIZE;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AltarCraftingRecipe recipe, IFocusGroup focuses) {
        
        builder.setShapeless();

        List<AltarCraftingRecipe.IngredientEntry> inputs = recipe.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, inputX(i), inputY(i))
                    .addItemStacks(inputs.get(i).getDisplayStacks());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(AltarCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);

        
        int count = Math.min(recipe.getInputs().size(), MAX_PER_ROW * 2);
        for (int i = 0; i < count; i++) {
            pedestalIcon.draw(guiGraphics, inputX(i) + 3, inputY(i) - 12);
        }

        
        
        if (I18n.exists(HINT_KEY)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.font != null) {
                guiGraphics.text(minecraft.font, Component.translatable(HINT_KEY), 2, 4, 0xFF404040, false);
            }
        }
    }
}
