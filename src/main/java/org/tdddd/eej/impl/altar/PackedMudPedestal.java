package org.tdddd.eej.impl.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;


public class PackedMudPedestal extends AbstractAltarBlock {
    public static final int ALTAR_POINTS = 30;
    public static final int MAX_COUNT_IN_STRUCTURE = 21;

    public PackedMudPedestal() {
        super(ALTAR_POINTS, MAX_COUNT_IN_STRUCTURE, Properties.of()
                .noOcclusion()
                .strength(1.0f, 3.0f)
                .sound(SoundType.PACKED_MUD)
                .isViewBlocking((state, world, pos) -> false)
                .isSuffocating((state, world, pos) -> false)
                .pushReaction(PushReaction.DESTROY)
                .requiresCorrectToolForDrops()
                .mapColor(DyeColor.ORANGE)
                .randomTicks()
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return PEDESTAL_SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PackedMudPedestalBlockEntity(pos, state);
    }
}
