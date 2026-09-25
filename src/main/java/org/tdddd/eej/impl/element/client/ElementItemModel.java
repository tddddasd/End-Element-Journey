package org.tdddd.eej.impl.element.client;

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.server.packs.resources.PreparableReloadListener.SharedState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.quad.BakedColors;
import net.neoforged.neoforge.client.model.quad.BakedNormals;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;
import org.tdddd.eej.impl.element.ElementStack;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * The custom item model type {@code eej:element}, registered on the mod bus through
 * {@code RegisterItemModelsEvent}.
 *
 * <p>The item has no texture of its own. This model resolves the element id of the stack to the sprite
 * {@code <ns>:item/element/<path>} in the block atlas and submits exactly one flat quad for it. When the stack
 * has no id, or the texture resource does not exist, nothing is submitted at all - so a textureless element
 * shows up as an empty item instead of the missing-texture checkerboard.
 *
 * <p>Both the sprite lookup and the resource existence check are cached, and the cache is dropped on every
 * resource reload by the listener registered in {@code EejClientSetup}.
 *
 * <p>Only the {@code eej} namespace ships the atlas directory source for {@code item/element/}; any other
 * namespace that wants element textures must contribute the same directory source in its own
 * {@code assets/<ns>/atlases/blocks.json}, otherwise its sprites are never stitched and resolve to nothing.
 */
@OnlyIn(Dist.CLIENT)
public class ElementItemModel implements ItemModel {
    /** Flat item geometry: 0..16 on X/Y and the vanilla 7.5..8.5 slab on Z, in block units. */
    private static final float MIN_X = 0.0F;
    private static final float MIN_Y = 0.0F;
    private static final float MAX_X = 1.0F;
    private static final float MAX_Y = 1.0F;
    private static final float MIN_Z = 7.5F / 16.0F;
    private static final float MAX_Z = 8.5F / 16.0F;

    private static final Supplier<Vector3fc[]> EXTENTS = () -> new Vector3fc[] {
            new Vector3f(MIN_X, MIN_Y, MIN_Z),
            new Vector3f(MAX_X, MAX_Y, MAX_Z)
    };

    /**
     * Resolved geometry by element id. The value is {@code null} when the id has no texture, and that answer is
     * cached too, so a textureless element never touches the resource manager or the atlas again. The quad is
     * baked once per id as well, because {@code update} runs for every rendered stack and every frame.
     */
    private static final Map<Identifier, Resolved> CACHE = new HashMap<>();

    /**
     * Drops the model's caches when resources are reloaded. Sprites are rebound by a reload, so the cached
     * {@link TextureAtlasSprite} instances and the "does this texture exist" answers would otherwise be stale.
     */
    public static final PreparableReloadListener RELOAD_LISTENER = new PreparableReloadListener() {
        @Override
        public CompletableFuture<Void> reload(SharedState currentReload, Executor taskExecutor,
                                              PreparationBarrier preparationBarrier, Executor reloadExecutor) {
            return preparationBarrier.wait(null).thenRunAsync(ElementItemModel::invalidateCache, reloadExecutor);
        }
    };

    /** Drops every cached lookup; runs from {@link #RELOAD_LISTENER}. */
    private static void invalidateCache() {
        CACHE.clear();
    }

    @Override
    public void update(ItemStackRenderState output, ItemStack item, ItemModelResolver resolver,
                       ItemDisplayContext displayContext, @Nullable ClientLevel level,
                       @Nullable ItemOwner owner, int seed) {
        Identifier id = ElementStack.id(item);
        if (id == null) {
            output.appendModelIdentityElement(this);
            return;
        }

        Resolved resolved = resolve(id);
        if (resolved == null) {
            output.appendModelIdentityElement(this);
            return;
        }

        output.appendModelIdentityElement(id);

        ItemStackRenderState.LayerRenderState layer = output.newLayer();
        layer.setItemTransform(ItemTransform.NO_TRANSFORM);
        layer.setExtents(EXTENTS);
        layer.setParticleMaterial(new Material.Baked(resolved.sprite(), false));
        layer.prepareQuadList().add(resolved.quad());
    }

    /**
     * Builds the single flat quad. The vertex order and UV corner assignment are the conventional face order used
     * by {@code FaceBakery}: the u0 corners are the first pair of vertices, the v0 corners are the first and last.
     */
    private static BakedQuad createQuad(TextureAtlasSprite sprite) {
        Vector3fc v0 = new Vector3f(MIN_X, MIN_Y, MAX_Z);
        Vector3fc v1 = new Vector3f(MIN_X, MAX_Y, MAX_Z);
        Vector3fc v2 = new Vector3f(MAX_X, MAX_Y, MAX_Z);
        Vector3fc v3 = new Vector3f(MAX_X, MIN_Y, MAX_Z);

        long uv0 = UVPair.pack(sprite.getU0(), sprite.getV0());
        long uv1 = UVPair.pack(sprite.getU0(), sprite.getV1());
        long uv2 = UVPair.pack(sprite.getU1(), sprite.getV1());
        long uv3 = UVPair.pack(sprite.getU1(), sprite.getV0());

        BakedQuad.MaterialInfo materialInfo = BakedQuad.MaterialInfo.of(
                new Material.Baked(sprite, false), Transparency.NONE, -1, false, 0);

        return new BakedQuad(v0, v1, v2, v3, uv0, uv1, uv2, uv3, Direction.SOUTH, materialInfo,
                BakedNormals.of(BakedNormals.computeQuadNormal(v0, v1, v2, v3)), BakedColors.DEFAULT);
    }

    /**
     * Cached render geometry for one element id, or {@code null} when the id has no texture at all.
     *
     * <p>The existence check is done against the {@code textures/item/element/<path>.png} resource, because the
     * atlas silently answers with the missing-texture sprite for anything that was never stitched; the atlas is
     * only consulted once that check passed.
     */
    private static @Nullable Resolved resolve(Identifier id) {
        if (CACHE.containsKey(id)) {
            return CACHE.get(id);
        }

        Resolved resolved = null;
        if (textureExists(id)) {
            TextureAtlasSprite sprite = atlasSprite(id);
            if (sprite != null) {
                resolved = new Resolved(sprite, createQuad(sprite));
            }
        }
        CACHE.put(id, resolved);
        return resolved;
    }

    private static @Nullable TextureAtlasSprite atlasSprite(Identifier id) {
        TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(ElementStack.textureId(id));
        if (sprite == null || sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return null;
        }
        return sprite;
    }

    private static boolean textureExists(Identifier id) {
        Identifier file = Identifier.fromNamespaceAndPath(id.getNamespace(),
                "textures/" + ElementStack.TEXTURE_DIRECTORY + "/" + id.getPath() + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(file).isPresent();
    }

    /** One element's cached sprite and its pre-baked flat quad. */
    private record Resolved(TextureAtlasSprite sprite, BakedQuad quad) {
    }

    /**
     * Codec half of the model type. The JSON has no options beyond {@code "type": "eej:element"}, so the unbaked
     * model is a unit codec.
     */
    public record Unbaked() implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        /** The model type has no referenced models, so there is nothing to declare here. */
        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            return new ElementItemModel();
        }
    }
}
