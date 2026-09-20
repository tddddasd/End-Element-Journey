package org.tdddd.eej.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantment;
import org.tdddd.eej.api.mob.MobEnchantmentApi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


public class MobEnchantmentSyncPacket {
    private final int entityId;
    private final List<MobEnchantment> entries;

    public MobEnchantmentSyncPacket(int entityId, List<MobEnchantment> entries) {
        this.entityId = entityId;
        this.entries = entries;
    }

    public static void encode(MobEnchantmentSyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.entityId);
        buf.writeVarInt(pkt.entries.size());
        for (MobEnchantment entry : pkt.entries) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getEnchantment());
            buf.writeResourceLocation(id == null ? new ResourceLocation("minecraft", "sharpness") : id);
            buf.writeVarInt(entry.getLevel());
            buf.writeVarInt(entry.getRemainingTicks());
        }
    }

    public static MobEnchantmentSyncPacket decode(FriendlyByteBuf buf) {
        int entityId = buf.readVarInt();
        int size = buf.readVarInt();
        List<MobEnchantment> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ResourceLocation id = buf.readResourceLocation();
            int level = buf.readVarInt();
            int ticks = buf.readVarInt();
            Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(id);
            if (enchantment != null) {
                entries.add(new MobEnchantment(enchantment, level, ticks));
            }
        }
        return new MobEnchantmentSyncPacket(entityId, entries);
    }

    public static void handle(MobEnchantmentSyncPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            if (Minecraft.getInstance().level == null) return;
            Entity entity = Minecraft.getInstance().level.getEntity(pkt.entityId);
            if (entity == null) return;
            IMobEnchantments data = MobEnchantmentApi.get(entity);
            if (data != null) {
                data.setAll(pkt.entries);
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}
