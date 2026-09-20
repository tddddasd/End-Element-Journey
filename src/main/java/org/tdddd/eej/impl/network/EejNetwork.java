package org.tdddd.eej.impl.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.tdddd.eej.impl.eej;


public class EejNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(EejNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(PedestalItemSyncPacket.TYPE, PedestalItemSyncPacket.STREAM_CODEC,
                PedestalItemSyncPacket::handle);
        registrar.playToClient(MobEnchantmentSyncPacket.TYPE, MobEnchantmentSyncPacket.STREAM_CODEC,
                MobEnchantmentSyncPacket::handle);
        registrar.playToServer(SmallItemFrameDataPacket.TYPE, SmallItemFrameDataPacket.STREAM_CODEC,
                SmallItemFrameDataPacket::handle);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(eej.MODID, path);
    }

    private EejNetwork() {
    }
}
