# End:Element Journey (eej)

`eej`（模组 id：`eej`，英文名 **End:Element Journey**，中文名 **终末-元素奇旅**）是从 EPCA 中分离出来的**必选前置模组**，负责：

* 祭坛方块（`eej:packed_mud_pedestal` 泥坯祭台 / `eej:packed_mud_altar_stone` 泥坯祭坛石）
* 小型物品过滤展示框（`eej:small_item_frame`）——祭台的过滤条件道具与侧面过滤器图标
* 祭坛结构的扫描与校验、祭坛点数统计
* 祭坛（仪式）合成：用祭台上存放的物品匹配原版工作台配方
* 祭坛合成的 JEI 分类（`eej:altar_crafting`）

献祭仪式等与祭坛本体无关的交互**不在此模组中**，由 EPCA 通过下面的扩展接口注入。

## 构建

```bash
./gradlew build
```

产物为 `build/libs/eej-1.0.0.jar`。EPCA 以「本地 libs」的方式依赖它：

1. 在 `eej` 目录执行 `gradlew build`
2. 把 `eej/build/libs/eej-1.0.0.jar` 复制到 `epca/libs/`
3. `epca/build.gradle` 中已声明 `implementation fg.deobf("org.tdddd.eej:eej:1.0.0")`
4. `epca/src/main/resources/META-INF/mods.toml` 中已声明 `[[dependencies.epca]] modId = "eej" mandatory = true`

## 资源文件与数据生成

模组资源分两部分：

| 位置 | 内容 | 维护方式 |
|---|---|---|
| `src/main/resources` | 模组图标 `eej_icon.png`、`pack.mcmeta`、`META-INF/mods.toml`、纹理 `assets/eej/textures/**`、祭台的自定义模型（`blockstates/packed_mud_pedestal.json`、`models/block/packed_mud_pedestal.json`、`models/block/altar_pedestal_level_1.json`、`models/item/packed_mud_pedestal.json`） | 手动维护 |
| `src/generated/resources` | 语言文件（`zh_cn` / `en_us`）、`packed_mud_altar_stone` 的方块状态与模型、`small_item_frame` 物品模型、方块标签（`minecraft:mineable/pickaxe`、`eej:pedestals`、`eej:altar_stones`）、配方与配方解锁进度、方块战利品表、`data/eej/altar_points/eej_altar_points.json` | 数据生成 |

重新生成：

```bash
./gradlew runData
```

对应实现（`org.tdddd.eej.impl.datagen`）：

* `EejDataGenEvent` —— 数据生成入口（`GatherDataEvent`）
* `gen/EejBlockStateData` —— 方块状态与模型；`packed_mud_pedestal` 因使用自定义 BBmodel 被列入 `MANUAL_BLOCKS` 跳过
* `gen/EejItemModelData` —— 非方块物品模型（小型物品展示框）
* `gen/lang/EejLangCN` / `EejLangEN` —— 中英语言文件
* `gen/EejBlockTagData` —— 挖掘工具标签与祭坛结构标签
* `gen/EejRecipeData` —— 配方（`RecipeCategory.TOOLS` 会写成 JSON 的 `equipment` 分类，与分离前一致）
* `gen/EejLootTableData` —— 方块战利品表（自掉落）
* `EejAltarPointData` —— 祭坛点数默认值，直接读取各子类的 `getAltarPoints()`

新增祭坛方块后，只要补上纹理并运行 `runData`，语言键、模型、标签、配方/战利品表与祭坛点数就会自动生成。

## 方块父类与子类

所有祭坛方块都继承同一个父类 `org.tdddd.eej.impl.altar.AbstractAltarBlock`（继承 `BaseEntityBlock`），
父类持有全部公共逻辑（结构扫描、校验、主祭台维护、祭坛合成、交互派发）。
子类之间只有以下区别：

| 子类 | 方块 id | 祭坛点数 | 单个结构数量上限 | 纹理 | 能否存放物品 |
|---|---|---|---|---|---|
| `PackedMudPedestal` | `eej:packed_mud_pedestal` | 30 | 21 | `eej:block/packed_mud_pedestal` | 是（有方块实体） |
| `PackedMudAltarStone` | `eej:packed_mud_altar_stone` | 20 | 不限 | `eej:block/packed_mud_altar_stone` | 否（只提供点数） |

