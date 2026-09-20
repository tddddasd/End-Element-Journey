package org.tdddd.eej.impl.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.registries.ForgeRegistries;
import org.tdddd.eej.api.mob.MobEnchantment;
import org.tdddd.eej.api.mob.MobEnchantmentApi;

import java.util.ArrayList;
import java.util.List;


public final class EejEnchantmentCommand {

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ENCHANTMENT =
            new DynamicCommandExceptionType(id -> Component.translatable("commands.eej.enchantment.unknown", id));

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_ENCHANTMENTS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(ForgeRegistries.ENCHANTMENTS.getKeys(), builder);

    private EejEnchantmentCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eej_enchantment")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityTargetArgument.entityTarget())
                        .then(Commands.argument("enchantment", ResourceLocationArgument.id())
                                .suggests(SUGGEST_ENCHANTMENTS)
                                .then(Commands.argument("duration", IntegerArgumentType.integer())
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(EejEnchantmentCommand::apply)))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityTargetArgument.entityTarget())
                                .executes(EejEnchantmentCommand::clear))));
    }

    private static int apply(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<LivingEntity> targets = livingTargets(context);
        ResourceLocation enchantmentId = ResourceLocationArgument.getId(context, "enchantment");
        Enchantment enchantment = ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
        if (enchantment == null) {
            throw ERROR_UNKNOWN_ENCHANTMENT.create(enchantmentId);
        }

        int seconds = IntegerArgumentType.getInteger(context, "duration");
        int level = IntegerArgumentType.getInteger(context, "level");
        if (level <= 0) {
            return remove(source, targets, enchantment);
        }
        if (targets.isEmpty()) {
            source.sendFailure(Component.translatable("commands.eej.enchantment.none"));
            return 0;
        }

        int durationTicks = seconds > 0 ? seconds * 20 : MobEnchantment.PERMANENT;
        int changed = 0;
        for (LivingEntity target : targets) {
            if (MobEnchantmentApi.apply(target, enchantment, level, durationTicks)) {
                changed++;
            }
        }

        
        int resultLevel = 0;
        int resultSeconds = 0;
        boolean resultPermanent = false;
        for (LivingEntity target : targets) {
            MobEnchantment entry = find(target, enchantment);
            if (entry == null) continue;
            resultLevel = Math.max(resultLevel, entry.getLevel());
            if (entry.isPermanent()) {
                resultPermanent = true;
            } else {
                resultSeconds = Math.max(resultSeconds, entry.getRemainingSeconds());
            }
        }

        if (changed == 0) {
            source.sendSuccess(() -> Component.translatable("commands.eej.enchantment.no_effect", targets.size()), false);
            return 0;
        }

        final int count = targets.size();
        final int effectiveLevel = resultLevel;
        final Component name = enchantment.getFullname(effectiveLevel);
        final Component durationText = resultPermanent
                ? Component.translatable("commands.eej.enchantment.permanent")
                : Component.translatable("commands.eej.enchantment.seconds", resultSeconds);
        source.sendSuccess(() -> Component.translatable("commands.eej.enchantment.success",
                count, name, effectiveLevel, durationText), true);
        return changed;
    }

    private static int remove(CommandSourceStack source, List<LivingEntity> targets, Enchantment enchantment) {
        if (targets.isEmpty()) {
            source.sendFailure(Component.translatable("commands.eej.enchantment.none"));
            return 0;
        }
        int changed = 0;
        for (LivingEntity target : targets) {
            if (MobEnchantmentApi.remove(target, enchantment)) {
                changed++;
            }
        }
        if (changed == 0) {
            source.sendSuccess(() -> Component.translatable("commands.eej.enchantment.no_effect", targets.size()), false);
            return 0;
        }
        final int count = targets.size();
        final Component name = enchantment.getFullname(1);
        source.sendSuccess(() -> Component.translatable("commands.eej.enchantment.removed", count, name), true);
        return changed;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        List<LivingEntity> targets = livingTargets(context);
        if (targets.isEmpty()) {
            source.sendFailure(Component.translatable("commands.eej.enchantment.none"));
            return 0;
        }
        int changed = 0;
        for (LivingEntity target : targets) {
            if (MobEnchantmentApi.clear(target)) {
                changed++;
            }
        }
        if (changed == 0) {
            source.sendSuccess(() -> Component.translatable("commands.eej.enchantment.no_effect", targets.size()), false);
            return 0;
        }
        final int count = targets.size();
        source.sendSuccess(() -> Component.translatable("commands.eej.enchantment.cleared", count), true);
        return changed;
    }

    
    private static List<LivingEntity> livingTargets(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        List<LivingEntity> living = new ArrayList<>();
        for (Entity entity : EntityTargetArgument.getTargets(context, "targets")) {
            if (entity instanceof LivingEntity livingEntity) {
                living.add(livingEntity);
            }
        }
        return living;
    }

    private static MobEnchantment find(LivingEntity entity, Enchantment enchantment) {
        for (MobEnchantment entry : MobEnchantmentApi.getAll(entity)) {
            if (entry.getEnchantment() == enchantment) return entry;
        }
        return null;
    }
}
