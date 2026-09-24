package org.tdddd.eej.impl.soulfire;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Tick hooks for the soul fire mechanic.
 *
 * <p>Item entities are handled per entity tick, which is O(1) per item entity and avoids scanning levels for
 * item entities near fire. Player inventories are swept every 20 ticks (one pass over the slots) so that the
 * 30 s fire immunity cannot outlive its window while the item sits in a backpack.
 *
 * <p>Two hooks per item entity on purpose: {@link #onEntityTickPre} only applies the fire protection, which must
 * happen <b>before</b> the entity's own tick because {@code BaseFireBlock.entityInside} hurts it (2 damage in
 * soul fire against 5 health) while it ticks; {@link #onEntityTickPost} then runs the mechanic itself. The
 * consuming actions (rolling, detonating) deliberately stay in the post hook so a discarded entity never ticks.
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

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.tickCount % 20 != 0) {
            return;
        }
        SoulFirePurificationManager.expireInventory(level, player);
    }
}
