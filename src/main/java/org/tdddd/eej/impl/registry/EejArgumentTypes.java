package org.tdddd.eej.impl.registry;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.tdddd.eej.impl.command.EntityTargetArgument;
import org.tdddd.eej.impl.eej;


public class EejArgumentTypes {
    public static final DeferredRegister<ArgumentTypeInfo<?, ?>> ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, eej.MODID);

    public static final DeferredHolder<ArgumentTypeInfo<?, ?>, ArgumentTypeInfo<?, ?>> ENTITY_TARGET =
            ARGUMENT_TYPES.register("entity_target", () -> ArgumentTypeInfos.registerByClass(
                    EntityTargetArgument.class,
                    SingletonArgumentInfo.contextFree(EntityTargetArgument::new)));

    public static void register(IEventBus eventBus) {
        ARGUMENT_TYPES.register(eventBus);
    }

    private EejArgumentTypes() {
    }
}
