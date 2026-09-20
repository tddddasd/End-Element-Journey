package org.tdddd.eej.api.mob;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.network.PacketDistributor;
import org.tdddd.eej.impl.capability.EejCapabilities;
import org.tdddd.eej.impl.network.EejNetwork;
import org.tdddd.eej.impl.network.MobEnchantmentSyncPacket;

import java.util.List;


public final class MobEnchantmentApi {

    private MobEnchantmentApi() {
    }

    
    public static IMobEnchantments get(Entity entity) {
        if (entity == null) return null;
        return entity.getCapability(EejCapabilities.MOB_ENCHANTMENTS).orElse(null);
    }

    public static List<MobEnchantment> getAll(Entity entity) {
        IMobEnchantments data = get(entity);
        return data == null ? List.of() : data.getAll();
    }

    public static boolean hasAny(Entity entity) {
        IMobEnchantments data = get(entity);
        return data != null && data.hasAny();
    }

    public static int getLevel(Entity entity, Enchantment enchantment) {
        IMobEnchantments data = get(entity);
        return data == null ? 0 : data.getLevel(enchantment);
    }

    
    public static boolean apply(LivingEntity entity, Enchantment enchantment, int level, int durationTicks) {
        IMobEnchantments data = get(entity);
        if (data == null) return false;
        if (!data.apply(enchantment, level, durationTicks)) return false;
        sync(entity, data);
        return true;
    }

    public static boolean remove(LivingEntity entity, Enchantment enchantment) {
        IMobEnchantments data = get(entity);
        if (data == null) return false;
        if (!data.remove(enchantment)) return false;
        sync(entity, data);
        return true;
    }

    public static boolean clear(LivingEntity entity) {
        IMobEnchantments data = get(entity);
        if (data == null) return false;
        if (!data.clear()) return false;
        sync(entity, data);
        return true;
    }

    
    public static void sync(LivingEntity entity, IMobEnchantments data) {
        if (!(entity.level() instanceof ServerLevel)) return;
        if (data == null) return;
        EejNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity),
                new MobEnchantmentSyncPacket(entity.getId(), data.getAll()));
    }

    
    public static void syncToPlayer(ServerPlayer player, LivingEntity entity, IMobEnchantments data) {
        if (data == null) return;
        EejNetwork.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new MobEnchantmentSyncPacket(entity.getId(), data.getAll()));
    }
}
