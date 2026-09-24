package org.tdddd.eej.impl.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.tdddd.eej.impl.eej;

import java.util.List;


/**
 * JEI category for {@code eej:soul_fire_purification}.
 *
 * <p>The layout mirrors a machine recipe instead of a text page: the input stack, an arrow with the
 * soul fire block floating right above it, and the purified result, with the chance drawn in orange
 * under the result slot. A single result slot cycles through every candidate item of its roll, so a
 * "30 % chance for one random original block" rule stays one compact slot.
 *
 * <p>Only two 1.20.1 JEI calls used here are deprecated ({@code IRecipeCategory#getBackground} is not
 * overridden, {@code draw} is the legacy hook); the warning is suppressed because the current JEI
 * 1.20.1 API still exposes exactly these methods.
 */
@SuppressWarnings("removal")
public class SoulFirePurificationCategory implements IRecipeCategory<SoulFirePurificationJeiRecipe> {
    public static final RecipeType<SoulFirePurificationJeiRecipe> TYPE =
            RecipeType.create(eej.MODID, "soul_fire_purification", SoulFirePurificationJeiRecipe.class);

    /** Orange chance labels: the only text this category draws. */
    private static final int CHANCE_COLOR = 0xFFFF8C00;
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_STEP = 20;
    private static final int COLUMNS = 4;
    private static final int ROW_Y = 20;
    private static final int INPUT_X = 5;
    private static final int ICON_SIZE = 16;
    private static final int GAP = 6;

    /** Frame 0 of the animated vanilla soul fire texture, the block the item has to be thrown into. */
    private static final ResourceLocation SOUL_FIRE_TEXTURE =
            new ResourceLocation("minecraft", "textures/block/soul_fire_0.png");
    /** The file is an animation strip of 32 16x16 frames, so the frame region needs its real height. */
    private static final int SOUL_FIRE_TEXTURE_HEIGHT = 512;

    private final IDrawable icon;
    private final IDrawable arrow;
    private final IDrawable soulFire;

    private final int arrowX;
    private final int arrowY;
    private final int soulFireX;
    private final int soulFireY;
    private final int outputX;
    private final int width;
    private final int height;

    public SoulFirePurificationCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.SOUL_CAMPFIRE));
        this.arrow = guiHelper.getRecipeArrow();
        this.soulFire = guiHelper.drawableBuilder(SOUL_FIRE_TEXTURE, 0, 0, ICON_SIZE, ICON_SIZE)
                .setTextureSize(ICON_SIZE, SOUL_FIRE_TEXTURE_HEIGHT)
                .build();

        this.arrowX = INPUT_X + SLOT_SIZE + GAP;
        this.arrowY = ROW_Y + Math.max(0, (SLOT_SIZE - arrow.getHeight()) / 2);
        this.soulFireX = arrowX + Math.max(0, (arrow.getWidth() - ICON_SIZE) / 2);
        this.soulFireY = Math.max(1, arrowY - 3 - ICON_SIZE);
        this.outputX = arrowX + arrow.getWidth() + GAP + 1;
        this.width = outputX + COLUMNS * SLOT_STEP - (SLOT_STEP - SLOT_SIZE) + 4;
        this.height = ROW_Y + SLOT_SIZE + 12;
    }

    @Override
    public RecipeType<SoulFirePurificationJeiRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.eej.soul_fire_purification");
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    /** Left edge of the result slot at the given index. */
    private int slotX(int index) {
        return outputX + index * SLOT_STEP;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SoulFirePurificationJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, ROW_Y).addItemStack(recipe.getInput());

        List<SoulFirePurificationJeiRecipe.Entry> outputs = recipe.getOutputs();
        if (outputs.isEmpty()) {
            // Explosive / destroy-only rule: no roll, so the outcome is shown as a symbol only.
            boolean explosive = recipe.isExplosive();
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, outputX, ROW_Y)
                    .addItemStack(new ItemStack(explosive ? Items.TNT : Items.FIRE_CHARGE))
                    .addTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable(
                            explosive
                                    ? "jei.eej.soul_fire_purification.explodes"
                                    : "jei.eej.soul_fire_purification.destroyed")));
            return;
        }

        for (int index = 0; index < outputs.size() && index < COLUMNS; index++) {
            SoulFirePurificationJeiRecipe.Entry entry = outputs.get(index);
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, slotX(index), ROW_Y)
                    .addItemStacks(entry.getStacks());
            if (entry.getCountMax() > entry.getCountMin()) {
                slot.addTooltipCallback((view, tooltip) ->
                        tooltip.add(Component.literal(entry.getCountText())));
            }
        }
    }

    @Override
    public void draw(SoulFirePurificationJeiRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, arrowX, arrowY);
        soulFire.draw(guiGraphics, soulFireX, soulFireY);

        Font font = Minecraft.getInstance().font;
        List<SoulFirePurificationJeiRecipe.Entry> outputs = recipe.getOutputs();
        for (int index = 0; index < outputs.size() && index < COLUMNS; index++) {
            String percent = outputs.get(index).getPercentText();
            int x = slotX(index) + Math.max(0, (ICON_SIZE - font.width(percent)) / 2);
            guiGraphics.drawString(font, percent, x, ROW_Y + SLOT_SIZE + 1, CHANCE_COLOR, false);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, SoulFirePurificationJeiRecipe recipe,
                           IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        if (mouseX >= soulFireX && mouseX < soulFireX + ICON_SIZE
                && mouseY >= soulFireY && mouseY < soulFireY + ICON_SIZE) {
            tooltip.add(Component.translatable("block.minecraft.soul_fire"));
        }
    }
}
