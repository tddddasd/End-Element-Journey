package org.tdddd.eej.impl.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.tdddd.eej.impl.eej;

public class EejNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(eej.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, PedestalItemSyncPacket.class,
                PedestalItemSyncPacket::encode,
                PedestalItemSyncPacket::decode,
                PedestalItemSyncPacket::handle);

        INSTANCE.messageBuilder(SmallItemFrameDataPacket.class, id++)
                .encoder(SmallItemFrameDataPacket::encode)
                .decoder(SmallItemFrameDataPacket::decode)
                .consumerMainThread(SmallItemFrameDataPacket::handle)
                .add();

        INSTANCE.registerMessage(id++, MobEnchantmentSyncPacket.class,
                MobEnchantmentSyncPacket::encode,
                MobEnchantmentSyncPacket::decode,
                MobEnchantmentSyncPacket::handle);
    }
}
