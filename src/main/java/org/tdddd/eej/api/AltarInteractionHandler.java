package org.tdddd.eej.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;


@FunctionalInterface
public interface AltarInteractionHandler {
    InteractionResult onAltarUse(Level level, BlockPos pos, BlockState state,
                                 Player player, InteractionHand hand, ItemStack heldItem);
}
