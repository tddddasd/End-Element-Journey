package org.tdddd.eej.impl.element;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The base item of every element.
 *
 * <p>A single registered item ({@code eej:element}) represents all elements; which element a stack
 * is comes from its {@code eej:element} NBT string tag (see {@link ElementStack}). The stack name
 * is translated with {@code element.<namespace>.<path>} and falls back to the raw id, so an
 * element shipped by a data pack still shows a readable name when no translation is installed.</p>
 *
 * <p>The item has no texture of its own: its model uses the {@code eej:element} geometry loader,
 * which resolves the sprite {@code <namespace>:item/element/<path>} per stack and renders nothing
 * when there is no id or no matching texture.</p>
 */
public class ElementItem extends Item {
    public ElementItem(Properties properties) {
        super(properties);
    }

    /**
     * @param id the element id
     * @return the translation key of {@code id}, i.e. {@code element.<namespace>.<path>}
     */
    public static String translationKey(ResourceLocation id) {
        return "element." + id.getNamespace() + "." + id.getPath();
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation id = ElementStack.id(stack);
        if (id == null) {
            return super.getName(stack);
        }
        return Component.translatableWithFallback(translationKey(id), id.toString());
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        ResourceLocation id = ElementStack.id(stack);
        return id == null ? super.getDescriptionId(stack) : translationKey(id);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        ResourceLocation id = ElementStack.id(stack);
        if (id != null) {
            tooltip.add(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
