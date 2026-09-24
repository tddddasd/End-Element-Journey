package org.tdddd.eej.impl.soulfire;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The soul fire purification mechanic.
 *
 * <p>An item entity that overlaps {@code minecraft:soul_fire} is looked up in the loaded
 * {@code eej:soul_fire_purification} recipes; each recipe entry rolls on its own, so a single stack produces
 * exactly one result set for the whole stack (per the specification "each item rolls independently" is
 * implemented per item <em>entity</em>: one roll when the entity touches the fire, then the entity is consumed
 * either way). Every produced stack is marked fire immune for 30 s.
 *
 * <p>An item entity that overlaps {@code minecraft:fire} only triggers the recipe's optional {@code explode}
 * block. Soul fire does the same and additionally applies Blindness I inside the explosion radius.
 *
 * <p>Recipes are indexed by item and the index is cached per {@link RecipeManager} instance in a
 * {@link WeakHashMap}, so a datapack reload (which builds a fresh recipe manager) invalidates it automatically.
 */
public final class SoulFirePurificationManager {
    /** Number of soul particles spawned on a successful purification (soul speed look and feel). */
    private static final int SOUL_PARTICLE_COUNT = 12;
    private static final double SOUL_PARTICLE_SPREAD = 0.25D;
    private static final double SOUL_PARTICLE_SPEED = 0.02D;
    /** Volume/pitch taken from the vanilla soul speed enchantment effect (0.6, uniform 0.6..1.0). */
    private static final float SOUL_ESCAPE_VOLUME = 0.6F;
    private static final float SOUL_ESCAPE_PITCH_MIN = 0.6F;
    private static final float SOUL_ESCAPE_PITCH_SPAN = 0.4F;

    private static final Map<RecipeManager, RecipeIndex> INDEX_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SoulFirePurificationManager() {
    }

    /** Per recipe manager lookup table from input item to purification recipe. */
    private record RecipeIndex(Map<Item, SoulFirePurificationRecipe> byItem) {
        static RecipeIndex build(RecipeManager manager) {
            Map<Item, SoulFirePurificationRecipe> byItem = new HashMap<>();
            Collection<RecipeHolder<?>> recipes = manager.getRecipes();
            for (RecipeHolder<?> holder : recipes) {
                if (!(holder.value() instanceof SoulFirePurificationRecipe recipe)) {
                    continue;
                }
                recipe.ingredient().items().forEach(item -> byItem.putIfAbsent(item.value(), recipe));
            }
            return new RecipeIndex(byItem);
        }

        SoulFirePurificationRecipe find(ItemStack stack) {
            return stack.isEmpty() ? null : byItem.get(stack.getItem());
        }
    }

    private static RecipeIndex index(RecipeManager manager) {
        return INDEX_CACHE.computeIfAbsent(manager, RecipeIndex::build);
    }

