package org.tdddd.eej.impl.altar.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import org.jspecify.annotations.Nullable;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.registry.EejItems;

import java.util.List;


public class PedestalItemRenderer implements BlockEntityRenderer<PackedMudPedestalBlockEntity, PedestalRenderState> {
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private final ItemModelResolver itemModelResolver;

    public PedestalItemRenderer(BlockEntityRendererProvider.Context context) {
        // 1.20.1: context.getItemRenderer()
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public PedestalRenderState createRenderState() {
        return new PedestalRenderState();
    }

    @Override
    public void extractRenderState(PackedMudPedestalBlockEntity blockEntity, PedestalRenderState state,
                                   float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        long gameTime = blockEntity.getLevel() != null
                ? blockEntity.getLevel().getGameTime()
                : (Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L);
        state.rotation = (gameTime + partialTicks) * 2.0f;

        state.hasItem = blockEntity.hasItem();
        if (state.hasItem) {
            ItemStack stack = blockEntity.getItem();
            this.itemModelResolver.updateForTopItem(state.item, stack, ItemDisplayContext.GROUND,
                    blockEntity.getLevel(), null, 0);
        } else {
            state.item.clear();
        }

        List<String> filterData = blockEntity.getFilterData();
        if (!filterData.isEmpty()) {
            ItemStack filterStack = new ItemStack(EejItems.SMALL_ITEM_FRAME.get(), 1);
            for (ItemStackRenderState side : state.filters) {
                this.itemModelResolver.updateForTopItem(side, filterStack, ItemDisplayContext.FIXED,
                        blockEntity.getLevel(), null, 0);
            }
        } else {
            for (ItemStackRenderState side : state.filters) {
                side.clear();
            }
        }
    }

    @Override
    public void submit(PedestalRenderState state, PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        if (state.hasItem) {
            poseStack.pushPose();
            poseStack.translate(0.5D, 1.2D, 0.5D);
            poseStack.mulPose(Axis.YP.rotationDegrees(state.rotation));
            poseStack.scale(1.0F, 1.0F, 1.0F);
            // 1.20.1: itemRenderer.renderStatic(stack, GROUND, packedLight, NO_OVERLAY, poseStack, bufferSource, level, 0)
            state.item.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        
        for (int i = 0; i < SIDES.length; i++) {
            if (state.filters[i].isEmpty()) continue;
            submitFilterItemOnSide(poseStack, submitNodeCollector, state.lightCoords, state.filters[i], SIDES[i]);
        }
    }

    
    private void submitFilterItemOnSide(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                                        ItemStackRenderState renderState, Direction direction) {
        double y = 0.6875;
        double offset = 0.34375;

        double xOff = 0.0, zOff = 0.0;
        switch (direction) {
            case NORTH: zOff = -offset; break;
            case SOUTH: zOff =  offset; break;
            case EAST:  xOff =  offset; break;
            case WEST:  xOff = -offset; break;
            default: return;
        }

        float angle;
        switch (direction) {
            case NORTH: angle = 0;   break;
            case SOUTH: angle = 180; break;
            case EAST:  angle = 90;  break;
            case WEST:  angle = 270; break;
            default: angle = 0;
        }

        poseStack.pushPose();
        poseStack.translate(0.5 + xOff, y, 0.5 + zOff);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        renderState.submit(poseStack, submitNodeCollector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
