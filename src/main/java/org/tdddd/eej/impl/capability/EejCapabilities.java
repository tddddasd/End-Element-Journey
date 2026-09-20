package org.tdddd.eej.impl.capability;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantments;
import org.tdddd.eej.impl.eej;


public final class EejCapabilities {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, eej.MODID);

    
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<IMobEnchantments>> MOB_ENCHANTMENTS =
            ATTACHMENT_TYPES.register("mob_enchantments",
                    () -> AttachmentType.<IMobEnchantments>serializable(MobEnchantments::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    private EejCapabilities() {
    }
}
