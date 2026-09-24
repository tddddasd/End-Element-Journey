package org.tdddd.eej.impl.soulfire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Fire immunity for freshly purified drops, valid for 30 seconds.
 *
 * <p>The marker is stored in the <b>item entity's</b> persistent data, never on the stack. That is what
 * keeps a purified drop an ordinary item: any stack tag or data component would change
 * {@code ItemStack#isSameItemSameComponents}, so the drop would refuse to merge with the player's existing
 * items (and with other purified drops) and the player would end up with several non-stacking slots. Because
 * the stack stays plain, picking the drop up ends the immunity and leaves a perfectly normal, stackable item.
 *
 * <p>The immunity itself is enforced by {@code SoulFirePurificationManager}, which makes every protected item
 * entity invulnerable while it sits in fire - that is stronger than clearing the fire ticks, because vanilla
 * fire also hurts the entity directly through {@code BaseFireBlock.entityInside}.
 */
public final class SoulFireImmunity {
    /** Immunity duration in ticks (30 seconds). */
    public static final int DURATION_TICKS = 20 * 30;

    /** Persistent-data key holding the game time at which the immunity ends. */
    private static final String EXPIRE_KEY = "eej:soul_fire_immune_until";

    private SoulFireImmunity() {
    }

    /** Starts the 30 s countdown on a freshly spawned drop. */
    public static void mark(ServerLevel level, ItemEntity entity) {
        entity.getPersistentData().putLong(EXPIRE_KEY, level.getGameTime() + DURATION_TICKS);
        entity.clearFire();
    }

    /** @return true while the entity carries a marker, whether or not its window has already passed */
    public static boolean isMarked(ItemEntity entity) {
        return entity.getPersistentData().getLongOr(EXPIRE_KEY, 0L) > 0L;
    }

    /** @return true when the entity carries the marker and its 30 s window has already passed */
    public static boolean isExpired(ServerLevel level, ItemEntity entity) {
        long until = entity.getPersistentData().getLongOr(EXPIRE_KEY, 0L);
        return until > 0L && level.getGameTime() >= until;
    }

    /** Removes the marker. */
    public static void clear(ItemEntity entity) {
        entity.getPersistentData().remove(EXPIRE_KEY);
    }
}
