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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejRecipes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Soul fire purification mechanic (灵魂火净化).
 *
 * <p>Every {@value #PROCESS_INTERVAL_TICKS} ticks each server level is scanned for item entities
 * standing inside {@code minecraft:soul_fire}. Every <em>single item</em> of such a stack rolls its
 * own recipe, and that recipe's chance is the chance that <b>one purification attempt</b> yields
 * something: a successful roll spawns the produced stacks, a failed roll burns the item away. The
 * item is consumed either way, so the configured chance is exactly the chance the player gets.
 *
 * <p>Vanilla fire destroys item entities: {@code BaseFireBlock.entityInside} hurts the entity every
 * tick (2 damage in soul fire against an item's 5 health), so an unprotected item dropped into soul
 * fire is gone within three ticks - before the 10-tick scan, and so before the roll that is supposed
 * to decide its fate. Every item the mechanic still needs is therefore kept invulnerable while it
 * sits in fire (see {@link #updateFireProtection}): explosive recipes waiting for their detonation,
 * purification inputs waiting for the scan, and freshly purified drops during their
 * {@link #IMMUNITY_TICKS}-tick immunity window.
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
    /** Entity persistent-data flag marking a drop that came out of purification. */
    public static final String TAG_PURIFIED = "eej_soul_fire_purified";
    /** Entity persistent-data key holding the game time until which a fresh drop ignores fire and lava. */
    public static final String TAG_FIRE_IMMUNE_UNTIL = "eej_soul_fire_immune_until";
    /** Damage multiplier of the extra custom damage sweep (falls off linearly with distance). */
    private static final double EXPLOSION_DAMAGE_SCALE = 14.0D;
    /** Persistent-data key marking an item entity whose invulnerability was set by this handler. */
    private static final String TAG_PROTECTED = "eej_soul_fire_protected";

    /**
     * Lookup table from input item to recipe, cached per {@link RecipeManager} instance. A datapack
     * reload builds a fresh manager, which invalidates the entry automatically.
     */
    private static final Map<RecipeManager, Map<Item, SoulFirePurificationRecipe>> RECIPE_INDEX =
            Collections.synchronizedMap(new WeakHashMap<>());

    private SoulFirePurificationHandler() {
    }

    private static Map<Item, SoulFirePurificationRecipe> recipeIndex(RecipeManager manager) {
        return RECIPE_INDEX.computeIfAbsent(manager, SoulFirePurificationHandler::buildRecipeIndex);
    }

    private static Map<Item, SoulFirePurificationRecipe> buildRecipeIndex(RecipeManager manager) {
        List<SoulFirePurificationRecipe> recipes =
                manager.getAllRecipesFor(EejRecipes.SOUL_FIRE_PURIFICATION_TYPE.get());
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
        return Map.copyOf(byItem);
    }

    // ---------------------------------------------------------------- fire immunity

    /**
     * Per-tick item entity sweep for fire protection: it expires the 30 s immunity of purified drops
     * and runs {@link #updateFireProtection}, which keeps inputs, explosives and fresh drops alive
     * against the fire block's per-tick damage.
     *
     * <p>Everything is stored in the <b>entity's</b> persistent data, never on the stack: a purified
     * drop is therefore a completely ordinary item, so it merges and stacks with the player's existing
     * items (any stack tag or data component would make {@code ItemStack#isSameItemSameTags} fail and
     * leave the player with separate, non-merging slots).</p>
     */
    @SubscribeEvent
    public static void onServerTickFireProtection(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (ServerLevel level : event.getServer().getAllLevels()) {
            long gameTime = level.getGameTime();
            Map<Item, SoulFirePurificationRecipe> byItem = recipeIndex(level.getRecipeManager());
            for (Entity entity : level.getEntities().getAll()) {
                if (entity instanceof ItemEntity itemEntity) {
                    expireImmunity(itemEntity, gameTime);
                    updateFireProtection(level, itemEntity, byItem, gameTime);
                }
            }
        }
    }

    /**
     * Keeps an item entity alive while the mechanic still needs it and gives it back to vanilla
     * otherwise.
     *
     * <p>Vanilla fire hurts an item entity every tick it overlaps the fire block (2 damage per tick
     * in soul fire against 5 health), which would destroy the input before it could be rolled or
     * detonated. The entity is made invulnerable for as long as it is inside fire <em>and</em> is one
     * of ours; the flag lives in the entity's persistent data, so only an invulnerability set here is
     * ever cleared again.</p>
     *
     * <p>Called for every item entity every tick, so the common case (an item with no recipe and no
     * immunity marker) must not touch the level: it only picks up a flag left behind earlier.</p>
     */
    private static void updateFireProtection(Level level, ItemEntity entity,
                                             Map<Item, SoulFirePurificationRecipe> byItem, long gameTime) {
        ItemStack stack = entity.getItem();
        SoulFirePurificationRecipe recipe = stack.isEmpty() ? null : byItem.get(stack.getItem());
        boolean marked = isImmune(entity, gameTime);
        if (recipe == null && !marked) {
            if (entity.isInvulnerable()) {
                setProtected(entity, false);
            }
            return;
        }

        boolean inSoulFire = isInSoulFire(level, entity);
        boolean inFire = inSoulFire || isInFire(level, entity);
        boolean protect = false;
        if (inFire && recipe != null) {
            if (recipe.isExplosive()) {
                // Must survive long enough to reach its own detonation.
                protect = true;
            } else if (inSoulFire) {
                // Waits in the soul fire for the scan that rolls it.
                protect = true;
            }
        }
        if (!protect && inFire && marked) {
            // A freshly purified drop during its immunity window.
            protect = true;
        }
        setProtected(entity, protect);
    }

    /** Applies (or clears) this handler's own invulnerability flag on the entity. */
    private static void setProtected(ItemEntity entity, boolean protect) {
        if (protect) {
            if (!entity.isInvulnerable()) {
                entity.setInvulnerable(true);
            }
            if (entity.getRemainingFireTicks() > 0) {
                entity.clearFire();
            }
            entity.getPersistentData().putBoolean(TAG_PROTECTED, true);
        } else if (entity.getPersistentData().getBoolean(TAG_PROTECTED)) {
            entity.setInvulnerable(false);
            entity.getPersistentData().remove(TAG_PROTECTED);
        }
    }

    /** Marks a freshly spawned drop: immune to fire for {@link #IMMUNITY_TICKS} and never re-rolled. */
    public static void markPurified(ItemEntity entity) {
        markPurified(entity, entity.level().getGameTime());
    }

    /**
     * Marks a freshly spawned drop using an explicit server game time. The markers live on the entity,
     * so picking the drop up ends them and leaves the player with a plain, stackable item.
     */
    public static void markPurified(ItemEntity entity, long gameTime) {
        CompoundTag data = entity.getPersistentData();
        data.putBoolean(TAG_PURIFIED, true);
        data.putLong(TAG_FIRE_IMMUNE_UNTIL, gameTime + IMMUNITY_TICKS);
        // Immune from its very first tick: the drop spawns inside the soul fire that just consumed the
        // input, and the sweep only runs at the end of the server tick.
        setProtected(entity, true);
    }

    /** True while the entity still sits inside its fire immunity window. */
    public static boolean isImmune(ItemEntity entity, long gameTime) {
        long until = entity.getPersistentData().getLong(TAG_FIRE_IMMUNE_UNTIL);
        return until > 0L && until >= gameTime;
    }

    /** Drops the immunity marker of an entity whose window has passed. */
    private static void expireImmunity(ItemEntity entity, long gameTime) {
        CompoundTag data = entity.getPersistentData();
        long until = data.getLong(TAG_FIRE_IMMUNE_UNTIL);
        if (until > 0L && until < gameTime) {
            data.remove(TAG_FIRE_IMMUNE_UNTIL);
        }
    }

    /** True when this item entity came out of purification and must not be rolled again. */
    private static boolean isPurifiedDropped(ItemEntity entity) {
        return entity.getPersistentData().getBoolean(TAG_PURIFIED);
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
        Map<Item, SoulFirePurificationRecipe> byItem = recipeIndex(level.getRecipeManager());
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
            if (isPurifiedDropped(entity)) {
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
        // Every single item of the stack rolls on its own, and the recipe's chance is the chance that ONE
        // purification attempt yields something: the item is consumed either way, producing its drops on a
        // success and simply burning away on a failure. The fire cannot take the item first (see
        // updateFireProtection), so the configured chance is always the chance the player gets.
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

    /** Convenience for callers that already work in world coordinates. */
    public static boolean isInSoulFireAt(Level level, Vec3 position) {
        return level.getBlockState(BlockPos.containing(position)).is(Blocks.SOUL_FIRE);
    }
}
