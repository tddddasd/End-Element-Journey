package org.tdddd.eej.impl.element;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import org.tdddd.eej.impl.eej;

import java.util.Comparator;
import java.util.List;

/**
 * The synced data pack registry {@code eej:element}.
 *
 * <p>Element values live in {@code data/<namespace>/element/<path>.json} and are only known once a data pack has
 * been loaded, so the registry must be registered on the mod event bus during mod construction (see
 * {@link #register(IEventBus)}) and only ever read through a {@link HolderLookup.Provider} that has finished
 * loading.
 */
public final class EejElements {
    /** Registry key of {@code eej:element}. */
    public static final ResourceKey<Registry<Element>> ELEMENT_REGISTRY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(eej.MODID, "element"));

    private EejElements() {
    }

    /**
     * Registers {@code eej:element} as a new data pack registry.
     *
     * <p>{@code DataPackRegistryEvent.NewRegistry} is a mod bus event. Passing only the persistent codec keeps
     * the registry synced to clients (NeoForge derives the network codec from it); pass a second codec here if
     * the persistent and network shapes ever diverge.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EejElements::onNewDataPackRegistry);
    }

    private static void onNewDataPackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(ELEMENT_REGISTRY, Element.CODEC);
    }

    /** Registry lookup, or {@code null} when the provider does not carry {@code eej:element}. */
    public static Registry<Element> registry(HolderLookup.Provider provider) {
        if (provider instanceof RegistryAccess registryAccess) {
            return registryAccess.lookup(ELEMENT_REGISTRY).orElse(null);
        }
        return provider.lookup(ELEMENT_REGISTRY)
                .map(lookup -> (Registry<Element>) lookup)
                .orElse(null);
    }

    /**
     * Every registered element id, sorted by namespace then path so that generated content (creative tab,
     * recipes, ...) has a deterministic order independent of data pack loading order.
     */
    public static List<Identifier> allIds(HolderLookup.Provider provider) {
        Registry<Element> registry = registry(provider);
        if (registry == null) {
            return List.of();
        }
        return registry.keySet().stream()
                .sorted(Comparator.comparing(Identifier::getNamespace).thenComparing(Identifier::getPath))
                .toList();
    }

    /** The element value of {@code id}, or {@code null} when it is not registered. */
    public static Element get(HolderLookup.Provider provider, Identifier id) {
        Registry<Element> registry = registry(provider);
        return registry == null ? null : registry.getOptional(id).orElse(null);
    }

    /** {@code true} when {@code id} is a registered element. */
    public static boolean contains(HolderLookup.Provider provider, Identifier id) {
        return get(provider, id) != null;
    }
}
