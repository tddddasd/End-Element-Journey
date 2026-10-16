package org.tdddd.eej.impl.mana;

import com.mojang.serialization.MapCodec;
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
 *
 * <p>Port note (26.1.2): like every other block of this tree it needs an explicit {@code codec()},
 * and its {@code Properties} must be reachable through that codec's constructor shape
 * ({@code simpleCodec}). The ticker registration is unchanged in shape.</p>
 */
public class CreativeManaSourceBlock extends BaseEntityBlock {

    public static final MapCodec<CreativeManaSourceBlock> CODEC = simpleCodec(CreativeManaSourceBlock::new);

    public CreativeManaSourceBlock() {
        this(Properties.of()
                .strength(5.0F, 1200.0F)
                .sound(SoundType.AMETHYST)
                .mapColor(DyeColor.PINK)
                .lightLevel(state -> 10)
        );
    }

    
    public CreativeManaSourceBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
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