祭坛点数由子类的 `getAltarPoints()` 提供默认值，数据包 `data/<namespace>/altar_points/*.json` 可以按方块 id 覆盖；
单个结构中的数量上限由 `getMaxCountInStructure()` 决定。

### 新增一个祭坛方块

1. 新建子类，只声明点数与数量上限（纹理、方块 id 属于资源与注册）：

```java
public class MyAltarBlock extends AbstractAltarBlock {
    public MyAltarBlock() {
        super(50, 8, Properties.of().strength(1.0F, 3.0F).sound(SoundType.PACKED_MUD).randomTicks());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null; // 需要存放物品时返回对应方块实体
    }
}
```

2. 在 `EejBlocks` / `EejItems` 中注册
3. 加入 `data/eej/tags/blocks/pedestals.json`（可存放物品）或 `altar_stones.json`（只提供点数）
4. 按需添加 blockstate/model/纹理/loot table/配方，并可选地在 `data/eej/altar_points/` 中写默认点数

## 供其它模组使用的 API（`org.tdddd.eej.api`）

* `AltarInteractionRegistry` + `AltarInteractionHandler`
  注册祭坛右键交互。祭坛方块在 `use()` 最开始按注册顺序调用处理器，返回 `InteractionResult.PASS` 表示继续交给下一个处理器/祭坛默认逻辑。
  EPCA 的献祭仪式与调试击杀棒就是这样接入的。
* `AbstractAltarBlock#findAltarStructure(Level, BlockPos)`
  扫描并校验以某个祭台为起点的祭坛结构，返回 `AltarStructure`。
* `AltarStructure`
  只读结构快照：`allPositions` / `totalPoints` / `pedestalCount` / `pedestalPositions` / `pedestalsWithItem` / `isValid` / `invalidReason` / `mainPedestal` / `isValidForCrafting`。
* `AltarItemContainer`
  祭台方块实体暴露的物品/过滤条件接口（`hasItem` / `getItem` / `setItem` / `clearItem` / `getFilterData` / `setFilterData` / `isMainPedestal` / `matchesFilter`）。
* `AltarBlockTags`
  `PEDESTAL_TAG`（`eej:pedestals`）与 `ALTAR_STONE_TAG`（`eej:altar_stones`）。

祭台的过滤器交互（手持 `eej:small_item_frame` 右键祭台写入过滤条件、清空主祭台标记）由 `AbstractAltarBlock` 内置处理，
外部模组不需要接入；祭台侧面的过滤器图标由 `PedestalItemRenderer` 直接使用 `eej:small_item_frame` 绘制。

### 结构 NBT

世界生成结构 `.nbt` 的方块调色板与方块实体 id 必须使用 `eej:packed_mud_pedestal` / `eej:packed_mud_altar_stone`，
EPCA 自带的 `abandoned_cake_workshop*.nbt` 已同步改为新 id。

### EPCA 侧接入示例

```java
public class EpcaAltarInteractionHandler implements AltarInteractionHandler {
    @Override
    public InteractionResult onAltarUse(Level level, BlockPos pos, BlockState state,
                                        Player player, InteractionHand hand, ItemStack heldItem) {
        if (tryPerformSacrifice(level, pos, player)) return InteractionResult.SUCCESS;
        return InteractionResult.PASS;
    }
}

// 在模组构造器中
AltarInteractionRegistry.register(new EpcaAltarInteractionHandler());
```

## 生物魔咒（`/eej_enchantment`）

给任意生物施加任意附魔：**不需要手持武器、也不需要穿戴护甲**，只要身上挂有该附魔即可生效；
同一个生物可以同时拥有任意多个不同附魔，每种独立生效。

### 指令

```
/eej_enchantment <目标实体id> <附魔id> <魔咒持续时间(秒)> <魔咒等级>
/eej_enchantment clear <目标实体id>
```

示例：

