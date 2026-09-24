package org.tdddd.eej.impl.datagen.gen.lang;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import org.tdddd.eej.impl.eej;
import org.tdddd.eej.impl.registry.EejBlocks;
import org.tdddd.eej.impl.registry.EejItems;


public class EejLangCN extends LanguageProvider {
    public EejLangCN(PackOutput output, String locale) {
        super(output, eej.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        
        add("itemGroup." + eej.MODID + ".main_tab", "终末-元素奇旅");

        
        add(EejBlocks.PACKED_MUD_PEDESTAL.get(), "泥坯祭台");
        add(EejBlocks.PACKED_MUD_ALTAR_STONE.get(), "泥坯祭坛石");
        add(EejItems.SMALL_ITEM_FRAME.get(), "小型物品过滤展示框");

        
        add("category.eej.altar_crafting", "祭坛合成");

        
        add("item.small_item_frame.added", "已记录：%s");
        add("item.small_item_frame.max_reached", "无法添加（已达上限或已存在）");
        add("item.small_item_frame.no_offhand", "副手无物品");
        add("item.small_item_frame.count", "已存 %d/%d");

        
        add("commands.eej.enchantment.success", "已为 %s 个生物施加 %s（等级 %s，持续 %s）");
        add("commands.eej.enchantment.removed", "已移除 %s 个生物身上的 %s");
        add("commands.eej.enchantment.cleared", "已清空 %s 个生物身上的全部魔咒");
        add("commands.eej.enchantment.unknown", "未知附魔：%s");
        add("commands.eej.enchantment.unknown_target", "未知的实体目标：%s（可用实体选择器、UUID、数字实体id、实体类型id 或 模组id）");
        add("commands.eej.enchantment.none", "没有找到可生效的目标生物（目标必须是生物）");
        add("commands.eej.enchantment.no_effect", "没有需要变更的目标：%s 个生物已拥有同级或更高级的该附魔（持续时间也不短于本次），或身上本来就没有该附魔");
        add("commands.eej.enchantment.permanent", "永久");
        add("commands.eej.enchantment.seconds", "%s 秒");

        
        
        add("altar.eej.no_material", "祭台上没有可用的材料");
        add("altar.eej.no_recipe", "没有匹配的祭坛配方");
        add("altar.eej.points_insufficient", "祭坛点数不足（需要 %s，当前 %s）");
        add("altar.eej.not_main_pedestal", "请对主祭台操作（%s %s %s）");
        add("altar.eej.not_main_pedestal_unknown", "这不是主祭台");
        add("altar.eej.structure_invalid", "祭坛结构无效");
        add("altar.eej.structure_invalid_reason", "祭坛结构无效：%s");
        add("altar.eej.filter_rejected", "该物品不被祭台过滤器接受（%s）");
        add("altar.eej.filter_cleared", "已清除该祭台的过滤器");

        
        add("jei.eej.altar_crafting.hint", "把材料放在任意祭台上，顺序无关");

        
        add("category.eej.soul_fire_purification", "灵魂火净化");
        add("jei.eej.soul_fire_purification.explode", "接触火焰时立即爆炸（灵魂火中额外施加失明 I）");

        
        add("commands.eej.island.info", "区块 %s,%s：底层基岩 %s，3×3 非真空岛底层合计 %s，%s");
        add("commands.eej.island.set.vacuum", "已将区块 %s,%s 设为真空岛区块");
        add("commands.eej.island.set.nonvacuum", "已将区块 %s,%s 设为非真空岛区块");
        add("commands.eej.island.status.vacuum", "永久真空岛");
        add("commands.eej.island.status.void", "虚空区块");
        add("commands.eej.island.status.nonvacuum", "非真空岛区块");
        add("commands.eej.island.unknown", "未判定");
        add("commands.eej.island.needs_pos", "控制台执行时必须指定坐标");    }
}
