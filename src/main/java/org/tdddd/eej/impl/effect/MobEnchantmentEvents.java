package org.tdddd.eej.impl.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.tdddd.eej.api.mob.IMobEnchantments;
import org.tdddd.eej.api.mob.MobEnchantment;
import org.tdddd.eej.api.mob.MobEnchantmentApi;
import org.tdddd.eej.impl.eej;

import java.util.List;


@EventBusSubscriber(modid = eej.MODID)
public class MobEnchantmentEvents {

    
    private static final net.minecraft.resources.Identifier SPEED_MODIFIER_ID =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(eej.MODID, "mob_enchantment_speed");

    private static final ThreadLocal<Boolean> THORNS_RECURSION = ThreadLocal.withInitial(() -> Boolean.FALSE);

    

    
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == victim) return;
        
        if (source.getDirectEntity() != attacker) return;

        List<MobEnchantment> enchantments = MobEnchantmentApi.getAll(attacker);
        if (enchantments.isEmpty()) return;

        float bonus = 0.0F;
        for (MobEnchantment entry : enchantments) {
            Holder<Enchantment> enchantment = entry.getEnchantment();
            int level = entry.getLevel();

            
            if (is(enchantment, Enchantments.SHARPNESS)) {
                bonus += 0.5F * level + 0.5F;
            } else if (is(enchantment, Enchantments.SMITE) && victim.getType().builtInRegistryHolder().is(EntityTypeTags.UNDEAD)) {
                bonus += 2.5F * level;
            } else if (is(enchantment, Enchantments.BANE_OF_ARTHROPODS) && victim.getType().builtInRegistryHolder().is(EntityTypeTags.ARTHROPOD)) {
                bonus += 2.5F * level;
            }

            if (is(enchantment, Enchantments.FIRE_ASPECT)) {
                victim.setRemainingFireTicks(level * 4 * 20);
            } else if (is(enchantment, Enchantments.KNOCKBACK)) {
                knockback(attacker, victim, level);
            }
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
    public static void onLooting(LivingDropsEvent event) {
        DamageSource source = event.getSource();
        if (source == null || !(source.getEntity() instanceof LivingEntity killer)) return;
        int level = MobEnchantmentApi.getLevel(killer, holder(killer, Enchantments.LOOTING));
        if (level <= 0 || event.getDrops().isEmpty()) return;

        int extra = Math.min(level, 3);
        List<net.minecraft.world.entity.item.ItemEntity> originals = new java.util.ArrayList<>(event.getDrops());
        for (net.minecraft.world.entity.item.ItemEntity drop : originals) {
            for (int i = 0; i < extra; i++) {
                ItemStack copy = drop.getItem().copy();
                if (copy.isEmpty()) break;
                net.minecraft.world.entity.item.ItemEntity extraEntity = new net.minecraft.world.entity.item.ItemEntity(
                        drop.level(), drop.getX(), drop.getY(), drop.getZ(), copy);
                event.getDrops().add(extraEntity);
            }
        }
    }

    

    
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity victim = event.getEntity();
        List<MobEnchantment> enchantments = MobEnchantmentApi.getAll(victim);
        if (enchantments.isEmpty()) return;

        DamageSource source = event.getSource();
        int epf = 0;
        for (MobEnchantment entry : enchantments) {
            epf += protectionPoints(entry.getEnchantment(), entry.getLevel(), source);
        }
        if (epf > 0) {
            float factor = 1.0F - Math.min(20, epf) / 25.0F;
            event.setNewDamage(event.getNewDamage() * factor);
        }

        
        Entity attacker = source.getEntity();
        if (attacker instanceof LivingEntity livingAttacker && !THORNS_RECURSION.get()) {
            int thorns = levelOf(enchantments, victim, Enchantments.THORNS);
            if (thorns > 0 && victim.getRandom().nextFloat() < 0.15F * thorns) {
                THORNS_RECURSION.set(Boolean.TRUE);
                try {
                    if (victim.level() instanceof ServerLevel serverLevel) {
                        livingAttacker.hurtServer(serverLevel, victim.damageSources().thorns(victim), 1.0F + thorns);
                    }
                } finally {
                    THORNS_RECURSION.set(Boolean.FALSE);
                }
            }
        }
    }

    
    private static int protectionPoints(Holder<Enchantment> enchantment, int level, DamageSource source) {
        if (is(enchantment, Enchantments.PROTECTION)) return level;
        if (is(enchantment, Enchantments.FIRE_PROTECTION) && source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) return 2 * level;
        if (is(enchantment, Enchantments.BLAST_PROTECTION) && source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) return 2 * level;
        if (is(enchantment, Enchantments.PROJECTILE_PROTECTION) && source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) return 2 * level;
        if (is(enchantment, Enchantments.FEATHER_FALLING) && source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) return 3 * level;
        return 0;
    }

    

    
    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
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
        
        if (data.getLevel(holder(entity, Enchantments.RESPIRATION)) > 0 && entity.isUnderWater()) {
            entity.setAirSupply(entity.getMaxAirSupply());
        }

        
        

        applySpeedModifier(entity, data);
    }

    private static void applySpeedModifier(LivingEntity entity, IMobEnchantments data) {
        double bonus = 0.0D;
        int depthStrider = data.getLevel(holder(entity, Enchantments.DEPTH_STRIDER));
        if (depthStrider > 0 && entity.isInWater()) {
            bonus += 0.15D * depthStrider;
        }
        int swiftSneak = data.getLevel(holder(entity, Enchantments.SWIFT_SNEAK));
        if (swiftSneak > 0 && entity.isCrouching()) {
            bonus += 0.3D * swiftSneak;
        }
        int soulSpeed = data.getLevel(holder(entity, Enchantments.SOUL_SPEED));
        if (soulSpeed > 0 && isOnSoulBlock(entity)) {
            bonus += 0.3D * soulSpeed;
        }

        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) return;
        attribute.removeModifier(SPEED_MODIFIER_ID);
        if (bonus > 0.0D) {
            // 26.1.2: AttributeModifier(Identifier id, double amount, Operation operation)
            attribute.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_ID,
                    bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void removeSpeedModifier(LivingEntity entity) {
        AttributeInstance attribute = entity.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null && attribute.getModifier(SPEED_MODIFIER_ID) != null) {
            attribute.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static boolean isOnSoulBlock(LivingEntity entity) {
        net.minecraft.core.BlockPos below = entity.blockPosition().below();
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
            Holder<Enchantment> enchantment = entry.getEnchantment();
            int enchantLevel = entry.getLevel();
            if (is(enchantment, Enchantments.POWER)) {
                
                
                arrow.setBaseDamage(arrowDamageBonus(arrow) + enchantLevel * 0.5D + 0.5D);
            } else if (is(enchantment, Enchantments.FLAME)) {
                arrow.setRemainingFireTicks(100 * 20);
            }
            
            
        }
    }

    
    private static double arrowDamageBonus(AbstractArrow arrow) {
        return 0.0D;
    }

    

    private static final java.util.concurrent.atomic.AtomicInteger START_TRACK_LOGGED = new java.util.concurrent.atomic.AtomicInteger();

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getTarget() instanceof LivingEntity living)) return;
        IMobEnchantments data = MobEnchantmentApi.get(living);
        if (data == null || !data.hasAny()) return;
        if (event.getEntity() instanceof ServerPlayer player) {
            MobEnchantmentApi.syncToPlayer(player, living, data);
            if (START_TRACK_LOGGED.incrementAndGet() <= 8) {
                org.tdddd.eej.impl.eej.LOGGER.info("[eej-glint] 玩家开始跟踪带魔咒的生物 {}，已同步 {} 条",
                        living.getType(), data.getAll().size());
            }
        }
    }

    

    
    private static boolean is(Holder<Enchantment> holder, ResourceKey<Enchantment> key) {
        return holder != null && holder.unwrapKey().map(key::equals).orElse(false);
    }

    
    private static Holder<Enchantment> holder(LivingEntity entity, ResourceKey<Enchantment> key) {
        return org.tdddd.eej.impl.registry.EejEnchantments.holder(key);
    }

    private static int levelOf(List<MobEnchantment> entries, LivingEntity entity, ResourceKey<Enchantment> key) {
        for (MobEnchantment entry : entries) {
            if (is(entry.getEnchantment(), key)) return entry.getLevel();
        }
        return 0;
    }
}
