package org.tdddd.eej.impl.soulfire;

import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DamageResistant;

/**
 * Fire immunity for freshly purified drops, valid for 30 seconds.
 *
 * <p>Mechanism: vanilla already routes every "can this item entity burn?" question through
 * {@code ItemStack#canBeHurtBy(DamageSource)} ({@code ItemEntity#fireImmune} and
 * {@code ItemEntity#hurtServer} both use it), and that method honours the {@code minecraft:damage_resistant}
 * data component. So a purified stack simply carries
 * {@code minecraft:damage_resistant} keyed on {@code #minecraft:is_fire} (the same component netherite items
 * use) plus an expiry timestamp in {@code minecraft:custom_data}.
 *
 * <p>Because both live on the <em>stack</em>, the immunity survives being picked up and dropped again while the
 * window is running. Two lightweight tick hooks remove the marker once the timestamp passes: one for item
 * entities (every tick, O(1)) and one for player inventories (every 20 ticks, one pass over the slots), so the
 * immunity cannot linger forever on an item that has been picked up.
 */
public final class SoulFireImmunity {
    /** Immunity duration in ticks (30 seconds). */
    public static final int DURATION_TICKS = 20 * 30;

    /** NBT key holding the game time at which the immunity ends. */
    private static final String EXPIRE_KEY = "eej:soul_fire_immune_until";

    private SoulFireImmunity() {
    }

    /** Adds the fire immunity marker and starts the 30 s countdown. */
    public static void mark(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        HolderSet<DamageType> fireDamage = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypeTags.IS_FIRE);
        stack.set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(fireDamage));
        long until = level.getGameTime() + DURATION_TICKS;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putLong(EXPIRE_KEY, until));
    }

    /** @return true when the stack carries the marker and its 30 s window has already passed */
    public static boolean isExpired(ServerLevel level, ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        long until = data.copyTag().getLongOr(EXPIRE_KEY, 0L);
        return until > 0L && level.getGameTime() >= until;
    }

    /**
     * @return true while the stack carries the marker, whether or not its window has already passed. Used by
     *     the fire protection sweep, which must keep the drop alive for exactly as long as the marker lives.
     */
    public static boolean isMarked(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getLongOr(EXPIRE_KEY, 0L) > 0L;
    }

    /** Removes the marker. Mutates the given stack in place; pass a copy when it belongs to an entity. */
    public static void clear(ItemStack stack) {
        stack.remove(DataComponents.DAMAGE_RESISTANT);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(EXPIRE_KEY));
    }
}
