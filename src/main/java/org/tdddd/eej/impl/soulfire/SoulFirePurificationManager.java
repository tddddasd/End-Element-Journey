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
 * {@code eej:soul_fire_purification} recipes. Every single item of the stack rolls on its own: a successful roll
 * spawns that item's drops and consumes it, a failed roll leaves the item where it is and it is rolled again on
 * the shared {@value #PROCESS_INTERVAL_TICKS}-tick cadence until it produces something. Nothing is ever burned
 * away by the mechanic, so a stack only shrinks by the items that actually yielded something.
 *
 * <p>An item entity that overlaps {@code minecraft:fire} only triggers the recipe's optional {@code explode}
 * block. Soul fire does the same and additionally applies Blindness I inside the explosion radius.
 *
 * <h2>Fire protection</h2>
 * Vanilla fire destroys item entities: {@code BaseFireBlock.entityInside} calls
 * {@code entity.hurt(damageSources().inFire(), fireDamage)} <b>every tick</b> (soul fire uses 2.0, normal fire
 * 1.0) and an item entity only has 5 health, so an unprotected item dropped into soul fire is gone in three
 * ticks - long before a purification roll or a detonation could happen. Every item the mechanic still needs is
 * therefore made invulnerable while it sits in fire ({@link #updateProtection}):
 * <ul>
 *   <li>an explosive recipe (infested coal and the coal ores) waiting for its detonation,</li>
 *   <li>a purification input waiting in soul fire for a successful roll,</li>
 *   <li>a freshly purified drop during its 30 s immunity window.</li>
 * </ul>
 * The flag is recorded in the entity's persistent data, so the manager only ever clears an invulnerability it
 * set itself and an item that leaves the fire (or whose window expires) goes back to normal.
 *
 * <p>Recipes are indexed by item and the index is cached per {@link RecipeManager} instance in a
 * {@link WeakHashMap}, so a datapack reload (which builds a fresh recipe manager) invalidates it automatically.
 */
public final class SoulFirePurificationManager {
    /** Ticks between two purification rolls, matching the 1.20.1 scan interval. */
    public static final int PROCESS_INTERVAL_TICKS = 10;

    /** Persistent-data key marking an item entity whose invulnerability was set by this manager. */
    private static final String PROTECTED_KEY = "eej_soul_fire_protected";

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
        boolean inFire = soulFire || isInside(level, itemEntity, Blocks.FIRE);

        // 3. Keep everything the mechanic still needs alive inside the flames.
        updateProtection(itemEntity, stack, recipe, soulFire, inFire);

        if (recipe == null) {
            // Data driven "soul fire simply consumes this" list (for example infested flesh).
            if (soulFire && stack.typeHolder().is(SoulFireTags.SOUL_FIRE_CONSUMED)) {
                itemEntity.discard();
            }
            return;
        }

        if (!inFire) {
            return;
        }

        if (recipe.explosion().isPresent()) {
            detonate(level, itemEntity, recipe.explosion().get(), soulFire);
            itemEntity.discard();
            return;
        }

        // Purification itself only happens in soul fire, on the shared 10-tick cadence.
        if (!soulFire || level.getGameTime() % PROCESS_INTERVAL_TICKS != 0L) {
            return;
        }
        purify(level, itemEntity, recipe);
    }

    /**
     * Fire protection only, called from the pre-tick hook so that it runs before
     * {@code BaseFireBlock.entityInside} hurts the item during its own tick. See {@link #updateProtection}.
     */
    public static void protectItemEntity(ServerLevel level, ItemEntity itemEntity) {
        if (itemEntity.isRemoved()) {
            return;
        }
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) {
            return;
        }
        SoulFirePurificationRecipe recipe = index(level.recipeAccess()).find(stack);
        if (recipe == null && !SoulFireImmunity.isMarked(stack)) {
            if (itemEntity.isInvulnerable()) {
                setProtected(itemEntity, false);
            }
            return;
        }
        boolean soulFire = isInside(level, itemEntity, Blocks.SOUL_FIRE);
        boolean inFire = soulFire || isInside(level, itemEntity, Blocks.FIRE);
        updateProtection(itemEntity, stack, recipe, soulFire, inFire);
    }

    /**
     * Keeps an item entity alive while the mechanic still needs it, and gives it back to vanilla otherwise.
     *
     * <p>Vanilla fire hurts item entities every tick while they overlap the fire block (2 damage per tick in
     * soul fire against 5 health), which would destroy the input before it could ever be rolled or detonated.
     * The entity is made invulnerable for as long as it is inside fire <em>and</em> is one of ours; the flag
     * lives in the persistent data so only an invulnerability set here is ever removed again.</p>
     */
    private static void updateProtection(ItemEntity itemEntity, ItemStack stack,
                                         SoulFirePurificationRecipe recipe, boolean soulFire, boolean inFire) {
        boolean marked = SoulFireImmunity.isMarked(stack);
        if (recipe == null && !marked) {
            // Cheap path: nothing of ours, so only pick up a flag we left behind earlier.
            if (itemEntity.isInvulnerable()) {
                setProtected(itemEntity, false);
            }
            return;
        }

        boolean protect = false;
        if (inFire && recipe != null) {
            if (recipe.explosion().isPresent()) {
                // Must survive long enough to reach its own detonation.
                protect = true;
            } else if (soulFire) {
                // Waits in the soul fire until a roll produces something.
                protect = true;
            }
        }
        if (!protect && inFire && marked) {
            // A freshly purified drop during its 30 s immunity window.
            protect = true;
        }
        setProtected(itemEntity, protect);
    }

    /** Applies (or clears) the manager's own invulnerability flag on the entity. */
    private static void setProtected(ItemEntity itemEntity, boolean protect) {
        if (protect) {
            if (!itemEntity.isInvulnerable()) {
                itemEntity.setInvulnerable(true);
            }
            if (itemEntity.getRemainingFireTicks() > 0) {
                itemEntity.clearFire();
            }
            itemEntity.getPersistentData().putBoolean(PROTECTED_KEY, true);
        } else if (itemEntity.getPersistentData().getBooleanOr(PROTECTED_KEY, false)) {
            itemEntity.setInvulnerable(false);
            itemEntity.getPersistentData().remove(PROTECTED_KEY);
        }
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

    /**
     * Rolls the recipe once per item of the stack. An item that produces nothing is left alone: the stack only
     * shrinks by the items that actually yielded a drop, and anything left is rolled again on the next cadence.
     */
    private static void purify(ServerLevel level, ItemEntity itemEntity, SoulFirePurificationRecipe recipe) {
        int pending = itemEntity.getItem().getCount();
        boolean producedAny = false;
        for (int index = 0; index < pending; index++) {
            if (itemEntity.isRemoved() || itemEntity.getItem().isEmpty()) {
                break;
            }
            if (!rollOnce(level, itemEntity, recipe)) {
                continue;
            }
            producedAny = true;
            ItemStack rest = itemEntity.getItem().copy();
            rest.shrink(1);
            if (rest.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(rest);
            }
        }
        if (producedAny) {
            playSoulFireFeedback(level, itemEntity);
        }
    }

    /** One roll of every result group of the recipe; true when at least one drop was spawned. */
    private static boolean rollOnce(ServerLevel level, ItemEntity itemEntity, SoulFirePurificationRecipe recipe) {
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
        return produced;
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
