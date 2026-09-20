package org.tdddd.eej.impl.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.eej.impl.altar.item.SmallItemFrame;


public class SmallItemFrameDataPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SmallItemFrameDataPacket> TYPE =
            new CustomPacketPayload.Type<>(EejNetwork.id("small_item_frame_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SmallItemFrameDataPacket> STREAM_CODEC =
            CustomPacketPayload.codec(SmallItemFrameDataPacket::encode, SmallItemFrameDataPacket::new);

    private final InteractionHand hand;
    private final String data;

    public SmallItemFrameDataPacket(InteractionHand hand, String data) {
        this.hand = hand;
        this.data = data;
    }

    public SmallItemFrameDataPacket(RegistryFriendlyByteBuf buf) {
        this.hand = buf.readEnum(InteractionHand.class);
        this.data = buf.readUtf();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeUtf(this.data);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Player player = ctx.player();
            if (player == null) return;
            ItemStack stack = player.getItemInHand(this.hand);
            if (stack.getItem() instanceof SmallItemFrame) {
                SmallItemFrame.setItemIds(stack, SmallItemFrame.parseIds(this.data));
            }
        });
    }
}
