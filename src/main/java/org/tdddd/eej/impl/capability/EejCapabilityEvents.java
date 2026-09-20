package org.tdddd.eej.impl.capability;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.impl.eej;


@Mod.EventBusSubscriber(modid = eej.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EejCapabilityEvents {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IMobEnchantments.class);
    }
}
