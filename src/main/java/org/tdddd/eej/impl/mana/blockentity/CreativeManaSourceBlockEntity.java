package org.tdddd.eej.impl.mana.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaStorage;
import org.tdddd.eej.impl.capability.EejCapabilities;
import org.tdddd.eej.impl.mana.CreativeManaStorage;
import org.tdddd.eej.impl.registry.EejBlockEntities;


/**
 * Creative mana source block entity: holds {@code Integer.MAX_VALUE / Integer.MAX_VALUE} mana and,
 * every tick, pushes mana into every mana-accepting block in the six adjacent positions
 * (north, south, east, west, up, down).
 *
 * <p>Port note (26.1.2): the capability is no longer fetched through
 * {@code BlockEntity#getCapability}; it is a NeoForge {@code BlockCapability} looked up on the
 * level, which needs the position and the block state as well (see
 * {@link EejCapabilities#MANA_STORAGE}).</p>
 */
public class CreativeManaSourceBlockEntity extends BlockEntity {

    private final ManaStorage manaStorage = new CreativeManaStorage();

    public CreativeManaSourceBlockEntity(BlockPos pos, BlockState state) {
        super(EejBlockEntities.CREATIVE_MANA_SOURCE.get(), pos, state);
    }

    
    public ManaStorage getManaStorage() {
        return this.manaStorage;
    }

    
    public static void tick(Level level, BlockPos pos, BlockState state, CreativeManaSourceBlockEntity source) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos targetPos = pos.relative(direction);
            
            Direction accessedFace = direction.getOpposite();
            IManaStorage storage = level.getCapability(EejCapabilities.MANA_STORAGE, targetPos, accessedFace);
            if (storage == null || !storage.canReceive(accessedFace) || storage.isFull()) {
                continue;
            }
            storage.receiveMana(Integer.MAX_VALUE, accessedFace, false);
        }
    }
}
