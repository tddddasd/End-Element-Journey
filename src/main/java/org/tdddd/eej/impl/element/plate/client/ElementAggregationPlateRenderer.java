package org.tdddd.eej.impl.element.plate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.tdddd.eej.impl.element.ElementStack;
import org.tdddd.eej.impl.element.plate.blockentity.ElementAggregationPlateBlockEntity;

/**
 * Draws the element plates stored in an {@link ElementAggregationPlateBlockEntity}.
 *
 * <p>Input elements orbit the block centre at a radius of 7.5 pixels, evenly spaced so the gap shrinks
 * as the plate fills. Each one is a flat 2x2 pixel quad that bobs up and down, slowly turns clockwise
 * and is drawn full bright with a per element flicker. The output element sits still at the centre of
 * the plate. The same look is implemented in the 1.20.1 tree.</p>
 */
public class ElementAggregationPlateRenderer
        implements BlockEntityRenderer<ElementAggregationPlateBlockEntity, ElementAggregationRenderState> {

    /** Distance of the orbiting elements from the block centre, in blocks (7.5 pixels). */
    private static final float ORBIT_RADIUS = 7.5F / 16.0F;
    /** Half size of one element quad, in blocks (1 pixel, so the quad is 2x2 pixels). */
    private static final float QUAD_HALF = 1.0F / 16.0F;
    /** Height the orbiting elements hover at, in blocks (just above the 3 pixel plate). */
    private static final float ORBIT_HEIGHT = 3.5F / 16.0F;
    /** Height of the output element at the plate centre. */
    private static final float OUTPUT_HEIGHT = 3.6F / 16.0F;

    /** Flat white texture, so the vertex colour alone decides the pixel colour. */
    private static final Identifier WHITE =
            Identifier.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

    /**
     * Unlit, un-culled alpha blending: the element plates are translucent glowing quads, so the render
     * type must keep the vertex alpha instead of ignoring it.
     */
    private static final RenderType TRANSLUCENT_QUADS = RenderTypes.entityTranslucentEmissive(WHITE);

    public ElementAggregationPlateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ElementAggregationRenderState createRenderState() {
        return new ElementAggregationRenderState();
    }

    @Override
    public void extractRenderState(ElementAggregationPlateBlockEntity plate, ElementAggregationRenderState state,
                                   float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(plate, state, partialTicks, cameraPosition, breakProgress);

        long gameTime = plate.getLevel() != null ? plate.getLevel().getGameTime() : 0L;
        state.time = gameTime + partialTicks;
        state.elements.clear();
        state.elements.addAll(plate.elements());
        state.output = plate.outputSlot().isEmpty() ? null : ElementStack.id(plate.outputSlot());
    }

    @Override
    public void submit(ElementAggregationRenderState state, PoseStack poseStack,
                       SubmitNodeCollector collector, CameraRenderState camera) {
        int count = state.elements.size();
        for (int i = 0; i < count; i++) {
            float phase = (float) (Math.PI * 2.0D * i / Math.max(count, 1));
            // clockwise, about eighteen seconds per turn
            float orbit = phase - state.time * 0.35F * 0.02F;
            float bob = (float) Math.sin(state.time * 0.12D + i * 1.7D) * 0.75F / 16.0F;

            float x = 0.5F + (float) Math.cos(orbit) * ORBIT_RADIUS;
            float z = 0.5F + (float) Math.sin(orbit) * ORBIT_RADIUS;
            float yaw = (float) Math.toDegrees(-orbit);
            float alpha = 0.55F + 0.45F * (float) (0.5D + 0.5D * Math.sin(state.time * 0.08D + i * 2.3D));

            this.submitQuad(collector, poseStack, state.elements.get(i), x, ORBIT_HEIGHT + bob, z, yaw, alpha);
        }

        if (state.output != null) {
            // output element rests exactly on the block centre
            this.submitQuad(collector, poseStack, state.output, 0.5F, OUTPUT_HEIGHT, 0.5F, 0.0F, 1.0F);
        }
    }

    /** Submits one flat element quad facing {@code yaw}. */
    private void submitQuad(SubmitNodeCollector collector, PoseStack poseStack, Identifier id,
                            float x, float y, float z, float yaw, float alpha) {
        int color = colorOf(id);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        // Copy the matrix now: the submitted lambda may run after the pose stack has moved on.
        final org.joml.Matrix4f matrix = new org.joml.Matrix4f(poseStack.last().pose());
        collector.submitCustomGeometry(poseStack, TRANSLUCENT_QUADS, (pose, buffer) -> {
            vertex(buffer, matrix, -QUAD_HALF, -QUAD_HALF, r, g, b, alpha, 0.0F, 1.0F);
            vertex(buffer, matrix, QUAD_HALF, -QUAD_HALF, r, g, b, alpha, 1.0F, 1.0F);
            vertex(buffer, matrix, QUAD_HALF, QUAD_HALF, r, g, b, alpha, 1.0F, 0.0F);
            vertex(buffer, matrix, -QUAD_HALF, QUAD_HALF, r, g, b, alpha, 0.0F, 0.0F);
        });

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, org.joml.Matrix4f matrix,
                               float x, float y, float r, float g, float b, float a, float u, float v) {
        // full bright (0xF000F0) and no overlay, with the quad filling the white texture
        buffer.addVertex(matrix, x, y, 0.0F).setColor(r, g, b, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                .setNormal(0.0F, 0.0F, 1.0F);
    }

    /** Base colour of the five shipped elements; anything else falls back to white. */
    private static int colorOf(Identifier id) {
        if (id == null) {
            return 0xFFFFFF;
        }
        return switch (id.getPath()) {
            case "fire" -> 0xFF8A2B;      // orange
            case "water" -> 0x9FD8F5;     // light blue
            case "wind" -> 0xC8CCD0;      // silver
            case "spirit" -> 0x6FD46F;    // green
            case "void" -> 0x4B2A6B;      // deep purple
            default -> 0xFFFFFF;
        };
    }
}
