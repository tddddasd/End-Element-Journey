package org.tdddd.eej.impl.recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejRecipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Soul fire purification mechanic (灵魂火净化).
 *
 * <p>Every {@value #PROCESS_INTERVAL_TICKS} ticks each server level is scanned for item entities
 * standing inside {@code minecraft:soul_fire}. Every <em>single item</em> of such a stack rolls its
 * own recipe: a successful roll spawns the produced stacks, a failed roll consumes the item
 * (burned). Items produced by purification carry the {@link #TAG_PURIFIED} flag and are immune to
 * fire and lava for {@link #IMMUNITY_TICKS} ticks; the deadline lives inside the {@link ItemStack}
 * NBT so the timer survives picking the stack up and dropping it again.
 *
 * <p>Items whose recipe carries an {@code explode} block are never rolled: they detonate as soon as
 * they touch {@code minecraft:fire} or {@code minecraft:soul_fire}. Soul fire additionally blinds
 * every living entity inside the blast radius for the configured duration.
 */
@Mod.EventBusSubscriber(modid = eej.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SoulFirePurificationHandler {
    /** How often the soul fire scan runs, in ticks. */
    public static final int PROCESS_INTERVAL_TICKS = 10;
    /** Fire / lava immunity granted to freshly purified drops, in ticks. */
    public static final int IMMUNITY_TICKS = 600;
    /** NBT flag marking a stack that came out of purification. */
    public static final String TAG_PURIFIED = "eej_soul_fire_purified";
    /** NBT flag holding the server game time until which the stack ignores fire and lava. */
    public static final String TAG_FIRE_IMMUNE_UNTIL = "eej_soul_fire_immune_until";
    /** Damage multiplier of the extra custom damage sweep (falls off linearly with distance). */
    private static final double EXPLOSION_DAMAGE_SCALE = 14.0D;

    private SoulFirePurificationHandler() {
    }

    // ---------------------------------------------------------------- fire immunity

    /**
     * Applies the fire immunity of purified stacks: the remaining fire ticks are pinned to zero, so
     * neither {@code minecraft:on_fire} damage nor {@code Entity#lavaHurt()} can destroy the stack.
     * The marker is dropped once the deadline has passed.
     */
    @SubscribeEvent
    public static void onServerTickFireImmunity(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            long gameTime = level.getGameTime();
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof ItemEntity itemEntity) {
                    applyFireImmunity(itemEntity, gameTime);
                }
            }
        }
    }

    /**
     * Applies the immunity as soon as an anointed stack rejoins a level, e.g. right after a player
     * dropped it again.
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (event.getEntity() instanceof ItemEntity itemEntity) {
            applyFireImmunity(itemEntity, serverLevel.getGameTime());
        }
    }

    private static void applyFireImmunity(ItemEntity entity, long gameTime) {
        ItemStack stack = entity.getItem();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_FIRE_IMMUNE_UNTIL)) {
            return;
        }
        if (tag.getLong(TAG_FIRE_IMMUNE_UNTIL) >= gameTime) {
            if (entity.getRemainingFireTicks() > 0) {
                entity.clearFire();
            }
        } else {
            tag.remove(TAG_PURIFIED);
            tag.remove(TAG_FIRE_IMMUNE_UNTIL);
        }
    }

    /** Marks a freshly spawned drop: immune to fire for {@link #IMMUNITY_TICKS} and never re-rolled. */
    public static void markPurified(ItemEntity entity) {
        markPurified(entity, entity.level().getGameTime());
    }

    /** Marks a freshly spawned drop using an explicit server game time. */
    public static void markPurified(ItemEntity entity, long gameTime) {
        CompoundTag tag = entity.getItem().getOrCreateTag();
        tag.putBoolean(TAG_PURIFIED, true);
        tag.putLong(TAG_FIRE_IMMUNE_UNTIL, gameTime + IMMUNITY_TICKS);
        entity.clearFire();
    }

    // ---------------------------------------------------------------- purification

    /** Scans soul fire and processes one item entity at a time. */
    @SubscribeEvent
    public static void onServerTickPurification(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.getServer().getTickCount() % PROCESS_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            processLevel(level);
        }
    }

    private static void processLevel(ServerLevel level) {
        RecipeManager recipeManager = level.getRecipeManager();
        List<SoulFirePurificationRecipe> recipes =
                recipeManager.getAllRecipesFor(EejRecipes.SOUL_FIRE_PURIFICATION_TYPE.get());
        if (recipes.isEmpty()) {
            return;
        }
        Map<Item, SoulFirePurificationRecipe> byItem = new HashMap<>();
        for (SoulFirePurificationRecipe recipe : recipes) {
            ResourceLocation itemId = recipe.ingredientItemId();
            if (itemId == null) {
                continue;
            }
            Item item = ForgeRegistries.ITEMS.getValue(itemId);
            if (item != null) {
                byItem.putIfAbsent(item, recipe);
            }
        }
        if (byItem.isEmpty()) {
            return;
        }

        // Snapshot first: processing spawns entities and detonates explosions.
        List<ItemEntity> candidates = new ArrayList<>();
        for (Entity entity : level.getEntities().getAll()) {
            if (entity instanceof ItemEntity itemEntity && itemEntity.isAlive()) {
                candidates.add(itemEntity);
            }
        }
        for (ItemEntity entity : candidates) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty() || byItem.get(stack.getItem()) == null) {
                continue;
            }
            if (stack.hasTag() && stack.getTag() != null && stack.getTag().getBoolean(TAG_PURIFIED)) {
                continue;
            }
            processEntity(level, entity, byItem);
        }
    }

    private static void processEntity(ServerLevel level, ItemEntity entity, Map<Item, SoulFirePurificationRecipe> byItem) {
        boolean inSoulFire = isInSoulFire(level, entity);
        boolean inAnyFire = inSoulFire || isInFire(level, entity);
        SoulFirePurificationRecipe recipe = byItem.get(entity.getItem().getItem());
        if (recipe == null) {
            return;
        }

        if (recipe.isExplosive()) {
            if (!inAnyFire) {
                return;
            }
            double x = entity.getX();
            double y = entity.getY();
            double z = entity.getZ();
            entity.discard();
            recipe.explosion().ifPresent(spec -> explode(level, null, x, y, z, spec, inSoulFire));
            return;
        }

        if (!inSoulFire) {
            return;
        }

        if (recipe.isDestroyOnly()) {
            // Explicit data driven "no roll, simply destroyed" case (epca:infested_flesh).
            consumeOne(entity);
            return;
        }

        RandomSource random = level.getRandom();
        // Every single item of the stack rolls independently.
        int rolls = entity.getItem().getCount();
        for (int i = 0; i < rolls; i++) {
            if (entity.getItem().isEmpty()) {
                break;
            }
            double x = entity.getX() + (random.nextDouble() - 0.5D) * 0.4D;
            double y = entity.getY() + 0.1D;
            double z = entity.getZ() + (random.nextDouble() - 0.5D) * 0.4D;
            recipe.rollAndSpawn(level, x, y, z, random);
            consumeOne(entity);
        }
    }

    /** Removes exactly one item from the entity, discarding the entity when it becomes empty. */
    private static void consumeOne(ItemEntity entity) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            entity.discard();
            return;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            entity.discard();
        } else {
            entity.setItem(stack);
        }
    }

    // ---------------------------------------------------------------- fire detection

    /** True when any block overlapped by the entity bounding box is soul fire. */
    public static boolean isInSoulFire(Level level, Entity entity) {
        return isInBlock(level, entity, Blocks.SOUL_FIRE);
    }

    /** True when any block overlapped by the entity bounding box is ordinary fire. */
    public static boolean isInFire(Level level, Entity entity) {
        return isInBlock(level, entity, Blocks.FIRE);
    }

    private static boolean isInBlock(Level level, Entity entity, Block block) {
        AABB box = entity.getBoundingBox().deflate(1.0E-3D);
        int minX = Mth.floor(box.minX);
        int maxX = Mth.floor(box.maxX);
        int minY = Mth.floor(box.minY);
        int maxY = Mth.floor(box.maxY);
        int minZ = Mth.floor(box.minZ);
        int maxZ = Mth.floor(box.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = level.getBlockState(cursor.set(x, y, z));
                    if (state.is(block)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- explosion

    /**
     * Detonates the configured explosion and - when triggered by soul fire - blinds every living
     * entity inside the radius.
     *
     * @param source living entity credited with the explosion, may be null
     */
    public static void explode(Level level, LivingEntity source, double x, double y, double z,
                               SoulFirePurificationRecipe.ExplosionSpec spec, boolean soulFire) {
        level.explode(source, x, y, z, spec.power(), spec.fire(), Level.ExplosionInteraction.TNT);

        double radius = spec.radius();
        AABB area = new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (!entity.isAlive()) {
                continue;
            }
            double distance = Math.sqrt(entity.distanceToSqr(x, y, z));
            if (distance > radius) {
                continue;
            }
            double falloff = 1.0D - distance / radius;
            if (soulFire && spec.blindnessTicks() > 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, spec.blindnessTicks(), 0));
            }
            if (entity instanceof Player player && player.isCreative()) {
                continue;
            }
            float damage = (float) (EXPLOSION_DAMAGE_SCALE * falloff);
            if (damage > 0.0F) {
                entity.hurt(level.damageSources().explosion(source, source), damage);
                applyKnockback(entity, x, y, z, distance, radius);
            }
        }
    }

    /** Same knockback shape as epca's coal ore explosion, kept identical for consistency. */
    private static void applyKnockback(LivingEntity entity, double x, double y, double z,
                                       double distance, double maxRadius) {
        double dx = entity.getX() - x;
        double dz = entity.getZ() - z;
        double magnitude = Math.sqrt(dx * dx + dz * dz);
        if (magnitude > 0.0D) {
            dx /= magnitude;
            dz /= magnitude;
            double strength = (1.0D - distance / maxRadius) * 0.5D;
            entity.setDeltaMovement(entity.getDeltaMovement().add(dx * strength, 0.3D, dz * strength));
        }
    }

    /** True while the given stack still sits inside its fire immunity window. */
    public static boolean isImmune(ItemStack stack, long gameTime) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG_FIRE_IMMUNE_UNTIL)
                && tag.getLong(TAG_FIRE_IMMUNE_UNTIL) >= gameTime;
    }

    /** Convenience for callers that already work in world coordinates. */
    public static boolean isInSoulFireAt(Level level, Vec3 position) {
        return level.getBlockState(BlockPos.containing(position)).is(Blocks.SOUL_FIRE);
    }
}
