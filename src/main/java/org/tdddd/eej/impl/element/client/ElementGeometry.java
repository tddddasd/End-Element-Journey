package org.tdddd.eej.impl.element.client;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.math.Transformation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.SimpleModelState;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import org.joml.Vector3f;
import org.tdddd.eej.impl.element.ElementStack;
import org.tdddd.eej.impl.eej;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The {@code eej:element} geometry loader: a flat item model whose texture depends on the element
 * id of the rendered stack.
 *
 * <p>The model JSON only declares the loader and inherits {@code minecraft:item/generated} for the
 * display transforms; the texture cannot be declared there because it varies per stack. The baked
 * model therefore returns its own {@link ItemOverrides}, which is the per-stack hook this version
 * offers to a custom model loader: {@code ItemRenderer} calls {@code getOverrides().resolve(...)}
 * for every stack it draws and renders the model that comes back. Resolving an element stack
 * therefore yields a small model that owns exactly one quad for
 * {@code <namespace>:item/element/<path>}; resolving a stack without an element id yields the
 * model built by this loader, which draws nothing.</p>
 *
 * <p>The quad is baked with the vanilla {@link FaceBakery} from the standard generated-item
 * element: it spans the full {@code 0..16} model space at {@code z = 7.5}, faces
 * {@link Direction#SOUTH}, uses tint index {@code -1} and is shaded. When the id is missing, the
 * namespace does not stitch the sprite, or the texture file does not exist, the quad list stays
 * empty so nothing is drawn - there is deliberately no missing-texture checkerboard.</p>
 */
public final class ElementGeometry implements IUnbakedGeometry<ElementGeometry> {
    /** Model JSON value of the {@code loader} key. */
    public static final ResourceLocation LOADER_ID = eej.asResource("element");

    /** Front face of a generated item, i.e. the face the player sees in the GUI and in hand. */
    private static final Direction FRONT = Direction.SOUTH;
    /** Tint index of an untinted item layer. */
    private static final int NO_TINT = -1;
    /** One item layer is a single quad. */
    private static final List<BakedQuad> NO_QUADS = List.of();
    private static final Map<Direction, List<BakedQuad>> NO_CULLED_FACES = Map.of();

    private static final FaceBakery FACE_BAKERY = new FaceBakery();
    private static final ModelState IDENTITY = new SimpleModelState(Transformation.identity());

    /**
     * Per-element baked models, so a rendered stack does not rebake its quad every frame. Cleared
     * together with {@link ElementSprites} when the block atlas is reloaded.
     */
    private static final Map<ResourceLocation, BakedModel> MODEL_CACHE = new HashMap<>();

    private ElementGeometry() {
    }

    /**
     * @return the loader to register on the mod bus for {@code eej:element}
     */
    public static IGeometryLoader<ElementGeometry> loader() {
        return Loader.INSTANCE;
    }

    /**
     * Forgets every cached per-element model. Called when the block atlas has been stitched again.
     */
    public static void invalidate() {
        MODEL_CACHE.clear();
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBaker modelBaker,
                           Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState,
                           ItemOverrides overrides, ResourceLocation modelLocation) {
        TextureAtlasSprite missingParticle = spriteGetter.apply(
                new Material(TextureAtlas.LOCATION_BLOCKS, MissingTextureAtlasSprite.getLocation()));
        return new EmptyModel(context.getTransforms(), missingParticle);
    }

    /**
     * Bakes the single flat item quad for {@code sprite}.
     *
     * @param sprite the stitched sprite to draw, may be {@code null}
     * @return a one-quad list, or an empty list when {@code sprite} is {@code null}
     */
    private static List<BakedQuad> bakeQuads(@Nullable TextureAtlasSprite sprite) {
        if (sprite == null) {
            return NO_QUADS;
        }
        BlockElementFace face = new BlockElementFace(null, NO_TINT, "", new BlockFaceUV(null, 0));
        BakedQuad quad = FACE_BAKERY.bakeQuad(
                new Vector3f(0.0F, 0.0F, 7.5F),
                new Vector3f(16.0F, 16.0F, 7.5F),
                face, sprite, FRONT, IDENTITY, null, true, LOADER_ID);
        return List.of(quad);
    }

    /**
     * The model this loader bakes. It draws nothing itself and resolves the element of a stack
     * through {@link #getOverrides()}.
     */
    private static final class EmptyModel implements BakedModel {
        private final ItemTransforms transforms;
        private final TextureAtlasSprite missingParticle;

        private EmptyModel(ItemTransforms transforms, TextureAtlasSprite missingParticle) {
            this.transforms = transforms;
            this.missingParticle = missingParticle;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
            return NO_QUADS;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public boolean isGui3d() {
            return false;
        }

        @Override
        public boolean usesBlockLight() {
            return false;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return missingParticle;
        }

        @Override
        public ItemOverrides getOverrides() {
            return new Overrides(this);
        }

        @Override
        public ItemTransforms getTransforms() {
            // Keeps the flat item display contexts inherited from minecraft:item/generated.
            return transforms;
        }

        /**
         * @param sprite the sprite to draw, may be {@code null}
         * @return a model that draws exactly one flat item quad of {@code sprite}, or this model
         *         when there is no sprite to draw
         */
        BakedModel withSprite(@Nullable TextureAtlasSprite sprite) {
            if (sprite == null) {
                return this;
            }
            return new SimpleBakedModel(bakeQuads(sprite), NO_CULLED_FACES, false, false, false,
                    sprite, transforms, ItemOverrides.EMPTY);
        }
    }

    /**
     * Resolves the element id of a stack into the model that draws that element's sprite. This is
     * the per-stack hook of this version: {@code ItemRenderer} asks the item's model for its
     * overrides for every stack it draws.
     */
    private static final class Overrides extends ItemOverrides {
        private final EmptyModel base;

        private Overrides(EmptyModel base) {
            this.base = base;
        }

        @Override
        public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            ResourceLocation id = ElementStack.id(stack);
            if (id == null) {
                // The bare eej:element item has no texture of its own and must draw nothing.
                return base;
            }
            BakedModel cached = MODEL_CACHE.get(id);
            if (cached == null) {
                cached = base.withSprite(ElementSprites.get(id));
                if (cached != base) {
                    MODEL_CACHE.put(id, cached);
                }
            }
            return cached;
        }
    }

    /**
     * Reads {@code {"loader": "eej:element"}}. The geometry has no configuration, so the JSON body
     * is ignored; a future revision can read extra keys here.
     */
    private static final class Loader implements IGeometryLoader<ElementGeometry> {
        private static final Loader INSTANCE = new Loader();

        @Override
        public ElementGeometry read(JsonObject json, JsonDeserializationContext context) throws JsonParseException {
            return new ElementGeometry();
        }
    }
}
