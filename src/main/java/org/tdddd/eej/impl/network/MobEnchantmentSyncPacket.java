package org.tdddd.eej.impl.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantment;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.registry.EejEnchantments;

import java.util.ArrayList;
import java.util.List;


public class MobEnchantmentSyncPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MobEnchantmentSyncPacket> TYPE =
            new CustomPacketPayload.Type<>(EejNetwork.id("mob_enchantment"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobEnchantmentSyncPacket> STREAM_CODEC =
            CustomPacketPayload.codec(MobEnchantmentSyncPacket::encode, MobEnchantmentSyncPacket::new);

    private final int entityId;
    private final List<MobEnchantment> entries;

    
    private static final java.util.Map<Integer, List<MobEnchantment>> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.concurrent.atomic.AtomicInteger SYNC_LOGGED = new java.util.concurrent.atomic.AtomicInteger();

    public MobEnchantmentSyncPacket(int entityId, List<MobEnchantment> entries) {
        this.entityId = entityId;
        this.entries = entries;
    }

    public MobEnchantmentSyncPacket(RegistryFriendlyByteBuf buf) {
        this.entityId = buf.readVarInt();
        int size = buf.readVarInt();
        List<MobEnchantment> decoded = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String idStr = buf.readUtf();
            int level = buf.readVarInt();
            int ticks = buf.readVarInt();
            Identifier id = Identifier.tryParse(idStr);
            if (id == null) continue;
            Holder<Enchantment> enchantment = EejEnchantments.holder(id);
            if (enchantment != null) {
                decoded.add(new MobEnchantment(enchantment, level, ticks));
            }
        }
        this.entries = decoded;
    }

    public void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId);
        buf.writeVarInt(this.entries.size());
        for (MobEnchantment entry : this.entries) {
            Identifier id = EejEnchantments.registry().getKey(entry.getEnchantment().value());
            buf.writeUtf(id == null ? "minecraft:sharpness" : id.toString());
            buf.writeVarInt(entry.getLevel());
            buf.writeVarInt(entry.getRemainingTicks());
        }
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
        if (Minecraft.getInstance().level == null) return;
        Entity entity = Minecraft.getInstance().level.getEntity(this.entityId);
        if (entity == null) {
            
            if (PENDING.size() > 256) {
                PENDING.clear();
            }
            PENDING.put(this.entityId, this.entries);
            log("客户端收到魔咒同步：实体 %d 尚未生成，已暂存 %d 条".formatted(this.entityId, this.entries.size()));
            return;
        }
        IMobEnchantments data = MobEnchantmentApi.get(entity);
        if (data != null) {
            data.setAll(this.entries);
        }
        log("客户端收到魔咒同步：实体 %d（%s）%d 条".formatted(this.entityId, entity.getType(), this.entries.size()));
    }

    
    public static boolean applyPending(Entity entity) {
        List<MobEnchantment> pending = PENDING.remove(entity.getId());
        if (pending == null) return false;
        IMobEnchantments data = MobEnchantmentApi.get(entity);
        if (data == null) return false;
        data.setAll(pending);
        log("客户端补挂魔咒同步：实体 %d（%s）%d 条".formatted(entity.getId(), entity.getType(), pending.size()));
        return true;
    }

    private static void log(String message) {
        if (SYNC_LOGGED.incrementAndGet() <= 8) {
            org.tdddd.eej.impl.eej.LOGGER.info("[eej-glint] {}", message);
        }
    }
}