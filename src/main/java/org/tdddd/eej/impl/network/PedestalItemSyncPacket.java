package org.tdddd.eej.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;


public class PedestalItemSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PedestalItemSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(EejNetwork.id("pedestal_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PedestalItemSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(PedestalItemSyncPacket::encode, PedestalItemSyncPacket::new);

    private final BlockPos pos;
    private final ItemStack stack;

    public PedestalItemSyncPacket(BlockPos pos, ItemStack stack) {
        this.pos = pos;
        this.stack = stack.copy();
    }

    public PedestalItemSyncPacket(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, this.stack);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (FMLEnvironment.getDist() != Dist.CLIENT) return;
            handleClient();
        });
    }

    private void handleClient() {
        if (Minecraft.getInstance().level != null) {
            var be = Minecraft.getInstance().level.getBlockEntity(this.pos);
            if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                pedestal.syncItem(this.stack);
            }
        }
    }
}
