package org.tdddd.eej.impl.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.eej.impl.registry.EejRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * Data driven recipe type {@code eej:soul_fire_purification}.
 *
 * <p>The recipe is never crafted in a container: an item entity standing inside
 * {@code minecraft:soul_fire} is consumed by {@link SoulFirePurificationHandler}, which rolls this
 * recipe. It carries data only (ingredient, weighted results, optional explosion) and exists at
 * runtime so datapacks - and JEI - can read it.
 *
 * <p>JSON schema:
 * <pre>
 * {
 *   "type": "eej:soul_fire_purification",
 *   "ingredient": { "item": "epca:infested_raw_iron" },
 *   "burn_chance": 0.85,                        // optional, informational (shown by JEI)
 *   "weighted": false,                          // optional; see "Roll semantics" below
 *   "destroyed": false,                         // optional; see "Destroy-only recipes" below
 *   "results": [
 *     { "chance": 0.15, "count_min": 2, "count_max": 3,
 *       "random": false, "items": [ "minecraft:raw_iron" ], "weight": 1 }
 *   ],
 *   "explode": { "power": 4.0, "fire": false, "blindness_ticks": 100 }   // optional
 * }
 * </pre>
 *
 * <p><b>Roll semantics.</b> By default every result group rolls its own {@code chance}
 * (independent rolls). When {@code weighted} is true the groups of one logical rule share a single
 * roll and group {@code i} fires when {@code random < chance * weight_i / sum(weight)} - that is how
 * "30 % chance to spawn one random original block out of the N blocks that convert into this
 * infested block" is stored once instead of repeating the 30 % on every candidate. The flag is also
 * inferred from any {@code weight > 1}, so data files that omit it still behave correctly.
 *
 * <p><b>Destroy-only recipes.</b> {@code "destroyed": true} with no results means the item is
 * consumed by soul fire without any roll (used for {@code epca:infested_flesh}). A recipe must
 * declare either results or {@code explode} or {@code destroyed}; an ambiguous empty recipe is
 * rejected at load time.
 *
 * <p>An empty {@code results} array without {@code explode} means "destroyed immediately without any
 * roll" (used for {@code epca:infested_flesh}).
 *
 * <p>1.20.1 keeps its ingredients Gson based, so this recipe parses with Gson directly instead of a
 * DFU codec; see {@link #fromJson(ResourceLocation, JsonObject)}.
 */
public class SoulFirePurificationRecipe implements Recipe<Container> {
    // --- soul speed feedback (灵魂疾行) tuning -------------------------------------------------
    /** Amount of {@code ParticleTypes.SOUL} particles emitted per successful purification. */
    public static final int SOUL_FEEDBACK_PARTICLES = 12;
    /** Symmetric spread of the burst around the purification position. */
    public static final double SOUL_FEEDBACK_SPREAD = 0.3D;
    /** Particle speed; vanilla soul speed particles are spawned with essentially no motion. */
    public static final double SOUL_FEEDBACK_PARTICLE_SPEED = 0.02D;
    /** Sound volume; vanilla randomises between 0.0 and 0.6, this always uses the audible value. */
    public static final float SOUL_FEEDBACK_VOLUME = 0.6F;
    /** Lowest pitch; vanilla uses {@code 0.6 + rand * 0.4}. */
    public static final float SOUL_FEEDBACK_PITCH_MIN = 0.6F;
    /** Pitch range; vanilla uses {@code 0.6 + rand * 0.4}. */
    public static final float SOUL_FEEDBACK_PITCH_RANGE = 0.4F;

    private final Ingredient ingredient;
    private final List<SoulFireOutput> outputs;
    private final float burnChance;
    private final Optional<ExplosionSpec> explosion;
    private final boolean weighted;
    private final boolean destroyed;

    private ResourceLocation id = new ResourceLocation("eej", "soul_fire_purification/unset");

    public SoulFirePurificationRecipe(Ingredient ingredient, List<SoulFireOutput> outputs,
                                      float burnChance, Optional<ExplosionSpec> explosion) {
        this(ingredient, outputs, burnChance, explosion, false, outputs.isEmpty() && explosion.isEmpty());
    }

    public SoulFirePurificationRecipe(Ingredient ingredient, List<SoulFireOutput> outputs,
                                      float burnChance, Optional<ExplosionSpec> explosion,
                                      boolean weighted) {
        this(ingredient, outputs, burnChance, explosion, weighted,
                outputs.isEmpty() && explosion.isEmpty());
    }

    public SoulFirePurificationRecipe(Ingredient ingredient, List<SoulFireOutput> outputs,
                                      float burnChance, Optional<ExplosionSpec> explosion,
                                      boolean weighted, boolean destroyed) {
        this.ingredient = ingredient;
        this.outputs = List.copyOf(outputs);
        this.burnChance = burnChance;
        this.explosion = explosion;
        // An explicit weight > 1 is enough to switch the recipe to the shared-roll semantics.
        this.weighted = weighted || outputs.stream().anyMatch(output -> output.weight() > 1);
        this.destroyed = destroyed;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public List<SoulFireOutput> outputs() {
        return outputs;
    }

    /** Informational chance that the item is simply destroyed instead of producing anything. */
    public float burnChance() {
        return burnChance;
    }

    /** Present when this item must explode on contact with fire instead of being purified. */
    public Optional<ExplosionSpec> explosion() {
        return explosion;
    }

    public boolean isExplosive() {
        return explosion.isPresent();
    }

    /** True when this recipe describes an item destroyed without any roll (e.g. infested flesh). */
    public boolean isDestroyOnly() {
        return destroyed && outputs.isEmpty() && !isExplosive();
    }

    /** True when the data file explicitly declared {@code "destroyed": true}. */
    public boolean isDestroyed() {
        return destroyed;
    }

    /** Total weight of the weighted-alternative roll, or 0 when the recipe uses independent rolls. */
    public int totalWeight() {
        if (!weighted) {
            return 0;
        }
        int total = 0;
        for (SoulFireOutput output : outputs) {
            total += Math.max(1, output.weight());
        }
        return total;
    }

    /** True when the groups of one logical rule share a single roll instead of rolling each. */
    public boolean isWeighted() {
        return weighted;
    }

    public boolean usesWeightedAlternative() {
        return weighted;
    }

    /**
     * Rolls this recipe once for a single consumed item and spawns the produced stacks.
     *
     * @param level  server level the item entity lives in
     * @param x      spawn x of the consumed item
     * @param y      spawn y of the consumed item
     * @param z      spawn z of the consumed item
     * @param random random source, normally the level's
     * @return true when at least one stack was produced
     */
    public boolean rollAndSpawn(Level level, double x, double y, double z, RandomSource random) {
        if (weighted) {
            int total = totalWeight();
            double threshold = 0.0D;
            for (SoulFireOutput output : outputs) {
                threshold += (double) output.chance() * Math.max(1, output.weight()) / total;
                if (random.nextDouble() < threshold) {
                    boolean spawned = spawn(level, x, y, z, output.rollStack(random));
                    if (spawned) {
                        playSoulFeedback(level, x, y, z, random);
                    }
                    return spawned;
                }
            }
            return false;
        }

        boolean produced = false;
        for (SoulFireOutput output : outputs) {
            if (output.chance() >= 1.0F || random.nextFloat() < output.chance()) {
                produced |= spawn(level, x, y, z, output.rollStack(random));
            }
        }
        if (produced) {
            playSoulFeedback(level, x, y, z, random);
        }
        return produced;
    }

    private static boolean spawn(Level level, double x, double y, double z, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemEntity entity = new ItemEntity(level, x, y, z, stack);
        entity.setDefaultPickUpDelay();
        // Mark the drop so it is fire immune for a while and never re-processed by the mechanic.
        SoulFirePurificationHandler.markPurified(entity);
        level.addFreshEntity(entity);
        return true;
    }

    /**
     * Plays the vanilla soul speed feedback (灵魂疾行) at the purification position: a burst of
     * {@link net.minecraft.core.particles.ParticleTypes#SOUL} and
     * {@link net.minecraft.sounds.SoundEvents#SOUL_ESCAPE}.
     *
     * <p>Vanilla reference, verified against the mapped 1.20.1 bytecode of
     * {@code LivingEntity#spawnSoulSpeedParticle()}: it emits a single {@code ParticleTypes.SOUL}
     * particle through {@code Level#addParticle} at
     * {@code (x + (rand-0.5)*bbWidth, y + 0.1, z + (rand-0.5)*bbWidth)} with the motion
     * {@code (dx*-0.2, 0.1, dz*-0.2)}, and plays {@code SoundEvents.SOUL_ESCAPE} at a randomised
     * volume of either {@code 0.0} or {@code 0.6} and a pitch of {@code 0.6 + rand*0.4}. This keeps
     * the pitch curve, uses the always audible volume {@code 0.6}, and widens the single vanilla
     * particle into a small burst.
     */
    public static void playSoulFeedback(Level level, double x, double y, double z, RandomSource random) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                x, y + 0.1D, z,
                SOUL_FEEDBACK_PARTICLES,
                SOUL_FEEDBACK_SPREAD, SOUL_FEEDBACK_SPREAD, SOUL_FEEDBACK_SPREAD,
                SOUL_FEEDBACK_PARTICLE_SPEED);
        serverLevel.playSound(null, x, y, z, net.minecraft.sounds.SoundEvents.SOUL_ESCAPE,
                net.minecraft.sounds.SoundSource.BLOCKS, SOUL_FEEDBACK_VOLUME,
                SOUL_FEEDBACK_PITCH_MIN + random.nextFloat() * SOUL_FEEDBACK_PITCH_RANGE);
    }

    @Override
    public boolean matches(Container container, Level level) {
        // Not used: the recipe is consumed by the soul fire mechanic, never by a container.
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    /**
     * JEI-facing "main" result: a deterministic sample of the first output group, or empty for
     * destroy-only / explosive recipes. JEI uses {@link #outputs()} directly for the full list.
     */
    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0).rollStack(RandomSource.create(0L));
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.add(ingredient);
        return list;
    }

    @Override
    public boolean isSpecial() {
        // Keeps the recipe out of the recipe book; the mechanic reads it directly.
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return EejRecipes.SOUL_FIRE_PURIFICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return EejRecipes.SOUL_FIRE_PURIFICATION_TYPE.get();
    }

    /** Item id of the single-item ingredient, or null when the ingredient is a tag / multi item. */
    public ResourceLocation ingredientItemId() {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 1) {
            return ForgeRegistries.ITEMS.getKey(stacks[0].getItem());
        }
        return null;
    }

    /** Optional explosion block of {@link SoulFirePurificationRecipe}. */
    public record ExplosionSpec(float power, boolean fire, int blindnessTicks) {
        /** Radius used for the extra custom damage / knockback and for the blindness sweep. */
        public double radius() {
            return power * 2.0D;
        }

        public static ExplosionSpec fromJson(JsonObject json) {
            return new ExplosionSpec(
                    GsonHelper.getAsFloat(json, "power", 4.0F),
                    GsonHelper.getAsBoolean(json, "fire", false),
                    GsonHelper.getAsInt(json, "blindness_ticks", 100));
        }
    }

    /** Gson serializer/deserializer pair plus the matching network form. */
    public static class Serializer implements RecipeSerializer<SoulFirePurificationRecipe> {
        @Override
        public SoulFirePurificationRecipe fromJson(ResourceLocation id, JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            if (ingredient.isEmpty()) {
                throw new com.google.gson.JsonParseException(
                        "Invalid eej:soul_fire_purification recipe " + id + ": empty ingredient");
            }

            List<SoulFireOutput> outputs = new ArrayList<>();
            if (json.has("results")) {
                for (JsonElement element : GsonHelper.getAsJsonArray(json, "results")) {
                    outputs.add(SoulFireOutput.fromJson(element.getAsJsonObject()));
                }
            }

            float burnChance = GsonHelper.getAsFloat(json, "burn_chance", 0.0F);
            boolean weighted = GsonHelper.getAsBoolean(json, "weighted", false);
            boolean destroyed = GsonHelper.getAsBoolean(json, "destroyed", false);
            Optional<ExplosionSpec> explosion = json.has("explode")
                    ? Optional.of(ExplosionSpec.fromJson(GsonHelper.getAsJsonObject(json, "explode")))
                    : Optional.empty();

            if (outputs.isEmpty() && explosion.isEmpty() && !destroyed) {
                throw new com.google.gson.JsonParseException("Invalid eej:soul_fire_purification recipe "
                        + id + ": it has no results, no explode and no \"destroyed\": true, so its "
                        + "behaviour would be undefined");
            }
            if (outputs.isEmpty() && weighted) {
                throw new com.google.gson.JsonParseException("Invalid eej:soul_fire_purification recipe "
                        + id + ": \"weighted\" needs at least one result group");
            }

            SoulFirePurificationRecipe recipe = new SoulFirePurificationRecipe(
                    ingredient, outputs, burnChance, explosion, weighted, destroyed);
            recipe.setId(id);
            return recipe;
        }

        @Override
        public SoulFirePurificationRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int resultCount = buffer.readVarInt();
            List<SoulFireOutput> outputs = new ArrayList<>(resultCount);
            for (int i = 0; i < resultCount; i++) {
                boolean random = buffer.readBoolean();
                int size = buffer.readVarInt();
                List<ResourceLocation> ids = new ArrayList<>(size);
                for (int j = 0; j < size; j++) {
                    ids.add(buffer.readResourceLocation());
                }
                List<Item> items = new ArrayList<>(ids.size());
                for (ResourceLocation itemId : ids) {
                    Item item = ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null) {
                        items.add(item);
                    }
                }
                int countMin = buffer.readVarInt();
                int countMax = buffer.readVarInt();
                float chance = buffer.readFloat();
                int weight = buffer.readVarInt();
                outputs.add(new SoulFireOutput(random, items, countMin, countMax, chance, weight));
            }
            float burnChance = buffer.readFloat();
            boolean weighted = buffer.readBoolean();
            boolean destroyed = buffer.readBoolean();
            Optional<ExplosionSpec> explosion = buffer.readOptional(
                    in -> new ExplosionSpec(in.readFloat(), in.readBoolean(), in.readVarInt()));
            SoulFirePurificationRecipe recipe = new SoulFirePurificationRecipe(
                    ingredient, outputs, burnChance, explosion, weighted, destroyed);
            recipe.setId(id);
            return recipe;
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, SoulFirePurificationRecipe recipe) {
            recipe.ingredient().toNetwork(buffer);
            buffer.writeVarInt(recipe.outputs().size());
            for (SoulFireOutput output : recipe.outputs()) {
                buffer.writeBoolean(output.random());
                List<ResourceLocation> ids = output.itemIds();
                buffer.writeVarInt(ids.size());
                for (ResourceLocation itemId : ids) {
                    buffer.writeResourceLocation(itemId);
                }
                buffer.writeVarInt(output.countMin());
                buffer.writeVarInt(output.countMax());
                buffer.writeFloat(output.chance());
                buffer.writeVarInt(output.weight());
            }
            buffer.writeFloat(recipe.burnChance());
            buffer.writeBoolean(recipe.isWeighted());
            buffer.writeBoolean(recipe.isDestroyed());
            buffer.writeOptional(recipe.explosion(), (out, spec) -> {
                out.writeFloat(spec.power());
                out.writeBoolean(spec.fire());
                out.writeVarInt(spec.blindnessTicks());
            });
        }
    }

    /** Builds a recipe straight from JSON; used by tooling and tests. */
    public static SoulFirePurificationRecipe fromJson(ResourceLocation id, JsonObject json) {
        return new Serializer().fromJson(id, json);
    }
}
