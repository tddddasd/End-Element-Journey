package org.tdddd.eej.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;


/**
 * Sends an altar pedestal's mana value to watching clients, and with {@code flash} set, tells them a
 * craft was just refused for lack of mana so the Jade mana bar can flash red.
 *
 * <p>The flash travels as a one-shot flag rather than as a counter: a client that merely loads the
 * chunk (or receives the routine resync from {@code onLoad}) must never flash, only the clients that
 * were already watching when the craft was refused.</p>
 *
 * <p>Port note (26.1.2): a {@link CustomPacketPayload} with a {@link StreamCodec}, registered in
 * {@code EejNetwork}, exactly like {@link PedestalItemSyncPacket}.</p>
 */
public class AltarManaSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AltarManaSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(EejNetwork.id("altar_mana"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AltarManaSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(AltarManaSyncPacket::encode, AltarManaSyncPacket::new);

    private final BlockPos pos;
    private final int mana;
    private final boolean flash;

    public AltarManaSyncPacket(BlockPos pos, int mana, boolean flash) {
        this.pos = pos;
        this.mana = mana;
        this.flash = flash;
    }

    public AltarManaSyncPacket(RegistryFriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.mana = buf.readVarInt();
        this.flash = buf.readBoolean();
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.mana);
        buf.writeBoolean(this.flash);
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
                pedestal.applyClientMana(this.mana, this.flash);
            }
        }
    }
}
