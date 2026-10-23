package org.tdddd.eej.impl.element.plate.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.tdddd.eej.impl.element.ElementStack;
import org.tdddd.eej.impl.element.plate.blockentity.ElementAggregationPlateBlockEntity;

import java.util.List;

/**
 * Draws the element plates stored in an {@link ElementAggregationPlateBlockEntity}.
 *
 * <p>Input elements orbit the block centre at a radius of 7.5 pixels, evenly spaced so the gap shrinks
 * as the plate fills. Each one is a flat 2x2 pixel quad that bobs up and down, slowly turns clockwise
 * and is drawn full bright with a per element flicker. The output element sits still at the centre of
 * the plate.</p>
 */
public class ElementAggregationPlateRenderer implements BlockEntityRenderer<ElementAggregationPlateBlockEntity> {

    /** Distance of the orbiting elements from the block centre, in blocks (7.5 pixels). */
    private static final float ORBIT_RADIUS = 7.5F / 16.0F;
    /** Half size of one element quad, in blocks (1 pixel, so the quad is 2x2 pixels). */
    private static final float QUAD_HALF = 1.0F / 16.0F;
    /** Height the orbiting elements hover at, in blocks (just above the 3 pixel plate). */
    private static final float ORBIT_HEIGHT = 3.5F / 16.0F;
    /** Height of the output element at the plate centre. */
    private static final float OUTPUT_HEIGHT = 3.6F / 16.0F;

    /** Flat white texture, so the vertex colour alone decides the pixel colour. */
    private static final ResourceLocation WHITE =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    /**
     * Unlit, un-culled, additive-free alpha blending: the element plates are translucent glowing
     * quads, so they need a render type that keeps the vertex alpha instead of ignoring it.
     */
    private static final RenderType TRANSLUCENT_QUADS = RenderType.entityTranslucentEmissive(WHITE);

    public ElementAggregationPlateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ElementAggregationPlateBlockEntity plate, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        long gameTime = plate.getLevel() == null ? 0L : plate.getLevel().getGameTime();
        float time = gameTime + partialTick;

        List<ResourceLocation> elements = plate.elements();
        int count = elements.size();
        for (int i = 0; i < count; i++) {
            float phase = (float) (Math.PI * 2.0D * i / Math.max(count, 1));
            // clockwise, one turn every ~18 seconds
            float orbit = phase - time * 0.35F * 0.02F;
            float bob = (float) Math.sin(time * 0.12D + i * 1.7D) * 0.75F / 16.0F;

            float x = 0.5F + (float) Math.cos(orbit) * ORBIT_RADIUS;
            float z = 0.5F + (float) Math.sin(orbit) * ORBIT_RADIUS;
            // face the block centre
            float yaw = (float) Math.toDegrees(-orbit);

            float alpha = 0.55F + 0.45F * (float) (0.5D + 0.5D * Math.sin(time * 0.08D + i * 2.3D));
            drawQuad(poseStack, bufferSource, elements.get(i), x, ORBIT_HEIGHT + bob, z, yaw, alpha);
        }

        if (!plate.outputSlot().isEmpty()) {
            // output element rests exactly on the block centre
            ResourceLocation id = ElementStack.id(plate.outputSlot());
            drawQuad(poseStack, bufferSource, id, 0.5F, OUTPUT_HEIGHT, 0.5F, 0.0F, 1.0F);
        }
    }

    /** Draws one flat element quad facing {@code yaw}. */
    private void drawQuad(PoseStack poseStack, MultiBufferSource bufferSource, ResourceLocation id,
                          float x, float y, float z, float yaw, float alpha) {
        int color = colorOf(id);
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        VertexConsumer buffer = bufferSource.getBuffer(TRANSLUCENT_QUADS);
        var pose = poseStack.last().pose();
        vertex(buffer, pose, -QUAD_HALF, -QUAD_HALF, 0.0F, r, g, b, alpha, 0.0F, 1.0F);
        vertex(buffer, pose, QUAD_HALF, -QUAD_HALF, 0.0F, r, g, b, alpha, 1.0F, 1.0F);
        vertex(buffer, pose, QUAD_HALF, QUAD_HALF, 0.0F, r, g, b, alpha, 1.0F, 0.0F);
        vertex(buffer, pose, -QUAD_HALF, QUAD_HALF, 0.0F, r, g, b, alpha, 0.0F, 0.0F);

        poseStack.popPose();
    }

    private static void vertex(VertexConsumer buffer, org.joml.Matrix4f pose,
                               float x, float y, float z, float r, float g, float b, float a,
                               float u, float v) {
        // full bright (the plates glow) and no overlay, with the quad filling the white texture
        buffer.vertex(pose, x, y, z).color(r, g, b, a)
                .uv(u, v)
                .overlayCoords(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 0.0F, 1.0F)
                .endVertex();
    }

    /** Base colour of the five shipped elements; anything else falls back to white. */
    private static int colorOf(ResourceLocation id) {
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