    /**
     * Handles one item entity for one tick. Called from the entity tick hook, so it must stay cheap for the
     * common case (an item entity that is not inside fire at all).
     */
    public static void handleItemEntity(ServerLevel level, ItemEntity itemEntity) {
        if (itemEntity.isRemoved()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        // 1. Drop the fire immunity marker once its 30 s window has elapsed. The stack is replaced with a
        //    modified copy so the synched entity data notices the change.
        if (SoulFireImmunity.isExpired(level, stack)) {
            ItemStack refreshed = stack.copy();
            SoulFireImmunity.clear(refreshed);
            itemEntity.setItem(refreshed);
            stack = refreshed;
        }

        // 2. Fire / soul fire processing.
        SoulFirePurificationRecipe recipe = index(level.recipeAccess()).find(stack);
        boolean soulFire = isInside(level, itemEntity, Blocks.SOUL_FIRE);
        if (recipe == null) {
            // Data driven "soul fire simply consumes this" list (for example infested flesh).
            if (soulFire && stack.typeHolder().is(SoulFireTags.SOUL_FIRE_CONSUMED)) {
                itemEntity.discard();
            }
            return;
        }

        boolean inFire = soulFire || isInside(level, itemEntity, Blocks.FIRE);
        if (!inFire) {
            return;
        }

        if (recipe.explosion().isPresent()) {
            detonate(level, itemEntity, recipe.explosion().get(), soulFire);
            itemEntity.discard();
            return;
        }

        // Purification itself only happens in soul fire.
        if (!soulFire) {
            return;
        }
        purify(level, itemEntity, recipe);
        itemEntity.discard();
    }

    /** Removes expired fire immunity markers from a player's inventory (called every 20 ticks). */
    public static void expireInventory(ServerLevel level, Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !SoulFireImmunity.isExpired(level, stack)) {
                continue;
            }
            ItemStack refreshed = stack.copy();
            SoulFireImmunity.clear(refreshed);
            inventory.setItem(slot, refreshed);
        }
    }

    private static void purify(ServerLevel level, ItemEntity itemEntity, SoulFirePurificationRecipe recipe) {
        boolean produced = false;
        for (SoulFireOutput output : recipe.results()) {
            if (level.getRandom().nextFloat() >= output.chance()) {
                continue;
            }
            SoulFireFixedDrop drop = output.pick(level.getRandom());
            if (drop == null) {
                continue;
            }
            spawnPurified(level, itemEntity, drop.createStack(level.getRandom()));
            produced = true;
        }
        if (produced) {
            playSoulFireFeedback(level, itemEntity);
        }
    }

    private static void spawnPurified(ServerLevel level, ItemEntity source, ItemStack stack) {
        SoulFireImmunity.mark(level, stack);
        ItemEntity created = new ItemEntity(level, source.getX(), source.getY() + 0.25D, source.getZ(), stack);
        level.addFreshEntity(created);
    }

    /**
     * The vanilla soul speed feedback: {@code ParticleTypes.SOUL} and {@code SoundEvents.SOUL_ESCAPE}
     * (see {@code net.minecraft.world.item.enchantment.Enchantments}, the soul speed TICK effects, which use
     * those two with volume 0.6 and a pitch uniform in 0.6..1.0).
     */
    private static void playSoulFireFeedback(ServerLevel level, Entity at) {
        double x = at.getX();
        double y = at.getY() + 0.2D;
        double z = at.getZ();
        level.sendParticles(ParticleTypes.SOUL, x, y, z, SOUL_PARTICLE_COUNT,
                SOUL_PARTICLE_SPREAD, SOUL_PARTICLE_SPREAD, SOUL_PARTICLE_SPREAD, SOUL_PARTICLE_SPEED);
        float pitch = SOUL_ESCAPE_PITCH_MIN + level.getRandom().nextFloat() * SOUL_ESCAPE_PITCH_SPAN;
        level.playSound(null, x, y, z, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS,
                SOUL_ESCAPE_VOLUME, pitch);
    }

    /**
     * Mirrors the block-form explosion that the infested coal ores already perform next to fire, so the item
     * form behaves identically: a vanilla explosion plus an extra linear-falloff damage/knockback pass, and
     * Blindness I inside the radius when the trigger block is soul fire.
     */
    private static void detonate(ServerLevel level, ItemEntity itemEntity, SoulFireExplosion explosion,
                                 boolean soulFire) {
        double x = itemEntity.getX();
        double y = itemEntity.getY() + 0.5D;
        double z = itemEntity.getZ();
        level.explode(null, x, y, z, explosion.power(), Level.ExplosionInteraction.TNT);

        double radius = explosion.radius();
        if (radius <= 0.0D) {
            return;
        }
        AABB area = new AABB(BlockPos.containing(x, y, z)).inflate(radius);
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!living.isAlive()) {
                continue;
            }
            double distance = Math.sqrt(living.distanceToSqr(x, y, z));
            if (distance > radius) {
                continue;
            }
            if (soulFire && explosion.blindnessTicks() > 0) {
                living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, explosion.blindnessTicks(), 0), null);
            }
            float damage = (float) (explosion.damage() * (1.0D - distance / radius));
            if (damage > 0.0F) {
                living.hurt(level.damageSources().explosion(null, null), damage);
                applyKnockback(living, x, z, distance, radius, explosion.knockback());
            }
        }
    }

    private static void applyKnockback(LivingEntity entity, double x, double z, double distance, double radius,
                                       double strength) {
        double dx = entity.getX() - x;
        double dz = entity.getZ() - z;
        double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (magnitude <= 0.0D) {
            return;
        }
        double scale = (1.0D - distance / radius) * strength;
        entity.setDeltaMovement(entity.getDeltaMovement()
                .add(dx / magnitude * scale, 0.3D, dz / magnitude * scale));
    }

    /** True when the entity's bounding box overlaps any block of the given type. */
    private static boolean isInside(Level level, Entity entity, Block block) {
        AABB box = entity.getBoundingBox().inflate(0.05D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockState(pos).getBlock() == block) {
                return true;
            }
        }
        return false;
    }
}
