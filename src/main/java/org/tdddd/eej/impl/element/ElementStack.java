package org.tdddd.eej.impl.element;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejItems;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Read and write helpers for the element id that an element item stack carries.
 *
 * <p>This Minecraft version has no data components, so the id is stored as the NBT string tag
 * {@code eej:element} on the stack, e.g. {@code {"eej:element": "eej:fire"}}. The tag also keeps
 * two element stacks with different ids from stacking with each other, while the item itself
 * keeps the default max stack size of 64.</p>
 */
public final class ElementStack {
    /** NBT key of the element id string tag. */
    public static final String ELEMENT_TAG = "eej:element";

    /**
     * The element ids eej ships itself, in the tab order.
     *
     * <p>Creative tabs are built from the event parameters, and that provider does not always carry the
     * {@code eej:element} datapack registry yet (a tab can be built before the data pack registries are
     * reachable). When the registry cannot be read, the tab falls back to these ids so the shipped elements
     * are never missing; a data pack that adds more elements still gets them whenever the registry is
     * readable.</p>
     */
    private static final List<ResourceLocation> SHIPPED_IDS = List.of(
            new ResourceLocation(eej.MODID, "fire"),
            new ResourceLocation(eej.MODID, "water"),
            new ResourceLocation(eej.MODID, "wind"),
            new ResourceLocation(eej.MODID, "spirit"),
            new ResourceLocation(eej.MODID, "void"));

    private ElementStack() {
    }

    /** The element ids eej ships itself, used when the datapack registry is not readable. */
    public static List<ResourceLocation> shippedIds() {
        return SHIPPED_IDS;
    }

    /**
     * @param stack the stack to read
     * @return the element id stored on {@code stack}, or {@code null} when the stack is empty, has
     *         no element tag, or the tag does not hold a parsable resource location
     */
    @Nullable
    public static ResourceLocation id(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(ELEMENT_TAG, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(ELEMENT_TAG));
    }

    /**
     * @param id the element id, may be {@code null}
     * @return a new element item stack carrying {@code id}, or a plain element stack when
     *         {@code id} is {@code null}
     */
    public static ItemStack create(@Nullable ResourceLocation id) {
        return withId(new ItemStack(EejItems.ELEMENT.get()), id);
    }

    /**
     * Sets or removes the element id on a copy of {@code stack}.
     *
     * @param stack the stack to copy
     * @param id    the element id to set, or {@code null} to remove the tag
     * @return the modified copy; the original stack is left untouched
     */
    public static ItemStack withId(ItemStack stack, @Nullable ResourceLocation id) {
        ItemStack copy = stack.copy();
        if (id == null) {
            CompoundTag tag = copy.getTag();
            if (tag != null) {
                tag.remove(ELEMENT_TAG);
                if (tag.isEmpty()) {
                    copy.setTag(null);
                }
            }
        } else {
            copy.getOrCreateTag().putString(ELEMENT_TAG, id.toString());
        }
        return copy;
    }

    /**
     * @param stack the stack to test
     * @return {@code true} when {@code stack} is an element item carrying a parsable element id
     */
    public static boolean isElement(ItemStack stack) {
        return id(stack) != null;
    }
}
