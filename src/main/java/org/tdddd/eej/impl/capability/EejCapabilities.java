package org.tdddd.eej.impl.capability;

import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantments;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;
import org.tdddd.eej.impl.registry.EejBlockEntities;

public final class EejCapabilities {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, eej.MODID);

    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IMobEnchantments>> MOB_ENCHANTMENTS =
            ATTACHMENT_TYPES.register("mob_enchantments",
                    () -> AttachmentType.<IMobEnchantments>serializable(MobEnchantments::new).build());

    
    public static final BlockCapability<IManaStorage, Direction> MANA_STORAGE =
            BlockCapability.createSided(eej.asResource("mana_storage"), IManaStorage.class);

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    
    public static void registerManaProviders(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(MANA_STORAGE, EejBlockEntities.CREATIVE_MANA_SOURCE.get(),
                (be, side) -> new ManaStorageProvider(be.getManaStorage()).getManaStorage());
        event.registerBlockEntity(MANA_STORAGE, EejBlockEntities.ELEMENT_AGGREGATION_PLATE.get(),
                (be, side) -> be.manaStorage());
    }

    private EejCapabilities() {
    }
}
