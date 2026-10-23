package org.tdddd.eej.impl.element.plate.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaStorage;
import org.tdddd.eej.impl.registry.EejBlockEntities;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the element ids fed into an {@link org.tdddd.eej.impl.element.plate.ElementAggregationPlate}.
 *
 * <p>Only ids are stored, not item stacks: every element is the same item and the element type is the
 * id, so the plate does not need to keep stacks alive. Elements can never be removed.</p>
 */
public class ElementAggregationPlateBlockEntity extends BlockEntity {

    /** Maximum number of element items a single plate can hold. */
    public static final int MAX_ELEMENTS = 9;
    /** Mana this plate can store. */
    public static final int MAX_MANA = 10;

    private static final String KEY_ELEMENTS = "Elements";

    private final List<ResourceLocation> elements = new ArrayList<>();
    /** Shared mana storage: the plate both stores and supplies mana for element synthesis. */
    private final ManaStorage mana = new ManaStorage(MAX_MANA).onChanged(this::setChanged);
    /** Capability view of {@link #mana}, so pipes and Jade can read it. */
    private final LazyOptional<IManaStorage> manaCapability = LazyOptional.of(() -> this.mana);
    /** Single output slot: element results stay here and are drawn at the plate centre. */
    private ItemStack outputSlot = ItemStack.EMPTY;

    public ElementAggregationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(EejBlockEntities.ELEMENT_AGGREGATION_PLATE.get(), pos, state);
    }

    /** Read-only view of the stored element ids, in insertion order. */
    public List<ResourceLocation> elements() {
        return Collections.unmodifiableList(this.elements);
    }

    public int elementCount() {
        return this.elements.size();
    }

    public boolean isFull() {
        return this.elements.size() >= MAX_ELEMENTS;
    }

    public ItemStack outputSlot() {
        return this.outputSlot;
    }

    /** Replaces the output slot; used by both recipes. */
    public void setOutputSlot(ItemStack stack) {
        this.outputSlot = stack == null ? ItemStack.EMPTY : stack;
        this.setChanged();
        this.sync();
    }

    /** Hands the output slot to {@code player} and clears it. */
    public ItemStack takeOutputSlot(net.minecraft.world.entity.player.Player player) {
        if (this.outputSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = this.outputSlot.copy();
        if (!player.getInventory().add(taken)) {
            player.drop(taken, false);
        }
        this.outputSlot = ItemStack.EMPTY;
        this.setChanged();
        this.sync();
        return taken;
    }

    public ManaStorage manaStorage() {
        return this.mana;
    }

    public int mana() {
        return this.mana.getManaStored();
    }

    /** Consumes up to {@code amount} mana and returns how much was actually taken. */
    public int consumeMana(int amount) {
        return this.mana.consume(amount);
    }

    /** Accepts mana pushed in from a block below (an altar). */
    public int receiveMana(int amount) {
        return this.mana.receiveMana(amount, null, false);
    }

    /** Pulls mana from a neighbour that exposes a storage, mimicking a pipe-less transfer. */
    public int pullManaFrom(net.minecraft.world.level.block.entity.BlockEntity source, int amount) {
        if (!(source instanceof org.tdddd.eej.api.mana.IManaStorage storage)) {
            return 0;
        }
        int available = storage.extractMana(amount, null, true);
        if (available <= 0) {
            return 0;
        }
        int accepted = this.mana.receiveMana(available, null, true);
        if (accepted <= 0) {
            return 0;
        }
        storage.extractMana(accepted, null, false);
        return this.mana.receiveMana(accepted, null, false);
    }

    /** Stores one element id. Returns false when the plate is full. */
    public boolean tryInsert(ServerPlayer player, ResourceLocation elementId) {
        if (this.isFull()) {
            return false;
        }
        this.elements.add(elementId);
        this.setChanged();
        this.sync();
        if (!player.isCreative()) {
            player.getMainHandItem().shrink(1);
        }
        return true;
    }


    /**
     * Runs one element aggregation: finds a recipe whose element multiset matches the stored elements
     * exactly, consumes them and returns the result stack (empty when nothing matched).
     */
    public ItemStack craftAggregation(net.minecraft.world.level.Level level) {
        if (this.elements.isEmpty() || level.isClientSide()) {
            return ItemStack.EMPTY;
        }
        var recipes = ((net.minecraft.world.item.crafting.RecipeManager) level.getRecipeManager())
                .getAllRecipesFor(org.tdddd.eej.impl.registry.EejRecipes.ELEMENT_AGGREGATION.get());
        for (org.tdddd.eej.impl.recipe.ElementAggregationRecipe recipe : recipes) {
            if (!recipe.matchesElements(this.elements)) {
                continue;
            }
            if (!this.outputSlot.isEmpty()) {
                return ItemStack.EMPTY;   // output slot occupied
            }
            ItemStack result = recipe.getResultItem(level.registryAccess()).copy();
            this.elements.clear();
            this.setOutputSlot(result);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag list = new ListTag();
        for (ResourceLocation id : this.elements) {
            list.add(StringTag.valueOf(id.toString()));
        }
        tag.put(KEY_ELEMENTS, list);
        this.mana.save(tag);
        if (!this.outputSlot.isEmpty()) {
            tag.put("Output", this.outputSlot.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.elements.clear();
        ListTag list = tag.getList(KEY_ELEMENTS, Tag.TAG_STRING);
        for (int i = 0; i < list.size() && this.elements.size() < MAX_ELEMENTS; i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                this.elements.add(id);
            }
        }
        this.mana.load(tag);
        this.outputSlot = tag.contains("Output")
                ? ItemStack.of(tag.getCompound("Output")) : ItemStack.EMPTY;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap == org.tdddd.eej.impl.capability.EejCapabilities.MANA_STORAGE) {
            return this.manaCapability.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        this.manaCapability.invalidate();
    }

    /** Sent to clients when the block entity is first loaded, so the renderer sees the elements. */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the block for a client sync; called whenever the stored elements or the mana change. */
    private void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}
