package org.tdddd.eej.impl.element;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.DataPackRegistryEvent;
import org.tdddd.eej.impl.eej;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.List;

/**
 * The synced, data driven element table of the mod.
 *
 * <p>{@code eej:element} is a datapack registry: a data pack (or another mod) may add, replace or
 * remove elements by shipping {@code data/<namespace>/eej/element/<path>.json} files, each
 * containing an object matching {@link Element#CODEC} (currently just {@code {}}). The registry is
 * synced to every client, so the client can translate ids into names and textures without an
 * extra packet.</p>
 *
 * <p>Data files live under {@code data/<namespace>/eej/element/}; the entry id therefore is
 * {@code <namespace>:<file name>}, e.g. {@code eej:fire}.</p>
 */
public final class EejElements {
    /** Registry key of the element registry: {@code eej:element}. */
    public static final ResourceKey<Registry<Element>> ELEMENT_REGISTRY =
            ResourceKey.createRegistryKey(new ResourceLocation(eej.MODID, "element"));

    /** Codec used for both the datapack files and the network sync of the registry. */
    public static final Codec<Element> CODEC = Element.CODEC;

    /** Sorts element ids by namespace first and path second, so tab order is deterministic. */
    private static final Comparator<ResourceLocation> ID_ORDER =
            Comparator.comparing(ResourceLocation::getNamespace).thenComparing(ResourceLocation::getPath);

    private EejElements() {
    }

    /**
     * Registers {@code eej:element} as a synced datapack registry.
     *
     * @param event the Forge mod bus event that collects the datapack registries of this mod
     */
    public static void register(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ELEMENT_REGISTRY, CODEC, CODEC);
    }

    /**
     * @param registryAccess the registry access to read from, may be {@code null}
     * @return the element registry, or {@code null} when it is not loaded (for example on a
     *         server that has not finished starting, or in a context without datapack access)
     */
    @Nullable
    public static Registry<Element> registry(@Nullable RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return null;
        }
        return registryAccess.registry(ELEMENT_REGISTRY).orElse(null);
    }

    /**
     * @param registryAccess the registry access to read from
     * @return every registered element id, sorted by namespace and then by path
     */
    public static List<ResourceLocation> allIds(@Nullable RegistryAccess registryAccess) {
        Registry<Element> registry = registry(registryAccess);
        if (registry == null) {
            return List.of();
        }
        return registry.keySet().stream().sorted(ID_ORDER).toList();
    }

    /**
     * @param registryAccess the registry access to read from
     * @param id             the element id to look up
     * @return the element registered under {@code id}, or {@code null} when there is none
     */
    @Nullable
    public static Element get(@Nullable RegistryAccess registryAccess, @Nullable ResourceLocation id) {
        Registry<Element> registry = registry(registryAccess);
        if (registry == null || id == null) {
            return null;
        }
        return registry.get(id);
    }
}
