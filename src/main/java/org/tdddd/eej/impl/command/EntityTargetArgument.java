package org.tdddd.eej.impl.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class EntityTargetArgument implements ArgumentType<EntityTarget> {

    private static final List<String> SELECTOR_EXAMPLES = List.of("@e", "@a", "@p", "@r", "@s");

    private static final DynamicCommandExceptionType ERROR_UNKNOWN_TARGET =
            new DynamicCommandExceptionType(raw -> Component.translatable("commands.eej.enchantment.unknown_target", raw));

    private static volatile Set<String> entityNamespaces;

    public static EntityTargetArgument entityTarget() {
        return new EntityTargetArgument();
    }

    public static List<Entity> getTargets(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, EntityTarget.class).resolve(context.getSource());
    }

    @Override
    public EntityTarget parse(StringReader reader) throws CommandSyntaxException {
        if (reader.canRead() && reader.peek() == '@') {
            EntitySelectorParser parser = new EntitySelectorParser(reader, true);
            return new EntityTarget.SelectorTarget(parser.parse());
        }

        String raw = readTargetToken(reader);
        if (raw.isEmpty()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
        }

        UUID uuid = tryParseUuid(raw);
        if (uuid != null) {
            return new EntityTarget.UuidTarget(uuid);
        }

        try {
            return new EntityTarget.IdTarget(Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            
        }

        ResourceLocation id = ResourceLocation.tryParse(raw.contains(":") ? raw : "minecraft:" + raw);
        
        
        if (id != null && ForgeRegistries.ENTITY_TYPES.containsKey(id)) {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            if (type != null) {
                return new EntityTarget.TypeTarget(type);
            }
        }

        
        if (!raw.contains(":") && net.minecraftforge.fml.ModList.get().isLoaded(raw)) {
            return new EntityTarget.ModTarget(raw);
        }

        throw ERROR_UNKNOWN_TARGET.create(raw);
    }

    
    static String readTargetToken(StringReader reader) {
        int start = reader.getCursor();
        while (reader.canRead() && isAllowedInTarget(reader.peek())) {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    private static boolean isAllowedInTarget(char c) {
        return (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'z')
                || (c >= 'A' && c <= 'Z')
                || c == '_' || c == '-' || c == '.' || c == '+' || c == ':' || c == '/';
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        if (remaining.startsWith("@")) {
            for (String selector : SELECTOR_EXAMPLES) {
                if (selector.startsWith(remaining)) builder.suggest(selector);
            }
            return builder.buildFuture();
        }

        String prefix = remaining.toLowerCase(Locale.ROOT);
        if (context.getSource() instanceof CommandSourceStack source) {
            Entity self = source.getEntity();
            for (Entity entity : source.getLevel().getAllEntities()) {
                if (entity == self) continue;
                if (entity.distanceToSqr(source.getPosition()) > 64.0D * 64.0D) continue;
                String uuid = entity.getUUID().toString();
                if (prefix.isEmpty() || uuid.toLowerCase(Locale.ROOT).startsWith(prefix)) builder.suggest(uuid);
            }
        }

        
        for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            String full = id.toString();
            if (prefix.isEmpty() || full.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(full);
            } else if ("minecraft".equals(id.getNamespace()) && id.getPath().startsWith(prefix)) {
                builder.suggest(id.getPath());
            }
        }

        
        for (String namespace : entityNamespaces()) {
            if (prefix.isEmpty() || namespace.startsWith(prefix)) builder.suggest(namespace);
        }

        return builder.buildFuture();
    }

    
    private static Set<String> entityNamespaces() {
        Set<String> cached = entityNamespaces;
        if (cached == null) {
            Set<String> namespaces = new TreeSet<>();
            for (ResourceLocation id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
                namespaces.add(id.getNamespace());
            }
            cached = namespaces;
            entityNamespaces = namespaces;
        }
        return cached;
    }

    private static UUID tryParseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
