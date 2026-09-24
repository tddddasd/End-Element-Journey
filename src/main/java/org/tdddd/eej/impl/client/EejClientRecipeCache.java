package org.tdddd.eej.impl.client;

import net.minecraft.world.item.crafting.RecipeMap;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import org.tdddd.eej.impl.eej;

/**
 * Caches the recipe content NeoForge syncs to a client.
 *
 * <p>26.1.2 no longer ships the whole recipe list in {@code ClientboundUpdateRecipesPacket} (it only carries
 * recipe property sets and the stonecutter recipes), so the client's {@code RecipeAccess} is a
 * {@code ClientRecipeContainer} and not a {@code RecipeManager}. NeoForge sends the real content through
 * {@code RecipeContentPayload} and exposes it as {@link RecipesReceivedEvent}; JEI itself waits for that event
 * before starting. Client side features that need the loaded recipes (the JEI categories) read them from here.
 */
@EventBusSubscriber(modid = eej.MODID, value = Dist.CLIENT)
public final class EejClientRecipeCache {
    private static volatile RecipeMap synced = RecipeMap.EMPTY;

    private EejClientRecipeCache() {
    }

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        synced = event.getRecipeMap();
    }

    /** The synced recipe content, or {@link RecipeMap#EMPTY} before the first sync. */
    public static RecipeMap synced() {
        return synced;
    }
}
