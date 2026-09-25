package org.tdddd.eej.impl.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
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


/**
 * JEI category for the altar crafting recipes.
 *
 * <p>Every ingredient is drawn as an item standing <b>on</b> a pedestal: the pedestal icon goes below the slot,
 * with a gap wide enough that the second ingredient row (and its own pedestals) fits underneath.</p>
 */
public class AltarCraftingCategory implements IRecipeCategory<AltarCraftingRecipe> {
    public static final IRecipeType<AltarCraftingRecipe> TYPE =
            IRecipeType.create(eej.MODID, "altar_crafting", AltarCraftingRecipe.class);

    private static final int WIDTH = 176;
    private static final int HEIGHT = 88;

    private static final int INPUT_START_X = 10;
    private static final int INPUT_START_Y = 14;
    private static final int SLOT_SIZE = 18;
    private static final int ROW_HEIGHT = 36;
    private static final int MAX_PER_ROW = 6;

    /**
     * Offset of a pedestal icon relative to the ingredient slot it belongs to.
     *
     * <p>JEI draws the 16 px item inside its 18 px slot with a one pixel inset, so the icon uses the same
     * offset: the item then sits <b>exactly above</b> its pedestal, in the same column, with the icon starting
     * right below the slot box.</p>
     */
    private static final int PEDESTAL_OFFSET_X = 1;
    private static final int PEDESTAL_OFFSET_Y = SLOT_SIZE;

    private static final int ARROW_X = 124;
    private static final int ARROW_Y = INPUT_START_Y + 1;
    private static final int OUTPUT_X = 150;
    private static final int OUTPUT_Y = INPUT_START_Y;

    private static final String HINT_KEY = "jei.eej.altar_crafting.hint";

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable pedestalIcon;

    public AltarCraftingCategory(IGuiHelper guiHelper) {
        // 26.1.2 sizes a category through getWidth()/getHeight() and has no background drawable hook, so the
        // page is simply the slot/pedestal layout below.
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
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /** Left edge of the ingredient slot at the given index. */
    static int inputX(int index) {
        return INPUT_START_X + (index % MAX_PER_ROW) * SLOT_SIZE;
    }

    /** Top edge of the ingredient slot at the given index. */
    static int inputY(int index) {
        return INPUT_START_Y + (index / MAX_PER_ROW) * ROW_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AltarCraftingRecipe recipe, IFocusGroup focuses) {
        // The altar accepts the materials in any order, so the slots are not placed in a shape.
        builder.setShapeless();

        List<AltarCraftingRecipe.IngredientEntry> inputs = recipe.getInputs();
        for (int index = 0; index < inputs.size(); index++) {
            builder.addSlot(RecipeIngredientRole.INPUT, inputX(index), inputY(index))
                    .addItemStacks(inputs.get(index).getDisplayStacks());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y)
                .addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(AltarCraftingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, ARROW_X, ARROW_Y);

        // The pedestal belongs *under* the item it carries: the icon sits below its slot, never above it.
        int count = Math.min(recipe.getInputs().size(), MAX_PER_ROW * 2);
        for (int index = 0; index < count; index++) {
            pedestalIcon.draw(guiGraphics, inputX(index) + PEDESTAL_OFFSET_X,
                    inputY(index) + PEDESTAL_OFFSET_Y);
        }

        if (I18n.exists(HINT_KEY)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.font != null) {
                guiGraphics.text(minecraft.font, Component.translatable(HINT_KEY), 2, 3, 0xFF404040, false);
            }
        }
    }
}
