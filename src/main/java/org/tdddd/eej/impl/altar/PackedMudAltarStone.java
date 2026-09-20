package org.tdddd.eej.impl.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;


public class PackedMudAltarStone extends AbstractAltarBlock {
    public static final int ALTAR_POINTS = 20;
    public static final int MAX_COUNT_IN_STRUCTURE = Integer.MAX_VALUE;

    public PackedMudAltarStone() {
        this(Properties.of()
                .strength(1.0f, 1.0f)
                .sound(SoundType.PACKED_MUD)
                .mapColor(DyeColor.ORANGE)
                .requiresCorrectToolForDrops());
    }

    
    public PackedMudAltarStone(Properties properties) {
        super(ALTAR_POINTS, MAX_COUNT_IN_STRUCTURE, properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
    }

    public static final com.mojang.serialization.MapCodec<PackedMudAltarStone> CODEC =
            simpleCodec(PackedMudAltarStone::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
}
