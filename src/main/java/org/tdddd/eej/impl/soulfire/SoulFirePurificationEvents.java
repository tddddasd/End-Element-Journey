package org.tdddd.eej.impl.soulfire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Tick hooks for the soul fire mechanic.
 *
 * <p>Item entities are handled per entity tick, which is O(1) per item entity and avoids scanning levels for
 * item entities near fire. Two hooks per item entity on purpose: {@link #onEntityTickPre} only applies the fire
 * protection, which must happen <b>before</b> the entity's own tick because {@code BaseFireBlock.entityInside}
 * hurts it (2 damage in soul fire against 5 health) while it ticks; {@link #onEntityTickPost} then runs the
 * mechanic itself. The consuming actions (rolling, detonating) deliberately stay in the post hook so a
 * discarded entity never ticks.
 *
 * <p>No player-inventory hook is needed: the 30 s fire immunity lives on the item <em>entity</em>, so nothing
 * ever has to be cleaned off a stack in a backpack.
 */
public final class SoulFirePurificationEvents {
    private SoulFirePurificationEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        SoulFirePurificationManager.protectItemEntity(level, itemEntity);
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof ItemEntity itemEntity)) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        SoulFirePurificationManager.handleItemEntity(level, itemEntity);
    }
}
