package org.tdddd.eej.impl.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;


public final class EejEnchantments {

    private EejEnchantments() {
    }

    
    public static Registry<Enchantment> registry() {
        return access().lookupOrThrow(Registries.ENCHANTMENT);
    }

    
    public static Holder<Enchantment> holder(ResourceKey<Enchantment> key) {
        return registry().getOrThrow(key);
    }

    
    public static Holder<Enchantment> holder(net.minecraft.resources.Identifier id) {
        return registry().get(id).orElse(null);
    }

    private static RegistryAccess access() {
        try {
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                return server.registryAccess();
            }
        } catch (Throwable ignored) {
            
        }
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }
}
