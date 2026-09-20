package org.tdddd.eej.impl.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import org.tdddd.eej.api.mob.IMobEnchantments;


public final class EejCapabilities {

    
    public static final Capability<IMobEnchantments> MOB_ENCHANTMENTS =
            CapabilityManager.get(new CapabilityToken<>() {
            });

    private EejCapabilities() {
    }
}
