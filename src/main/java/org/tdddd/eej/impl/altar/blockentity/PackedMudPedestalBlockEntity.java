package org.tdddd.eej.impl.altar.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.Nullable;
import org.tdddd.eej.api.AltarItemContainer;
import org.tdddd.eej.impl.altar.AbstractAltarBlock;
import org.tdddd.eej.impl.network.PedestalItemSyncPacket;
import org.tdddd.eej.impl.registry.EejBlockEntities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public class PackedMudPedestalBlockEntity extends BlockEntity implements AltarItemContainer {
    private final ItemStackHandler itemHandler = createHandler();
    private final Map<Direction, IItemHandler> sideHandlers = new EnumMap<>(Direction.class);

    private List<String> filterData = Collections.emptyList();
    private boolean isMainPedestal = false;

    public PackedMudPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(EejBlockEntities.PACKED_MUD_PEDESTAL.get(), pos, state);
        for (Direction dir : Direction.values()) {
            sideHandlers.put(dir, new SideFilteredItemHandler(dir));
        }
    }

    private ItemStackHandler createHandler() {
        return new ItemStackHandler(1) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
                if (level != null) {
                    requestModelDataUpdate();
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return true;
            }

            @Override
            public int getSlotLimit(int slot) {
                
                return 64;
            }
        };
    }

    @Override
    public boolean hasItem() {
        return !itemHandler.getStackInSlot(0).isEmpty();
    }

    @Override
    public ItemStack getItem() {
        return itemHandler.getStackInSlot(0);
    }

    @Override
    public void setItem(ItemStack stack) {
        if (stack.isEmpty()) {
            clearItem();
        } else {
            itemHandler.setStackInSlot(0, stack.copy());
            syncToClient();
        }
    }

    @Override
    public void clearItem() {
        itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        syncToClient();
    }

    @Override
    public List<String> getFilterData() {
        return filterData;
    }

    @Override
    public void setFilterData(List<String> data) {
        this.filterData = data == null ? Collections.emptyList() : new ArrayList<>(data);
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public boolean isMainPedestal() {
        return isMainPedestal;
    }

    
    @Override
    public void setMainPedestal(boolean isMain) {
        if (this.isMainPedestal == isMain) {
            return;
        }
        this.isMainPedestal = isMain;
        setChanged();
    }

    

    
    @Override
    public void onDataPacket(Connection net, ValueInput valueInput) {
        handleUpdateTag(valueInput);
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            requestModelDataUpdate();
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("inventory").ifPresent(itemHandler::deserialize);
        List<String> filters = new ArrayList<>();
        input.listOrEmpty("filterData", com.mojang.serialization.Codec.STRING).forEach(filters::add);
        filterData = filters.isEmpty() ? Collections.emptyList() : filters;
        
        isMainPedestal = input.getBooleanOr("mainPedestal", false);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput inventory = output.child("inventory");
        itemHandler.serialize(inventory);
        ValueOutput.TypedOutputList<String> list = output.list("filterData", com.mojang.serialization.Codec.STRING);
        for (String s : filterData) {
            list.add(s);
        }
        output.putBoolean("mainPedestal", isMainPedestal);
    }

    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    
    @Override
    public void handleUpdateTag(ValueInput input) {
        loadWithComponents(input);
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            requestModelDataUpdate();
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    
    
    
    

    public void syncItem(ItemStack stack) {
        if (stack.isEmpty()) {
            itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        } else {
            itemHandler.setStackInSlot(0, stack.copy());
        }
        requestModelDataUpdate();
        if (level != null && level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void syncToClient() {
        if (level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingChunk(
                serverLevel,
                new net.minecraft.world.level.ChunkPos(worldPosition.getX() >> 4, worldPosition.getZ() >> 4),
                new PedestalItemSyncPacket(worldPosition, getItem()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel) {
            syncToClient();
        }
    }

    

    
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        
        
        
        event.registerBlockEntity(Capabilities.Item.BLOCK, EejBlockEntities.PACKED_MUD_PEDESTAL.get(),
                (be, side) -> side == null ? null : new SideFilteredResourceHandler(be, side));
    }

    private boolean isLocked() {
        if (level == null) return true;
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof AbstractAltarBlock altarBlock) {
            return altarBlock.isLocked(level, worldPosition);
        }
        return true;
    }

    private void notifyChange() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClient();
        }
    }

    
    public IItemHandler getSideHandler(@Nullable Direction side) {
        return side == null ? null : sideHandlers.get(side);
    }

    
    private class SideFilteredItemHandler implements IItemHandler {
        private final Direction side;

        SideFilteredItemHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (isLocked()) return stack;
            if (side == Direction.UP || side == Direction.DOWN) return stack;
            if (!itemHandler.getStackInSlot(slot).isEmpty()) return stack;

            
            
            if (!PackedMudPedestalBlockEntity.this.acceptsInsertion(stack)) {
                return stack;
            }

            ItemStack remaining = itemHandler.insertItem(slot, stack, simulate);
            if (!simulate && (remaining.isEmpty() || remaining.getCount() < stack.getCount())) {
                notifyChange();
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isLocked()) return ItemStack.EMPTY;
            if (side != Direction.DOWN) return ItemStack.EMPTY;

            ItemStack extracted = itemHandler.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                notifyChange();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isLocked()) return false;
            if (side == Direction.UP || side == Direction.DOWN) return false;
            return itemHandler.getStackInSlot(slot).isEmpty() && itemHandler.isItemValid(slot, stack);
        }
    }

    
    private static class SideFilteredResourceHandler implements ResourceHandler<ItemResource> {
        private final PackedMudPedestalBlockEntity be;
        private final Direction side;

        SideFilteredResourceHandler(PackedMudPedestalBlockEntity be, Direction side) {
            this.be = be;
            this.side = side;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemResource getResource(int index) {
            return ItemResource.of(be.itemHandler.getStackInSlot(0));
        }

        @Override
        public long getAmountAsLong(int index) {
            return be.itemHandler.getStackInSlot(0).getCount();
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            return be.itemHandler.getSlotLimit(0);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            if (be.isLocked()) return false;
            if (side == Direction.UP || side == Direction.DOWN) return false;
            if (!be.itemHandler.getStackInSlot(0).isEmpty()) return false;
            
            return be.acceptsInsertion(resource.toStack());
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (!isValid(index, resource)) return 0;
            ItemStack stack = resource.toStack(amount);
            ItemStack remaining = be.itemHandler.insertItem(0, stack, false);
            int inserted = amount - remaining.getCount();
            if (inserted > 0) {
                be.notifyChange();
            }
            return inserted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            if (be.isLocked()) return 0;
            if (side != Direction.DOWN) return 0;
            ItemStack current = be.itemHandler.getStackInSlot(0);
            if (current.isEmpty() || !resource.matches(current)) return 0;
            ItemStack extracted = be.itemHandler.extractItem(0, amount, false);
            if (!extracted.isEmpty()) {
                be.notifyChange();
            }
            return extracted.getCount();
        }
    }
}
