package org.tdddd.eej.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;

import java.util.function.Supplier;


/**
 * Sends an altar pedestal's mana value to watching clients, and with {@code flash} set, tells them a
 * craft was just refused for lack of mana so the Jade mana bar can flash red.
 *
 * <p>The flash travels as a one-shot flag rather than as a counter: a client that merely loads the
 * chunk (or receives the routine resync from {@code onLoad}) must never flash, only the clients that
 * were already watching when the craft was refused.</p>
 */
public class AltarManaSyncPacket {
    private final BlockPos pos;
    private final int mana;
    private final boolean flash;

    public AltarManaSyncPacket(BlockPos pos, int mana, boolean flash) {
        this.pos = pos;
        this.mana = mana;
        this.flash = flash;
    }

    public static void encode(AltarManaSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeVarInt(pkt.mana);
        buf.writeBoolean(pkt.flash);
    }

    public static AltarManaSyncPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int mana = buf.readVarInt();
        boolean flash = buf.readBoolean();
        return new AltarManaSyncPacket(pos, mana, flash);
    }

    public static void handle(AltarManaSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().level != null) {
                var be = Minecraft.getInstance().level.getBlockEntity(pkt.pos);
                if (be instanceof PackedMudPedestalBlockEntity pedestal) {
                    pedestal.applyClientMana(pkt.mana, pkt.flash);
                }
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