```
/eej_enchantment @e[type=zombie,limit=3] minecraft:sharpness 60 5
/eej_enchantment minecraft:skeleton minecraft:protection 0 4        # 0 秒 = 永久
/eej_enchantment epca:infested_zombie minecraft:sharpness 60 5      # 模组生物：命名空间实体 id
/eej_enchantment epca minecraft:sharpness 60 5                      # 模组 id：该模组注册的全部生物
/eej_enchantment 069a79f4-44e9-4726-a5be-fca90e38aaf5 minecraft:fire_aspect 30 2
/eej_enchantment 123 minecraft:thorns 120 3
/eej_enchantment clear @e[type=zombie]
```

* **目标实体id** 支持五种写法（均有 Tab 补全）：
  1. 实体选择器：`@e` / `@p` / `@s` / `@r` / `@a` / 玩家名（可用完整选择器参数，如 `@e[type=minecraft:pig,tag=xxx]`）
  2. 实体 UUID：`069a79f4-44e9-4726-a5be-fca90e38aaf5`
  3. 数字实体 id：`123`
  4. 实体类型 id（含模组命名空间）：`minecraft:zombie`、`epca:infested_zombie`，也可省略命名空间写 `zombie`；作用于当前维度该类型的全部已加载生物
  5. 模组 id：`epca`；作用于该模组注册的全部生物类型（当前维度中已加载的）
* **持续时间** 单位为秒，`0` 或负数表示**永久**
* **魔咒等级** `<= 0` 表示移除该附魔
* 同一附魔重复施加：等级取**最大值**，持续时间取**更长的一方**（永久优先）；若两者都不需要变化，指令会提示“没有需要变更的目标”
* 需要权限等级 2（与 `/effect` 相同）
* 实现注意：Brigadier 的 `readUnquotedString()` 不接受命名空间分隔符 `:`，因此目标参数使用自定义 token 读取；
  另外 `minecraft:entity_type` 是**带默认值**的注册表（默认 `minecraft:pig`），未知 id 必须用 `containsKey` 判断，
  否则会静默变成猪。

### 生效范围

| 分类 | 附魔 |
|---|---|
| 攻击 | 锋利 / 亡灵杀手 / 节肢杀手（按目标类型加伤）、火焰附加、击退、抢夺、其它 `doPostAttack` 效果 |
| 防御 | 保护 / 火焰保护 / 爆炸保护 / 弹射物保护 / 摔落保护（EPF 减伤，上限 20）、荆棘等 `doPostHurt` 效果 |
| 状态与移动 | 水下呼吸（不再消耗氧气）、深海探索者、冰霜行者（走过水面结霜冰）、灵魂疾行、迅捷潜行 |
| 远程 | 生物射出的箭享有力、冲击、火矢 |

挖掘 / 钓鱼 / 弩 / 三叉戟 / 耐久 / 诅咒类附魔对生物本身没有可作用的行为，因此不会产生效果。

### 附魔光效

只要生物身上还有至少一条魔咒，就会渲染原版附魔闪光（`RenderType.entityGlint()`）：

* **Java 模型**：`RenderLivingEvent.Post` 触发时用同一个渲染器 + 附魔闪光缓冲区再渲染一遍（`impl/client`）
* **GeckoLib 模型**：GeckoLib 的 `GeoEntityRenderer` 不是 `LivingEntityRenderer`，因此走 GeckoLib 自己的
  `GeoRenderEvent.Entity.Post` / `ReplacedEntity.Post`，用 `GeoRenderer#reRender` 以附魔闪光重绘（`impl/compat/geckolib`）

GeckoLib 是**可选**兼容：`build.gradle` 里只有 `compileOnly` 依赖，代码只在检测到 `geckolib` 模组时才注册。

### 其它模组调用

```java
// 服务端施加：等级 5、持续 60 秒；持续时间传负数表示永久
MobEnchantmentApi.apply(livingEntity, Enchantments.SHARPNESS, 5, 20 * 60);
MobEnchantmentApi.remove(livingEntity, Enchantments.SHARPNESS);
MobEnchantmentApi.clear(livingEntity);

int level = MobEnchantmentApi.getLevel(livingEntity, Enchantments.THORNS);
boolean enchanted = MobEnchantmentApi.hasAny(livingEntity);
```

数据以 Capability（`EejCapabilities.MOB_ENCHANTMENTS` / `IMobEnchantments`）挂在生物身上，会随实体 NBT 持久化，
并通过 `MobEnchantmentSyncPacket` 同步到客户端（供光效渲染与客户端查询）。
