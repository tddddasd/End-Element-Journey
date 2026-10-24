package org.tdddd.eej.impl.element.plate.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaStorage;
import org.tdddd.eej.impl.recipe.ElementAggregationRecipe;
import org.tdddd.eej.impl.registry.EejBlockEntities;
import org.tdddd.eej.impl.registry.EejRecipeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stores the element ids fed into an {@link org.tdddd.eej.impl.element.plate.ElementAggregationPlate}.
 *
 * <p>Only ids are stored, not item stacks: every element is the same item and the element type is the
 * id. Elements can never be removed. A single output slot holds the element an aggregation produced,
 * and the plate also stores up to {@link #MAX_MANA} mana for element synthesis.</p>
 */
public class ElementAggregationPlateBlockEntity extends BlockEntity {

    /** Maximum number of element items a single plate can hold. */
    public static final int MAX_ELEMENTS = 9;
    /** Mana this plate can store. */
    public static final int MAX_MANA = 10;

    private static final String KEY_ELEMENTS = "Elements";

    private final List<Identifier> elements = new ArrayList<>();
    private final ManaStorage mana = new ManaStorage(MAX_MANA).onChanged(this::setChanged);
    /** Single output slot: element results stay here and are drawn at the plate centre. */
    private ItemStack outputSlot = ItemStack.EMPTY;

    public ElementAggregationPlateBlockEntity(BlockPos pos, BlockState state) {
        super(EejBlockEntities.ELEMENT_AGGREGATION_PLATE.get(), pos, state);
    }

    public IManaStorage manaStorage() {
        return this.mana;
    }

    public int mana() {
        return this.mana.getManaStored();
    }

    public int consumeMana(int amount) {
        return this.mana.consume(amount);
    }

    /** Read-only view of the stored element ids, in insertion order. */
    public List<Identifier> elements() {
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

    public void setOutputSlot(ItemStack stack) {
        this.outputSlot = stack == null ? ItemStack.EMPTY : stack;
        this.setChanged();
        this.sync();
    }

    /** Hands the output slot to {@code player} and clears it. */
    public ItemStack takeOutputSlot(Player player) {
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

    /** Stores one element id. Returns false when the plate is full. */
    public boolean tryInsert(Player player, Identifier elementId) {
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
     * exactly, consumes them and puts the result in the output slot.
     */
    public ItemStack craftAggregation(Level level) {
        if (this.elements.isEmpty() || level.isClientSide() || !this.outputSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // 26.1.2 has no per-type lookup on the manager: walk the recipes and filter by type
        for (net.minecraft.world.item.crafting.RecipeHolder<?> holder : ((net.minecraft.world.item.crafting.RecipeManager) level.recipeAccess()).getRecipes()) {
            if (!(holder.value() instanceof ElementAggregationRecipe recipe)) {
                continue;
            }
            if (!recipe.matchesElements(this.elements)) {
                continue;
            }
            ItemStack result = recipe.assemble(null);
            this.elements.clear();
            this.setOutputSlot(result);
            return result;
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.TypedOutputList<String> list = output.list(KEY_ELEMENTS, com.mojang.serialization.Codec.STRING);
        for (Identifier id : this.elements) {
            list.add(id.toString());
        }
        this.mana.save(output);
        if (!this.outputSlot.isEmpty()) {
            output.store("Output", ItemStack.CODEC, this.outputSlot);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.elements.clear();
        input.listOrEmpty(KEY_ELEMENTS, com.mojang.serialization.Codec.STRING).forEach(text -> {
            Identifier id = Identifier.tryParse(text);
            if (id != null && this.elements.size() < MAX_ELEMENTS) {
                this.elements.add(id);
            }
        });
        this.mana.load(input);
        this.outputSlot = input.read("Output", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public void handleUpdateTag(ValueInput input) {
        this.loadWithComponents(input);
        if (this.level != null && this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    /** Marks the block for a client sync; called whenever the stored elements or the mana change. */
    private void sync() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}
