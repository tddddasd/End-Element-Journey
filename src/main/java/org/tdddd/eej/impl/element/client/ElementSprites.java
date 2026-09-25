package org.tdddd.eej.impl.element.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Resolves and caches the sprite an element uses.
 *
 * <p>An element {@code <namespace>:<path>} uses the sprite {@code <namespace>:item/element/<path>},
 * i.e. the texture file {@code assets/<namespace>/textures/item/element/<path>.png}. That sprite is
 * stitched into the block atlas by the atlas source files the namespace ships; eej contributes a
 * directory source for {@code item/element/} in
 * {@code assets/eej/atlases/blocks.json}.</p>
 *
 * <p>Lookups are cached because they run once per element per rendered frame. The cache is dropped
 * whenever the block atlas is reloaded (see {@link ElementClientSetup}), so it never keeps a sprite
 * of a texture pack or data pack that has been replaced. Misses are cached too: nothing can add a
 * texture to an already stitched atlas, and the cache is dropped on the next reload anyway, so a
 * nameless id cannot cause a filesystem lookup every frame.</p>
 */
public final class ElementSprites {
    private static final Map<ResourceLocation, TextureAtlasSprite> CACHE = new HashMap<>();

    private ElementSprites() {
    }

    /**
     * @param id the element id
     * @return the sprite to draw for {@code id}, or {@code null} when the id is {@code null}, the
     *         block atlas is not loaded yet, or the namespace does not stitch that texture
     */
    @Nullable
    public static TextureAtlasSprite get(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        if (CACHE.containsKey(id)) {
            return CACHE.get(id);
        }
        TextureAtlasSprite sprite = resolve(id);
        CACHE.put(id, sprite);
        return sprite;
    }

    /**
     * Forgets every cached sprite. Called when the block atlas has been stitched again.
     */
    public static void invalidate() {
        CACHE.clear();
    }

    @Nullable
    private static TextureAtlasSprite resolve(ResourceLocation id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            return null;
        }
        if (!(minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS) instanceof TextureAtlas atlas)) {
            return null;
        }
        ResourceLocation spriteId = new ResourceLocation(id.getNamespace(), "item/element/" + id.getPath());
        if (!atlas.getTextureLocations().contains(spriteId)) {
            return null;
        }
        TextureAtlasSprite sprite = atlas.getSprite(spriteId);
        if (sprite == null || sprite.contents() == null) {
            return null;
        }
        // A stitched entry is not proof that a texture file exists (resource packs may alias
        // sprites); without the file the atlas would draw the missing texture checkerboard.
        ResourceManager resources = minecraft.getResourceManager();
        ResourceLocation textureFile = new ResourceLocation(
                spriteId.getNamespace(), "textures/" + spriteId.getPath() + ".png");
        if (resources == null || resources.getResource(textureFile).isEmpty()) {
            return null;
        }
        return sprite;
    }
}
