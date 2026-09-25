package org.tdddd.eej.impl.element;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import org.tdddd.eej.impl.registry.EejDataComponents;
import org.tdddd.eej.impl.registry.EejItems;

/**
 * Helpers for reading and writing the element id of an {@link net.minecraft.world.item.ItemStack}.
 *
 * <p>The id lives in the data component {@code eej:element} (a plain {@link Identifier}), so stacks carrying
 * different ids are never merged by the vanilla stacking rules.
 */
public final class ElementStack {
    /** Texture folder (relative to {@code textures/}) that holds every element sprite. */
    public static final String TEXTURE_DIRECTORY = "item/element";

    private ElementStack() {
    }

    /** The element id of {@code stack}, or {@code null} when it does not carry one. */
    public static @Nullable Identifier id(ItemStack stack) {
        return stack.get(EejDataComponents.ELEMENT_ID.get());
    }

    /** The element id of {@code stack}, or {@code null} when the stack is not an element item or has no id. */
    public static @Nullable Identifier idOf(ItemStack stack) {
        return stack.is(EejItems.ELEMENT.get()) ? id(stack) : null;
    }

    /** A single element item stack carrying {@code id} (or a plain element item when {@code id} is null). */
    public static ItemStack create(@Nullable Identifier id) {
        return create(id, 1);
    }

    /** A {@code count}-sized element item stack carrying {@code id} (or a plain element item when null). */
    public static ItemStack create(@Nullable Identifier id, int count) {
        ItemStack stack = new ItemStack(EejItems.ELEMENT.get(), count);
        return withId(stack, id);
    }

    /** The atlas sprite location an element id resolves to, i.e. {@code <ns>:item/element/<path>}. */
    public static Identifier textureId(Identifier id) {
        return Identifier.fromNamespaceAndPath(id.getNamespace(), TEXTURE_DIRECTORY + "/" + id.getPath());
    }

    /** Writes (or clears, with {@code null}) the element id of {@code stack} and returns the same stack. */
    public static ItemStack withId(ItemStack stack, @Nullable Identifier id) {
        if (id == null) {
            stack.remove(EejDataComponents.ELEMENT_ID.get());
        } else {
            stack.set(EejDataComponents.ELEMENT_ID.get(), id);
        }
        return stack;
    }

    /** {@code true} when {@code stack} is the element item, with or without an id. */
    public static boolean isElement(ItemStack stack) {
        return stack.is(EejItems.ELEMENT.get());
    }
}
