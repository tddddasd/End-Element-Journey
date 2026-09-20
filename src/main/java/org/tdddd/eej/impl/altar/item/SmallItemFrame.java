package org.tdddd.eej.impl.altar.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.tdddd.eej.api.AltarItemContainer;
import org.tdddd.eej.impl.altar.AbstractAltarBlock;
import org.tdddd.eej.impl.registry.EejDataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class SmallItemFrame extends Item {
    private static final int MAX_IDS = 9;
    
    private static final String SEPARATOR = "\n";

    public SmallItemFrame(Properties properties) {
        super(properties);
    }

    

    public static List<String> getItemIds(ItemStack stack) {
        List<String> stored = stack.get(EejDataComponents.SMALL_ITEM_FRAME_IDS.get());
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    public static void setItemIds(ItemStack stack, List<String> ids) {
        stack.set(EejDataComponents.SMALL_ITEM_FRAME_IDS.get(), List.copyOf(ids));
    }

    public static boolean addItemId(ItemStack stack, String id) {
        List<String> ids = getItemIds(stack);
        if (ids.contains(id)) return false;
        if (ids.size() >= MAX_IDS) return false;
        ids.add(id);
        setItemIds(stack, ids);
        return true;
    }

    
    public static List<String> parseIds(String raw) {
        List<String> ids = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return ids;
        for (String part : raw.split(SEPARATOR)) {
            if (!part.isEmpty()) ids.add(part);
        }
        return ids;
    }

    
    public static String joinIds(List<String> ids) {
        return String.join(SEPARATOR, ids);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof AbstractAltarBlock) {
            if (!level.isClientSide() && player instanceof ServerPlayer) {
                ItemStack stack = context.getItemInHand();
                List<String> ids = getItemIds(stack);
                if (level.getBlockEntity(pos) instanceof AltarItemContainer pedestal) {
                    pedestal.setFilterData(ids);
                    level.playSound(null, pos, SoundEvents.WOODEN_BUTTON_CLICK_ON, SoundSource.BLOCKS, 1.0F, 1.0F);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            Identifier id = BuiltInRegistries.ITEM.getKey(offhand.getItem());
            if (id != null) {
                String idStr = id.toString();
                if (addItemId(stack, idStr)) {
                    
                    
                    player.sendSystemMessage(
                            Component.translatable("item.small_item_frame.added", idStr));
                    return InteractionResult.SUCCESS;
                } else {
                    player.sendSystemMessage(
                            Component.translatable("item.small_item_frame.max_reached"));
                    return InteractionResult.FAIL;
                }
            }
        } else {
            player.sendSystemMessage(
                    Component.translatable("item.small_item_frame.no_offhand"));
            return InteractionResult.FAIL;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        List<String> ids = getItemIds(stack);
        if (!ids.isEmpty()) {
            for (String id : ids) {
                tooltip.accept(Component.literal(" - " + id)
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            }
            tooltip.accept(Component.translatable("item.small_item_frame.count", ids.size(), MAX_IDS)
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
