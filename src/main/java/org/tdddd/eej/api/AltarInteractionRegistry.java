package org.tdddd.eej.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public final class AltarInteractionRegistry {
    private static final List<AltarInteractionHandler> HANDLERS = new CopyOnWriteArrayList<>();

    private AltarInteractionRegistry() {
    }

    public static void register(AltarInteractionHandler handler) {
        if (handler != null && !HANDLERS.contains(handler)) {
            HANDLERS.add(handler);
        }
    }

    public static void unregister(AltarInteractionHandler handler) {
        HANDLERS.remove(handler);
    }

    public static List<AltarInteractionHandler> getHandlers() {
        return List.copyOf(HANDLERS);
    }

    
    public static InteractionResult dispatch(Level level, BlockPos pos, BlockState state,
                                             Player player, InteractionHand hand, ItemStack heldItem) {
        for (AltarInteractionHandler handler : HANDLERS) {
            InteractionResult result = handler.onAltarUse(level, pos, state, player, hand, heldItem);
            if (result != null && result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.PASS;
    }
}
