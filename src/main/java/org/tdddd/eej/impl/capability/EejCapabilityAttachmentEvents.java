package org.tdddd.eej.impl.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;


@Mod.EventBusSubscriber(modid = eej.MODID)
public class EejCapabilityAttachmentEvents {
    public static final ResourceLocation MOB_ENCHANTMENTS_ID = new ResourceLocation(eej.MODID, "mob_enchantments");
    public static final ResourceLocation MANA_STORAGE_ID = new ResourceLocation(eej.MODID, "mana_storage");

    @SubscribeEvent
    public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(MOB_ENCHANTMENTS_ID, new MobEnchantmentProvider());
        }
    }

    
    @SubscribeEvent
    public static void attachBlockEntityCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof PackedMudPedestalBlockEntity pedestal) {
            event.addCapability(MANA_STORAGE_ID, new ManaStorageProvider(pedestal.getManaStorage()));
        } else if (event.getObject() instanceof CreativeManaSourceBlockEntity source) {
            event.addCapability(MANA_STORAGE_ID, new ManaStorageProvider(source.getManaStorage()));
        }
    }
}
