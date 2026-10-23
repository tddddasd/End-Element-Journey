package org.tdddd.eej.impl.element.plate;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.tdddd.eej.impl.element.EejElements;
import org.tdddd.eej.impl.element.ElementStack;
import org.tdddd.eej.impl.element.plate.blockentity.ElementAggregationPlateBlockEntity;

import javax.annotation.Nullable;

/**
 * A flat plate that holds up to {@link ElementAggregationPlateBlockEntity#MAX_ELEMENTS} element items.
 *
 * <p>The plate is three pixels tall: the model shows a two pixel base plus a one pixel inset plate, so
 * the collision shape and the outline shape are the same {@code 16 x 3 x 16} box. Elements go in with
 * a right click and can never be taken out again.</p>
 */
public class ElementAggregationPlate extends BaseEntityBlock {

    /** Collision and outline box: the full footprint, three pixels high. */
    public static final VoxelShape SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 3.0D / 16.0D, 1.0D);

    public ElementAggregationPlate(Properties properties) {
        super(properties);
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ElementAggregationPlateBlockEntity plate)) {
            return InteractionResult.PASS;
        }
        // a bare hand only inspects the stored elements
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
        // empty hand: take whatever sits in the output slot, otherwise run one aggregation
        if (held.isEmpty()) {
            if (!plate.takeOutputSlot(player).isEmpty()) {
                return InteractionResult.CONSUME;
            }
        }
        // any element item is accepted; an item without an element id counts as the base element
        boolean isElement = !held.isEmpty()
                && held.getItem() instanceof org.tdddd.eej.impl.element.ElementItem;
        ResourceLocation elementId = isElement
                ? (ElementStack.id(held) != null ? ElementStack.id(held)
                        : new ResourceLocation("eej", "element"))
                : null;
        if (elementId == null) {
            // not holding an element: try to run one element aggregation instead
            // aggregation only ever produces an element, which stays in the output slot
            net.minecraft.world.item.ItemStack crafted = plate.craftAggregation(level);
            return crafted.isEmpty() ? InteractionResult.PASS : InteractionResult.CONSUME;
        }
        return plate.tryInsert(serverPlayer, elementId)
                ? InteractionResult.CONSUME
                : InteractionResult.PASS;
    }
}
