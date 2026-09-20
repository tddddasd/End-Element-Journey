package org.tdddd.eej.impl.capability;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.impl.eej;


@Mod.EventBusSubscriber(modid = eej.MODID)
public class EejCapabilityAttachmentEvents {
    public static final ResourceLocation MOB_ENCHANTMENTS_ID = new ResourceLocation(eej.MODID, "mob_enchantments");

    @SubscribeEvent
    public static void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof LivingEntity) {
            event.addCapability(MOB_ENCHANTMENTS_ID, new MobEnchantmentProvider());
        }
    }
}
