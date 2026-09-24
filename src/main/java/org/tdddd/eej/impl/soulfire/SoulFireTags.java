package org.tdddd.eej.impl.soulfire;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.tdddd.eej.impl.eej;

/**
 * Tags that drive the soul fire purification mechanic without any hard dependency on the mods that use it.
 *
 * <p>{@link #SOUL_FIRE_CONSUMED} lists items that soul fire simply destroys: they get no purification roll at
 * all (not even a failed one). The tag is intentionally declared only by data packs/mods, so an absent tag is a
 * valid, empty state and {@code holder.is(tag)} stays false.
 */
public final class SoulFireTags {
    /** Items destroyed outright by soul fire, with no purification roll. */
    public static final TagKey<Item> SOUL_FIRE_CONSUMED =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(eej.MODID, "soul_fire_consumed"));

    private SoulFireTags() {
    }
}
