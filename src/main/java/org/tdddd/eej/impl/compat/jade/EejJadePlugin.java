package org.tdddd.eej.impl.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IBoxStyle;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;
import snownee.jade.api.ui.IProgressStyle;
import org.tdddd.eej.api.mana.IManaStorage;
import org.tdddd.eej.api.mana.ManaFormat;
import org.tdddd.eej.impl.altar.PackedMudPedestal;
import org.tdddd.eej.impl.altar.blockentity.PackedMudPedestalBlockEntity;
import org.tdddd.eej.impl.capability.EejCapabilities;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.mana.CreativeManaSourceBlock;
import org.tdddd.eej.impl.mana.blockentity.CreativeManaSourceBlockEntity;


/**
 * Jade integration for the mod's mana blocks: a pink mana bar (black when empty) with the current
 * and maximum mana printed as pink text to its right.
 *
 * <p>Registered through the {@code @WailaPlugin} annotation, exactly like the sibling tree's
 * {@code EPCAJadePlugin} - Jade 1.20.1 scans mod classes for that annotation, there is no services
 * file to edit. The provider is registered for both the creative mana source and the packed mud
 * altar; the altar additionally reports the client-side red flash used when a craft is refused for
 * lack of mana.</p>
 */
@WailaPlugin
public class EejJadePlugin implements IWailaPlugin {

    public static final ResourceLocation MANA_INFO = new ResourceLocation(eej.MODID, "mana_info");

    
    private static final String TAG_MANA = "eej:Mana";
    private static final String TAG_MAX_MANA = "eej:MaxMana";

    
    private static final int COLOR_MANA_PINK = 0xFFFFB6C1;
    
    private static final int COLOR_MANA_EMPTY = 0xFF000000;
    
    private static final int COLOR_FLASH_RED = 0xFFFF2020;
    
    private static final int COLOR_MANA_EMPTY_FLASH = 0xFF4A0000;

    private static final float BAR_WIDTH = 80.0F;
    private static final float BAR_HEIGHT = 10.0F;

    
    private static final float TEXT_GAP = 4.0F;

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, PackedMudPedestalBlockEntity.class);
        registration.registerBlockDataProvider(ManaBlockProvider.INSTANCE, CreativeManaSourceBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ManaBlockProvider.INSTANCE, PackedMudPedestal.class);
        registration.registerBlockComponent(ManaBlockProvider.INSTANCE, CreativeManaSourceBlock.class);
        try {
            registration.addConfig(MANA_INFO, true);
        } catch (IllegalArgumentException ignored) {
            
        }
    }

    
    public static class ManaBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
        public static final ManaBlockProvider INSTANCE = new ManaBlockProvider();

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            int mana;
            int max;
            CompoundTag serverData = accessor.getServerData();
            if (serverData.contains(TAG_MANA) && serverData.contains(TAG_MAX_MANA)) {
                mana = serverData.getInt(TAG_MANA);
                max = serverData.getInt(TAG_MAX_MANA);
            } else {
                
                IManaStorage storage = manaOf(accessor.getBlockEntity());
                if (storage == null) {
                    return;
                }
                mana = storage.getManaStored();
                max = storage.getMaxManaStored();
            }

            float flash = flashOf(accessor.getBlockEntity());
            float ratio = max > 0 ? Mth.clamp((float) mana / (float) max, 0.0F, 1.0F) : 0.0F;

            IElementHelper helper = tooltip.getElementHelper();
            IProgressStyle style = helper.progressStyle()
                    .color(mix(COLOR_MANA_PINK, COLOR_FLASH_RED, flash),
                            mix(COLOR_MANA_EMPTY, COLOR_MANA_EMPTY_FLASH, flash));

            
            // canDecrease = true: Jade's progress tracker is keyed by plugin uid and shared by every
            // mana block, so it must be allowed to chase the value downwards too - otherwise looking
            // at a full creative source and then at a drained altar would keep the bar full.
            IElement bar = helper.progress(ratio, Component.empty(), style, IBoxStyle.Empty.INSTANCE, true)
                    .size(new Vec2(BAR_WIDTH, BAR_HEIGHT));
            tooltip.add(bar);

            
            int textColor = mix(COLOR_MANA_PINK, COLOR_FLASH_RED, flash);
            tooltip.append(helper.text(Component.literal(ManaFormat.bar(mana, max))
                            .withStyle(style2 -> style2.withColor(TextColor.fromRgb(textColor))))
                    .translate(new Vec2(TEXT_GAP, 1.0F)));
        }

        @Override
        public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
            IManaStorage storage = manaOf(accessor.getBlockEntity());
            if (storage == null) {
                return;
            }
            tag.putInt(TAG_MANA, storage.getManaStored());
            tag.putInt(TAG_MAX_MANA, storage.getMaxManaStored());
        }

        @Override
        public ResourceLocation getUid() {
            return MANA_INFO;
        }

        private static IManaStorage manaOf(BlockEntity blockEntity) {
            return blockEntity == null ? null
                    : blockEntity.getCapability(EejCapabilities.MANA_STORAGE).orElse(null);
        }

        private static float flashOf(BlockEntity blockEntity) {
            return blockEntity instanceof PackedMudPedestalBlockEntity pedestal
                    ? pedestal.getManaFlashIntensity() : 0.0F;
        }

        
        private static int mix(int base, int flashColor, float intensity) {
            float t = Mth.clamp(intensity, 0.0F, 1.0F);
            if (t <= 0.0F) {
                return base;
            }
            if (t >= 1.0F) {
                return flashColor;
            }
            int red = (int) Mth.lerp(t, (base >> 16) & 0xFF, (flashColor >> 16) & 0xFF);
            int green = (int) Mth.lerp(t, (base >> 8) & 0xFF, (flashColor >> 8) & 0xFF);
            int blue = (int) Mth.lerp(t, base & 0xFF, flashColor & 0xFF);
            return 0xFF000000 | (red << 16) | (green << 8) | blue;
        }
    }
}
