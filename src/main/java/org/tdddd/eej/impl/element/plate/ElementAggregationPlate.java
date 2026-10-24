package org.tdddd.eej.impl.element.plate;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.impl.element.ElementItem;
import org.tdddd.eej.impl.element.ElementStack;
import org.tdddd.eej.impl.element.plate.blockentity.ElementAggregationPlateBlockEntity;

/**
 * A flat plate that holds up to {@link ElementAggregationPlateBlockEntity#MAX_ELEMENTS} element items.
 *
 * <p>Three pixels tall: the model is a two pixel base plus a one pixel inset plate, so collision and
 * outline both use the same {@code 16 x 3 x 16} box. Elements go in with a right click and can never
 * be taken out again; a bare right click takes the output slot, or runs one element aggregation.</p>
 */
public class ElementAggregationPlate extends BaseEntityBlock {

    public static final MapCodec<ElementAggregationPlate> CODEC = simpleCodec(ElementAggregationPlate::new);

    /** Collision and outline box: the full footprint, three pixels high. */
    public static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 3.0D / 16.0D, 1.0D);

    public ElementAggregationPlate() {
        this(Properties.of());
    }

    public ElementAggregationPlate(Properties properties) {
        super(properties
                .strength(1.5F, 6.0F)
                .sound(SoundType.STONE)
                .noOcclusion()
        );
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElementAggregationPlateBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ElementAggregationPlateBlockEntity plate)) {
            return InteractionResult.PASS;
        }
        // bare hand: take the output slot, otherwise try one aggregation
        if (!plate.takeOutputSlot(player).isEmpty()) {
            return InteractionResult.CONSUME;
        }
        ItemStack crafted = plate.craftAggregation(level);
        return crafted.isEmpty() ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ElementAggregationPlateBlockEntity plate)) {
            return InteractionResult.PASS;
        }
        if (held.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // any element item is accepted; an item without an element id counts as the base element
        if (!(held.getItem() instanceof ElementItem)) {
            ItemStack crafted = plate.craftAggregation(level);
            return crafted.isEmpty() ? InteractionResult.PASS : InteractionResult.CONSUME;
        }
        Identifier id = ElementStack.id(held);
        Identifier elementId = id != null ? id : Identifier.fromNamespaceAndPath("eej", "element");
        return plate.tryInsert(player, elementId)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }
}
