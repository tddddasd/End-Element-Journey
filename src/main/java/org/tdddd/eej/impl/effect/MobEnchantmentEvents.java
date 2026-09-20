package org.tdddd.eej.impl.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.FrostWalkerEnchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantment;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.eej;

import java.util.List;
import java.util.UUID;


@Mod.EventBusSubscriber(modid = eej.MODID)
public class MobEnchantmentEvents {

    private static final UUID SPEED_MODIFIER_ID = UUID.fromString("5f4a1c3e-9b2d-4c67-8e10-7a6d5b9c1f20");
    private static final ThreadLocal<Boolean> THORNS_RECURSION = ThreadLocal.withInitial(() -> Boolean.FALSE);

    

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim) return;
        
        if (source.getDirectEntity() != attacker) return;

        List<MobEnchantment> enchantments = MobEnchantmentApi.getAll(attacker);
        if (enchantments.isEmpty()) return;

        MobType mobType = victim.getMobType();
        float bonus = 0.0F;
        for (MobEnchantment entry : enchantments) {
            Enchantment enchantment = entry.getEnchantment();
            int level = entry.getLevel();
            bonus += enchantment.getDamageBonus(level, mobType, ItemStack.EMPTY);
            if (enchantment == Enchantments.FIRE_ASPECT) {
                victim.setRemainingFireTicks(level * 4 * 20);
            } else if (enchantment == Enchantments.KNOCKBACK) {
                knockback(attacker, victim, level);
            }
            enchantment.doPostAttack(attacker, victim, level);
        }
        if (bonus > 0.0F) {
            event.setAmount(event.getAmount() + bonus);
        }
    }

    private static void knockback(LivingEntity attacker, LivingEntity victim, int level) {
        double yaw = Math.toRadians(attacker.getYRot());
        victim.push(-Math.sin(yaw) * 0.5D * level, 0.1D, Math.cos(yaw) * 0.5D * level);
    }

    
    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        DamageSource source = event.getDamageSource();
        if (source == null || !(source.getEntity() instanceof LivingEntity killer)) return;
        int level = MobEnchantmentApi.getLevel(killer, Enchantments.MOB_LOOTING);
        if (level > event.getLootingLevel()) {
            event.setLootingLevel(level);
        }
    }

    

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        List<MobEnchantment> enchantments = MobEnchantmentApi.getAll(victim);
        if (enchantments.isEmpty()) return;

        DamageSource source = event.getSource();
        int epf = 0;
        for (MobEnchantment entry : enchantments) {
            epf += entry.getEnchantment().getDamageProtection(entry.getLevel(), source);
        }
        if (epf > 0) {
            float factor = 1.0F - Math.min(20, epf) / 25.0F;
            event.setAmount(event.getAmount() * factor);
        }

        
        Entity attacker = source.getEntity();
        if (attacker != null && !THORNS_RECURSION.get()) {
            THORNS_RECURSION.set(Boolean.TRUE);
            try {
                for (MobEnchantment entry : enchantments) {
                    entry.getEnchantment().doPostHurt(victim, attacker, entry.getLevel());
                }
            } finally {
                THORNS_RECURSION.set(Boolean.FALSE);
            }
        }
    }

    

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        IMobEnchantments data = MobEnchantmentApi.get(entity);
        if (data == null || !data.hasAny()) {
            removeSpeedModifier(entity);
            return;
        }

        if (data.tickDown()) {
            MobEnchantmentApi.sync(entity, data);
        }
        if (data.hasAny()) {
            applyPassiveEffects(entity, data);
        } else {
            removeSpeedModifier(entity);
        }
    }

    private static void applyPassiveEffects(LivingEntity entity, IMobEnchantments data) {
        
        if (data.getLevel(Enchantments.RESPIRATION) > 0 && entity.isUnderWater()) {
            entity.setAirSupply(entity.getMaxAirSupply());
        }

        
        int frostWalker = data.getLevel(Enchantments.FROST_WALKER);
        if (frostWalker > 0 && entity.onGround() && !entity.isSpectator() && entity.level() instanceof ServerLevel serverLevel) {
            FrostWalkerEnchantment.onEntityMoved(entity, serverLevel, entity.blockPosition(), frostWalker);
        }

        applySpeedModifier(entity, data);
    }

    private static void applySpeedModifier(LivingEntity entity, IMobEnchantments data) {
        double bonus = 0.0D;
        int depthStrider = data.getLevel(Enchantments.DEPTH_STRIDER);
        if (depthStrider > 0 && entity.isInWater()) {
            bonus += 0.15D * depthStrider;
        }
        int swiftSneak = data.getLevel(Enchantments.SWIFT_SNEAK);
        if (swiftSneak > 0 && entity.isCrouching()) {
            bonus += 0.3D * swiftSneak;
        }
        int soulSpeed = data.getLevel(Enchantments.SOUL_SPEED);
        if (soulSpeed > 0 && isOnSoulBlock(entity)) {
            bonus += 0.3D * soulSpeed;
        }

        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        attribute.removeModifier(SPEED_MODIFIER_ID);
        if (bonus > 0.0D) {
            attribute.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID,
                    "eej_mob_enchantment_speed", bonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(SPEED_MODIFIER_ID) != null) {
            attribute.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static boolean isOnSoulBlock(LivingEntity entity) {
        BlockPos below = entity.blockPosition().below();
        BlockState state = entity.level().getBlockState(below);
        return state.is(Blocks.SOUL_SAND) || state.is(Blocks.SOUL_SOIL);
    }

    

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return;

        List<MobEnchantment> enchantments = MobEnchantmentApi.getAll(shooter);
        if (enchantments.isEmpty()) return;

        for (MobEnchantment entry : enchantments) {
            Enchantment enchantment = entry.getEnchantment();
            int enchantLevel = entry.getLevel();
            if (enchantment == Enchantments.POWER_ARROWS) {
                arrow.setBaseDamage(arrow.getBaseDamage() + enchantLevel * 0.5D + 0.5D);
            } else if (enchantment == Enchantments.PUNCH_ARROWS) {
                arrow.setKnockback(enchantLevel);
            } else if (enchantment == Enchantments.FLAMING_ARROWS) {
                arrow.setRemainingFireTicks(100 * 20);
            }
        }
    }

    

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity living)) return;
        IMobEnchantments data = MobEnchantmentApi.get(living);
        if (data == null || !data.hasAny()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            MobEnchantmentApi.syncToPlayer(player, living, data);
        }
    }
}
