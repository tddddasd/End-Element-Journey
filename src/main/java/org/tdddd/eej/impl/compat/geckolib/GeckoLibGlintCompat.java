package org.tdddd.eej.impl.compat.geckolib;

import net.minecraftforge.common.MinecraftForge;


public final class GeckoLibGlintCompat {

    private GeckoLibGlintCompat() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(GeckoLibGlintEvents.class);
    }
}
