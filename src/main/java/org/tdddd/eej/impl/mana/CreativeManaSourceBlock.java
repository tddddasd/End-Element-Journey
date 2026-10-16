package org.tdddd.eej.impl.mana;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;
import org.tdddd.eej.impl.registry.EejBlockEntities;


/**
 * Creative mana source. Feeds mana into all six neighbours every tick; see
 * {@link CreativeManaSourceBlockEntity#tick}.
 */
public class CreativeManaSourceBlock extends BaseEntityBlock {

    public CreativeManaSourceBlock() {
        super(Properties.of()
                .strength(5.0F, 1200.0F)
                .sound(SoundType.AMETHYST)
                .mapColor(DyeColor.PINK)
                .lightLevel(state -> 10)
        );
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CreativeManaSourceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, EejBlockEntities.CREATIVE_MANA_SOURCE.get(),
                CreativeManaSourceBlockEntity::tick);
    }
}
