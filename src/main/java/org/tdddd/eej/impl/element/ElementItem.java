package org.tdddd.eej.impl.element;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * The base "Element" item ({@code eej:element}).
 *
 * <p>The item itself is deliberately textureless and meaningless without data: the element id in the data
 * component {@code eej:element} decides both the display name and the sprite. The sprite is resolved by
 * convention to {@code <ns>:item/element/<path>} and stitched from the block atlas, so a namespace that wants
 * its own element textures must contribute the matching directory source in its own
 * {@code assets/<ns>/atlases/blocks.json}. Without an id (or without a texture) the stack renders as nothing,
 * never as the missing-texture checkerboard.
 *
 * <p>Element items are strictly non-droppable and are swept out of non-creative player inventories; see
 * {@link ElementEvents}.
 */
public class ElementItem extends Item {
    public ElementItem(Properties properties) {
        super(properties);
    }

    /**
     * Name resolution for 26.1.2: {@code ItemStack#getHoverName} falls back to {@code ItemStack#getItemName},
     * which calls this method. The {@code ITEM_NAME} component is only the default
     * {@code item.eej.element} translation, so the element name has to be produced here; an explicit
     * {@code CUSTOM_NAME} still wins, because {@code getHoverName} checks it before ever calling this.
     */
    @Override
    public Component getName(ItemStack stack) {
        Identifier id = ElementStack.id(stack);
        if (id == null) {
            return super.getName(stack);
        }
        return Component.translatableWithFallback("element." + id.getNamespace() + "." + id.getPath(), id.toString());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier id = ElementStack.id(stack);
        if (id != null) {
            tooltip.accept(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
