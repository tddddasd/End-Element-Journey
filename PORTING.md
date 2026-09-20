# eej → Minecraft 26.1.2 / NeoForge 26.1.2.76 移植记录

本目录（`G:\epca\eej\eej-port\`）是 **eej**（mod id `eej`，"End:Element Journey" / 终末-元素奇旅）
从 **Minecraft 1.20.1 / Forge 47.2.0** 到 **Minecraft 26.1.2 / NeoForge 26.1.2.76**
（ModDevGradle 2.0.140 / Gradle 9.3.0 / Java 25）的移植工作副本。

## 状态

| 项 | 结果 |
|---|---|
| 源码覆盖 | **51 / 51** 个原始 java 文件全部移植（`src` 对比脚本确认无遗漏） |
| 新增文件 | **3** 个（见下文「新增文件」一节） |
| 资源 | 全部移植并针对 26.1.2 重写格式（见「资源」一节） |
| **javac 全量编译** | **0 errors / 16 warnings**（54 个源文件 → 67 个 class） |
| 真实 Gradle 构建 | **未运行**（本会话沙箱只允许写 `G:\epca\eej`，且任务明确要求不跑 Gradle） |

### 编译验证命令（可复现）

```powershell
$env:JAVA_TOOL_OPTIONS = "-Duser.language=en -Duser.country=US"
& "C:\Users\Administrator.DESKTOP-D39LTDF\.jdks\openjdk-25.0.2\bin\javac.exe" `
  -nowarn -proc:none -encoding UTF-8 -Xmaxerrs 300 `
  -cp "@G:\epca\eej\eej-port\cp-port.txt" `
  -d "G:\epca\eej\eej-port\out" `
  "@G:\epca\eej\eej-port\sources.txt"
```

- `cp-port.txt` = `G:\epca\eej\cp.txt`（296 个 jar）**去掉**里面那个 1.20.1 的
  `curse.maven:jei-238222:8677648` jar，并把 `G:\epca\eej\libs\jei-26.1.2-neoforge-29.37.0.99.jar`
  与 `G:\epca\eej\libs\geckolib-neoforge-26.1.2-5.5.2.jar` 放到**最前面**（共 299 条）。
  > 原 `cp.txt` 里含 1.20.1 的 JEI jar，会先把旧版 `IRecipeCategory` 解析进来导致
  > 「`GuiGraphics` not found / `RecipeType` 不兼容」等假错误——必须把它从 classpath 中剔除。
- `sources.txt` = 54 个 `.java` 的绝对路径清单。
- 结果：**0 errors**，16 条 `[removal]` 弃用警告（全部来自 NeoForge 把
  `IItemHandler`/`ItemStackHandler`/JEI 的 `RecipeType` 标记为 forRemoval，见下文「警告」）。

---

## 一、逐文件移植记录

> 说明：未特别提到的文件只做了机械替换（`ResourceLocation`→`Identifier`、
> `ForgeRegistries`→`BuiltInRegistries`/`Registries`、`.isClientSide`→`.isClientSide()`、
> `net.minecraftforge.*`→`net.neoforged.*`），行为不变。

### 1. 入口与注册表

#### `impl/eej.java`
- `@Mod` 构造器由「无参 + `FMLJavaModLoadingContext.get().getModEventBus()`」改为注入
  `(IEventBus modEventBus, ModContainer modContainer)`。
- `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`；改为 `NeoForge.EVENT_BUS.register(this)`
  + 两个 `@SubscribeEvent` 方法（原来用 `addListener(this::...)`，两种都可）。
- `onAddReloadListeners(AddReloadListenerEvent)` → `onAddReloadListeners(AddServerReloadListenersEvent)`，
  监听器带 key `eej:altar_points`。
- 新增：
  - `EejCapabilities.register(modEventBus)`（数据附件必须在模组总线注册）；
  - `EejDataComponents.register(modEventBus)`（自定义数据组件，见下）；
  - `modEventBus.addListener(EejDataGenEvent::gatherClientData / ::gatherServerData)`；
  - `modEventBus.addListener(PackedMudPedestalBlockEntity::registerCapabilities)`。
- `FMLEnvironment.dist == Dist.CLIENT` → `FMLEnvironment.getDist() == Dist.CLIENT`；
  `net.minecraftforge.fml.ModList` → `net.neoforged.fml.ModList`；
  `net.minecraftforge.api.distmarker.Dist` → `net.neoforged.api.distmarker.Dist`。
- `asResource(String)` 的 `new ResourceLocation(MODID, path)` → `Identifier.fromNamespaceAndPath(...)`。

#### `impl/registry/EejBlocks.java`
- `DeferredRegister.create(ForgeRegistries.BLOCKS, MODID)` → `DeferredRegister.createBlocks(MODID)`；
  `RegistryObject<Block>` → `DeferredBlock<Block>`。
- **注意**：因为两个方块类都多了一个接受 `Properties` 的构造器（26.1.2 的 `simpleCodec` 需要），
  `BLOCKS.register(name, PackedMudPedestal::new)` 会与方法重载
  `register(String, Function<Identifier,B>)` 产生歧义。这里显式写成
  `(Supplier<Block>) PackedMudPedestal::new` 消歧。

#### `impl/registry/EejItems.java`
- `DeferredRegister.create(ForgeRegistries.ITEMS, MODID)` → `DeferredRegister.createItems(MODID)`；
  `RegistryObject<Item>` → `DeferredItem<Item>`。`BlockItem(block, new Item.Properties())` 不变。

#### `impl/registry/EejBlockEntities.java`
- **`BlockEntityType.Builder.of(...).build(null)` 在 26.1.2 已被删除**，改为直接构造
  `new BlockEntityType<>(factory, blocks...)`（构造器仍是 public，数据修正器参数没了）。
- `ForgeRegistries.BLOCK_ENTITY_TYPES` → `Registries.BLOCK_ENTITY_TYPE`。

#### `impl/registry/EejCreativeTabs.java`
- `RegistryObject` → `DeferredHolder`；其余（`CreativeModeTab.builder()` / `displayItems`）不变。

#### `impl/registry/EejArgumentTypes.java`
- `ForgeRegistries.COMMAND_ARGUMENT_TYPES` → `Registries.COMMAND_ARGUMENT_TYPE`。
- `ArgumentTypeInfos.registerByClass(...)` + `SingletonArgumentInfo.contextFree(...)` 用法不变。

#### `impl/registry/EejClientSetup.java`
- `@Mod.EventBusSubscriber(modid, bus = MOD, value = CLIENT)` + `FMLClientSetupEvent`
  → `@EventBusSubscriber(modid, value = CLIENT)` + **`EntityRenderersEvent.RegisterRenderers`**。
  26.1.2 有专门的方块实体渲染器注册事件，不再需要 `event.enqueueWork(...)`。

#### `impl/registry/EejDataComponents.java` 【新增】
见「新增文件」。

#### `impl/registry/EejEnchantments.java` 【新增】
见「新增文件」。

### 2. 祭坛 API（`api/`）

| 文件 | 改动 |
|---|---|
| `api/AltarStructure.java` | **逐字未改**（只用 `BlockPos`）。 |
| `api/AltarInteractionHandler.java` | **签名逐字未改**（`InteractionResult.PASS` 在 26.1.2 仍是常量，`InteractionResult` 变成了 sealed interface 但常量保留）。 |
| `api/AltarInteractionRegistry.java` | **逐字未改**。 |
| `api/AltarBlockTags.java` | `ResourceLocation` → `Identifier`；标签路径不变（`eej:pedestals` / `eej:altar_stones`）。 |
| `api/AltarItemContainer.java` | `new ResourceLocation(filter)` → `Identifier.parse(filter)`；`builtInRegistryHolder().key().location()` → `...key().identifier()`。**接口方法签名一字未改**。 |

### 3. 祭坛实现（`impl/altar/`）

#### `impl/altar/AbstractAltarBlock.java`（改动最大的两个文件之一）
- **右键交互被拆分**：1.20.1 的 `Block#use(state, level, pos, player, hand, hit)` 在 26.1.2
  拆成 `useItemOn(itemStack, state, level, pos, player, hand, hit)` 与
  `useWithoutItem(state, level, pos, player, hit)`。原逻辑的三段
  （外部处理器 → 展示框/潜行取回 → 放置物品）保留在 `useItemOn`；空手时的
  「主祭台合成」搬进 `useWithoutItem`，由 `useItemOn` 返回 `InteractionResult.TRY_WITH_EMPTY_HAND` 转交。
  → **行为等价**：空手右键仍走「取回（潜行）/ 合成」两条路。
- **合成 API 重写**：
  - `TransientCraftingContainer` / `CraftingContainer` / `AbstractContainerMenu` 匿名子类
    **全部删除**（这些类在 26.1.2 不存在），改用 `CraftingInput.of(3, 3, List<ItemStack>)`。
  - `recipe.getIngredients()` → `recipe.placementInfo().ingredients()`
    （26.1.2 的 `Recipe` 接口不再有 `getIngredients()`，改为 `PlacementInfo`）。
  - `recipe.assemble(container, registryAccess)` → `recipe.assemble(CraftingInput)`
    （不再需要 registryAccess 参数）。
  - `recipe.getResultItem(registryAccess)` 已删除；结果直接用 `assemble` 计算（与原实现一致，
    原来也只是用 `assemble` 得到结果）。
  - `matchedRecipe.getRemainingItems(container)` → `getRemainingItems(CraftingInput)`。
- **配方来源变化（⚠ 需注意）**：
  1.20.1 用 `level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)`。
  26.1.2 的 `Level#recipeAccess()` 返回的 `RecipeAccess` 只暴露
  `propertySet` / `stonecutterRecipes`，**拿不到配方列表**。因此改为
  `level.getServer().getRecipeManager().getRecipes()` 再按
  `holder.value() instanceof CraftingRecipe && !isSpecial()` 过滤。
  - 配方顺序与原实现同为「注册表迭代顺序」。
  - 客户端侧 `level.getServer()` 为 null → 返回空列表 → `performCrafting` 返回 false，
    与原实现「客户端没有配方表」的表现一致（客户端本来就只会走 `InteractionResult.SUCCESS`
    的回显路径）。
- `onRemove(state, level, pos, newState, moving)` **已删除**，改用
  `affectNeighborsAfterRemoval(state, ServerLevel, pos, moving)`；
  因为新签名拿不到 `newState`，改用 `level.getBlockState(pos).is(state.getBlock())` 判断
  「确实被替换/移除」。掉落祭台内物品的行为保留。
- `getAnalogOutputSignal(state, level, pos)` → `getAnalogOutputSignal(state, level, pos, Direction)`
  （26.1.2 新增 `Direction` 参数），逻辑不变。
- `matchesTarget` 里 `builtInRegistryHolder().tags()` 不变（`Holder.Reference#tags()` 仍在）。

#### `impl/altar/PackedMudPedestal.java` / `PackedMudAltarStone.java`
- `BlockBehaviour.Properties` 的 `strength/sound/mapColor/randomTicks/noOcclusion/
  isViewBlocking/isSuffocating/pushReaction/requiresCorrectToolForDrops` **全部保留**，属性完全一致。
- 各新增一个 `public Xxx(Properties properties)` 构造器 + `CODEC` 静态字段 + `codec()` 覆写：
  26.1.2 的 `BaseEntityBlock` 新增抽象方法
  `protected MapCodec<? extends BaseEntityBlock> codec()`（注意返回类型是
  `? extends BaseEntityBlock`，不是 `? extends Block`）。
  用 `simpleCodec(Xxx::new)` 提供，因此需要接受 `Properties` 的构造器；
  方块属性在构造器里固定，传入的 `properties` 直接透传（与原版 `simpleCodec` 用法一致）。

#### `impl/altar/AltarPointManager.java`
- `ResourceLocation` → `Identifier`；`ForgeRegistries.BLOCKS` → `BuiltInRegistries.BLOCK`
  （`getKey`/`getValue` 语义相同，`getValue` 现在可空）。
- 仍然实现 `ResourceManagerReloadListener`（它是 `PreparableReloadListener` 的子类型），
  可直接交给 `AddServerReloadListenersEvent#addListener(Identifier, listener)`。
- 数据包路径 `data/<ns>/altar_points/*.json` **未变**。

#### `impl/altar/blockentity/PackedMudPedestalBlockEntity.java`（改动最大的两个文件之一）
- **NBT → ValueIO**：
  - `load(CompoundTag)` 覆写 → `loadAdditional(ValueInput)`；
  - `saveAdditional(CompoundTag)` → `saveAdditional(ValueOutput)`。
  - **存档字段名与结构保持**：物品栏整块写进 `inventory` 子复合
    （`ItemStackHandler#serialize/deserialize` 内部字段是 `Items` / `Size`），
    过滤列表写进 `filterData`（字符串列表）。
  - `tag.contains("filterData")` → `input.listOrEmpty("filterData", Codec.STRING)`。
- **更新标签**：
  - `getUpdateTag()` → `getUpdateTag(HolderLookup.Provider)`，
    实现为 `saveCustomOnly(registries)`；
  - `handleUpdateTag(CompoundTag)` → `handleUpdateTag(ValueInput)`，
    实现为 `loadWithComponents(input)`；
  - `onDataPacket(Connection, ClientboundBlockEntityDataPacket)` → 覆写 NeoForge
    `IBlockEntityExtension#onDataPacket(Connection, ValueInput)`。
- **Forge Capability → NeoForge 新形态 Capability**：
  - 1.20.1 在 BlockEntity 上覆写 `getCapability` / `invalidateCaps` 并用
    `LazyOptional<IItemHandler>`；26.1.2 **没有这两个可覆写点**。
  - 改为 `RegisterCapabilitiesEvent#registerBlockEntity(Capabilities.Item.BLOCK, type, provider)`。
  - ⚠ **能力类型变了**：26.1.2 的 `Capabilities.Item.BLOCK` 是
    `BlockCapability<ResourceHandler<ItemResource>, Direction>`，
    **不再是 `IItemHandler`**。因此新增了 `SideFilteredResourceHandler`（实现
    `ResourceHandler<ItemResource>`），行为对齐 1.20.1 的 `SideFilteredItemHandler`：
    上/下面不可插入、只有下面可抽取、未锁定且槽位为空才允许插入、非主祭台时按过滤列表校验。
  - 旧的 `SideFilteredItemHandler`（`IItemHandler`）**保留**为内部兼容视图，
    并提供 `getSideHandler(Direction)` 取用；但**不再通过能力系统对外暴露**
    （外部模组若查询旧 `IItemHandler` 能力会拿不到东西——这是平台层面的变化，无法两全，见「风险」）。
- `ItemStackHandler` **仍然存在**（26.1.2 标记 `@Deprecated(forRemoval)`，指向
  `ItemStacksResourceHandler`），物品栏逻辑逐字保留。
- **网络同步**：`EejNetwork.INSTANCE.toVanillaPacket(pkt, PLAY_TO_CLIENT)` +
  手工遍历 `chunkMap.getPlayers(...)` 已随 `SimpleChannel` 删除，
  改为 `PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos, payload)`。
  `ChunkPos` 在 26.1.2 不再有 `(BlockPos)` 构造器（record，只有 `(int,int)`），
  用 `worldPosition.getX() >> 4` / `getZ() >> 4`。
- `level.isClientSide` → `level.isClientSide()`。

#### `impl/altar/item/SmallItemFrame.java`
- **物品 NBT 彻底移除**：`ItemStack#hasTag/getTag/getOrCreateTag` 在 26.1.2 全部不存在。
  过滤列表改存自定义组件 **`eej:item_ids`**（`DataComponentType<List<String>>`，
  见 `EejDataComponents`）。`getItemIds/setItemIds/addItemId` 的**签名和语义保留**。
  - ⚠ **旧存档里的 `item_ids` NBT 不会迁移**（展示框会显示为空），需要重新记录。
- `appendHoverText(ItemStack, Level, List<Component>, TooltipFlag)` →
  `appendHoverText(ItemStack, Item.TooltipContext, TooltipDisplay, Consumer<Component>, TooltipFlag)`。
- `player.displayClientMessage(Component, boolean)` **已被删除**，
  改用 `player.sendSystemMessage(Component)`。
  ⚠ **可感知差异**：原来是 actionbar 提示（物品栏上方），现在进入聊天栏。
- `level.isClientSide` → `level.isClientSide()`。

#### `impl/altar/client/PedestalItemRenderer.java`（**重写**）
1.20.1 形态：`BlockEntityRenderer<T>#render(be, partialTick, poseStack, bufferSource, light, overlay)`
+ `ItemRenderer#renderStatic(...)`。

26.1.2 形态：`BlockEntityRenderer<T, S extends BlockEntityRenderState>`，
拆成 **`extractRenderState`**（抽取）与 **`submit`**（提交）两阶段：
- 物品不再由 `ItemRenderer` 画，改为 `ItemModelResolver#updateForTopItem(...)` 解析进
  `ItemStackRenderState`，再由 `ItemStackRenderState#submit(poseStack, submitNodeCollector, light, overlay, outline)` 提交；
- `MultiBufferSource` → `SubmitNodeCollector`；
- 光照来自 `state.lightCoords`（`BlockEntityRenderState.extractBase` 填好），不再是方法参数；
- 需要一个自定义渲染状态类 → **新增 `impl/altar/client/PedestalRenderState.java`**。
- 几何变换（`translate(0.5, 1.2, 0.5)`、`rotation = (gameTime + partialTick) * 2.0f`、
  四个侧面的 `y=0.6875 / offset=0.34375`、`YP(angle)` + `XP(180)` + `ZP(180)`）
  **与 1.20.1 逐字一致**，因此视觉结果应当相同。
- ⚠ 唯一无法完全等价的点：物品模型现在只在**抽取阶段**解析一次（26.1.2 的预期用法），
  而 1.20.1 是每帧 `renderStatic`。对静态展示物品没有可见差异。
- `Minecraft.getInstance().level` 为 null 时做了保护（原来会 NPE）。

### 4. 生物魔咒系统

#### `api/mob/MobEnchantment.java` / `IMobEnchantments.java` / `MobEnchantmentApi.java` / `MobEnchantments.java`
- **附魔类型 `Enchantment` → `Holder<Enchantment>`（被迫改）**：
  1.20.1 的 `Enchantments.SHARPNESS` 是 `Enchantment` 常量；26.1.2 把它数据化/注册表化了——
  `Enchantments.XXX` 现在是 `ResourceKey<Enchantment>`，任何附魔引用都必须是 `Holder<Enchantment>`。
  类名、方法名、枚举名、`PERMANENT` 常量、等级/持续时间语义**全部保持**。
- `IMobEnchantments extends INBTSerializable<CompoundTag>` → **`extends ValueIOSerializable`**
  （NeoForge 删除了 `INBTSerializable`）。
  - **同时保留 `save()/load(CompoundTag)` 两个方法**（接口里仍在），因为它们被网络负载与
    `MobEnchantmentProvider` 使用，且能保持 1.20.1 的 NBT 结构。
  - 新增 `serialize(ValueOutput)` / `deserialize(ValueInput)`：把整份 `save()` 的结果
    用 `ValueOutput.store(DATA_KEY, CompoundTag.CODEC, tag)` 原样写入，
    `DATA_KEY = "eej_mob_enchantments"`。
    → **NBT 内部结构（`Enchantments` 列表 + 每条 `id`/`level`/`ticks`）与 1.20.1 完全一致**，
    只是外层容器从「Forge capability」变成「数据附件」。
- **附魔注册表解析（被迫新增）**：26.1.2 的附魔是 **datapack 注册表**，
  `BuiltInRegistries` 里**没有** `ENCHANTMENT`（只有 `ENCHANTMENT_*_TYPE` 辅助注册表），
  必须经 `RegistryAccess` 解析 → 新增 `impl/registry/EejEnchantments.java`。
  - `MobEnchantments.load` 用 `getListOrEmpty` / `getCompoundOrEmpty` / `getStringOr`
    （`CompoundTag#getList(String,byte)`、`getCompound(int)` 的签名都变了）。
  - `Holder.Reference` 的 `equals/hashCode` 在 26.1.2 是**按 key** 实现的
    （NeoForge 为 `DeferredHolder` 兼容加的），所以 `Map<Holder<Enchantment>, ...>` 仍然可用，
    不同 `RegistryAccess` 实例解析出的同名 Holder 可以正确相等。
- `MobEnchantmentApi`：
  - `entity.getCapability(CAP).orElse(null)` → `entity.getData(EejCapabilities.MOB_ENCHANTMENTS)`
    （数据附件下缺失会自动创建默认值并返回，因此 `data == null` 判断恒假，为贴近原代码保留）；
  - `EejNetwork.INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(...), pkt)`
    → `PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload)`；
    `PacketDistributor.PLAYER` → `PacketDistributor.sendToPlayer(player, payload)`；
  - **`EejNetwork.INSTANCE`（`SimpleChannel`）字段已删除**（平台没有这个类了）。

#### `impl/capability/EejCapabilities.java`
- 从「`CapabilityManager.get(new CapabilityToken<>(){})`」改为「数据附件注册持有类」：
  `DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MODID)` 注册
  `AttachmentType<IMobEnchantments> MOB_ENCHANTMENTS`（名沿用 `mob_enchantments`），
  用 `AttachmentType.serializable(MobEnchantments::new)` 获得自动持久化。
  → **公共 API 变化**：原来的 `Capability<IMobEnchantments> MOB_ENCHANTMENTS` 字段
  现在是 `DeferredHolder<AttachmentType<?>, AttachmentType<IMobEnchantments>>`。

#### `impl/capability/EejCapabilityEvents.java`
- 1.20.1 是 `@Mod.EventBusSubscriber(bus = MOD)` + `RegisterCapabilitiesEvent#register(Class)`。
  26.1.2 的 `RegisterCapabilitiesEvent` 只用于**方块/实体/物品的能力提供者**，
  没有 `register(Class)` 这种「声明能力接口」的用法（能力不再需要声明）。
  → 该文件退化为**空壳类**（保留类名/包名以维持形状），不再监听任何事件。

#### `impl/capability/EejCapabilityAttachmentEvents.java`
- 1.20.1 是 `@Mod.EventBusSubscriber` + `AttachCapabilitiesEvent<Entity>`。数据附件**不需要逐实体附加**。
  → 退化为只保留 `public static final Identifier MOB_ENCHANTMENTS_ID`（1.20.1 里是 public 字段）。

#### `impl/capability/MobEnchantmentProvider.java`
- `implements ICapabilitySerializable<CompoundTag>`（含 `LazyOptional`）在附件模型下无对应概念。
  → 退化为轻量适配器：持有一份 `MobEnchantments`，保留
    `serializeNBT()` / `deserializeNBT(CompoundTag)` 两个方法，供按旧形态调用的外部代码使用。

#### `impl/effect/MobEnchantmentEvents.java`（**语义重写**）
26.1.2 把 `Enchantment` 的实例方法（`getDamageBonus` / `doPostAttack` / `getDamageProtection` /
`doPostHurt`）**全部删除**，改成数据驱动的 `EnchantmentEffectComponents` + `EnchantmentHelper`
（而且 `EnchantmentHelper` 只遍历「真正存在于装备栏物品上的附魔」）。因此本文件按等价公式重建：

| 能力 | 26.1.2 实现 | 等价性 |
|---|---|---|
| 锋利 | `0.5*level + 0.5` | 与 1.20.1 `getDamageBonus` 一致 |
| 亡灵杀手 / 节肢杀手 | `2.5*level`，用 `EntityTypeTags.UNDEAD` / `ARTHROPOD` 判定 | 一致（`MobType` 已被原版删除） |
| 火焰附加 | `victim.setRemainingFireTicks(level*4*20)` | 一致 |
| 击退 | `victim.push(...)` | 一致 |
| 抢夺 | 在 `LivingDropsEvent` 里按等级**追加复制**掉落物（每级 +1，最多 +3） | ⚠ **近似**：原版抢夺是「额外一次掉落判定」，这里只能做数量近似 |
| 保护家族 | 手写 EPF 公式（保护 1/lv、火/爆炸/弹射 2/lv、摔落 3/lv），`1 - min(20,epf)/25` | 一致（与原版 `getDamageProtection` 公式相同） |
| 荆棘 | 手写 `0.15*level` 概率 + `damageSources().thorns(victim)` | 一致 |
| 水下呼吸 | `setAirSupply(getMaxAirSupply())` | 一致 |
| 深海探索者 / 迅捷潜行 / 灵魂疾行 | 乘算加成累加进 `MOVEMENT_SPEED` 修饰符 | 一致 |
| **冰霜行者** | **未实现** | ❌ `FrostWalkerEnchantment#onEntityMoved` 已被原版删除，复现需要重写「走动放置霜冰」逻辑，超出等价移植范围（见「未能复现的行为」） |
| 力量（箭） | `arrow.setBaseDamage(0 + level*0.5 + 0.5)` | 一致（`AbstractArrow#getBaseDamage()` 已删除，改为从 0 起算；JoinLevel 时只处理一次，等价） |
| 火矢 | `setRemainingFireTicks(100*20)` | 一致 |
| **冲击** | **未实现** | ❌ `AbstractArrow#setKnockback(int)` 已删除 |

- 事件替换：
  - `LivingHurtEvent` → **`LivingIncomingDamageEvent`**（`getAmount/setAmount/getSource` 一一对应）。
  - `LivingDamageEvent` → **`LivingDamageEvent.Pre`**（`getNewDamage/setNewDamage`）。
  - `LivingEvent.LivingTickEvent` → **`EntityTickEvent.Post`**（`Pre` 可取消 tick，`Post` 是原本的语义位置）。
  - `LootingLevelEvent` → **已删除** → 改用 `LivingDropsEvent`（见上表）。
  - `PlayerEvent.StartTracking` / `EntityJoinLevelEvent` 仍然存在，用法不变。
- `AttributeModifier(UUID, String name, double, Operation.MULTIPLY_TOTAL)` →
  `AttributeModifier(Identifier, double, Operation.ADD_MULTIPLIED_TOTAL)`
  （record 化，name 参数被删除；`MULTIPLY_TOTAL` 改名为 `ADD_MULTIPLIED_TOTAL`）。
- `AttributeInstance#getModifier(UUID)` → `getModifier(Identifier)`；`removeModifier(UUID)` → `removeModifier(Identifier)`。
- `@Mod.EventBusSubscriber(modid)` → `@EventBusSubscriber(modid)`。

### 5. 命令

#### `impl/command/EejEnchantmentCommand.java`
- `ResourceLocationArgument` **已重命名为 `IdentifierArgument`**（用法相同）。
- `ForgeRegistries.ENCHANTMENTS.getValue(id)` → `source.registryAccess()
  .lookupOrThrow(Registries.ENCHANTMENT).get(id)`（datapack 注册表）。
- `enchantment.getFullname(level)`（实例方法）→ **`Enchantment.getFullname(Holder, level)`（静态）**。
- `source.hasPermission(2)` **已删除** → `Commands.hasPermission(new PermissionCheck.Require(
  Permissions.COMMANDS_GAMEMASTER))`（`PermissionLevel.GAMEMASTERS` == 2，与原语义一致）。
- 命令名、参数名、子命令 `clear`、返回的 changed 计数、所有 lang key **全部不变**。

#### `impl/command/EntityTarget.java`
- `ForgeRegistries.ENTITY_TYPES` → `BuiltInRegistries.ENTITY_TYPE`；`ResourceLocation` → `Identifier`。
- `source.getLevel().getAllEntities()`：`ServerLevel#getAllEntities()` 仍在（`Level` 上也有）。
- `EntitySelector#findEntities` 返回 `List<? extends Entity>`（原来是 `List<Entity>`），
  用 `new ArrayList<>(...)` 收窄。

#### `impl/command/EntityTargetArgument.java`
- `ForgeRegistries.ENTITY_TYPES.containsKey/getValue/getKeys` → `BuiltInRegistries.ENTITY_TYPE.*`（`keySet()`）。
- `net.minecraftforge.fml.ModList` → `net.neoforged.fml.ModList`。
- 自定义 token 读取、UUID/数字 id/类型 id/模组 id 的解析顺序、suggestion 逻辑**全部保留**。

### 6. 网络（**整体改写**）

#### `impl/network/EejNetwork.java`
- `NetworkRegistry.newSimpleChannel` + `SimpleChannel#registerMessage/messageBuilder` →
  `RegisterPayloadHandlersEvent#registrar(PROTOCOL_VERSION)` + `PayloadRegistrar`。
  - 协议版本号仍为 `"1"`。
  - 3 个负载：`pedestal_item` / `mob_enchantment`（playToClient）、
    `small_item_frame_data`（playToServer）。
  - **`public static final SimpleChannel INSTANCE` 字段删除**（平台已无 `SimpleChannel`）。
- 新增 `EejNetwork.id(String)` 工具（造 `Identifier`）。

#### `impl/network/PedestalItemSyncPacket.java` / `MobEnchantmentSyncPacket.java` / `SmallItemFrameDataPacket.java`
- 从 SimpleChannel 消息类改写为 **`CustomPacketPayload`**：新增 `TYPE` 与 `STREAM_CODEC`
  （`CustomPacketPayload.codec(::encode, ::new)`），`handle` 由
  `Supplier<NetworkEvent.Context>` 改为 `IPayloadContext`。
- 缓冲区 `FriendlyByteBuf` → **`RegistryFriendlyByteBuf`**。
- 客户端分支用 `FMLEnvironment.getDist() != Dist.CLIENT` 判断
  （`DistExecutor` 仍在，但 `Dist` 改包；这里用更直接的判断）。
- **同步字段与顺序保持不变**：
  - `PedestalItemSyncPacket`：`BlockPos` + 物品（原来写整份 NBT，现在用
    `ItemStack.STREAM_CODEC` 写物品组件，语义等价）。
  - `MobEnchantmentSyncPacket`：`entityId` / 条目数 / 每条 `(id, level, ticks)`。
    id 的编码从 `buf.writeResourceLocation` 改为 `buf.writeUtf(id.toString())`
    （因为 `ForgeRegistries` 已删除，且附魔成为注册表对象）；
    **线上格式与 1.20.1 不兼容**，但这是双端同版本的模组内部协议，无兼容性要求。
  - `SmallItemFrameDataPacket`：`hand` + `data` 字符串（不变）；
    写入内容由 NBT 键 `item_data` 改为写 `eej:item_ids` 组件
    （原实现**从未发送**过这个包，所以不改变既有行为）。

### 7. 客户端渲染

#### `impl/client/GlintBufferSource.java`
- `net.minecraft.client.renderer.RenderType` → **`net.minecraft.client.renderer.rendertype.RenderType`**（包被移动）；
  `RenderType.entityGlint()` → **`RenderTypes.entityGlint()`**（改成静态工厂类）。
- ⚠ **这个类在 26.1.2 的实体渲染路径上已不再被使用**：`LivingEntityRenderer#submit` 收的是
  `SubmitNodeCollector` 而不是 `MultiBufferSource`，无法再靠「换 buffer」叠加光效。
  保留它是为了维持 1.20.1 的类形状与工具语义（在仍走 `MultiBufferSource` 的自定义绘制路径里仍有意义）。

#### `impl/client/MobEnchantmentGlintHandler.java`（**重写，行为有差异**）
1.20.1：在 `RenderLivingEvent.Post` 里把 buffer 换成 `GlintBufferSource`，
再调用 `LivingEntityRenderer#render(...)` 重放整帧 → 整只生物（含所有 render layer）带附魔光效。

26.1.2：
- `RenderLivingEvent.Post<T, S, M>` 现在提供 `getRenderState()` / `getRenderer()` /
  `getPoseStack()` / `getSubmitNodeCollector()`（**没有 `MultiBufferSource` 了**）；
- `LivingEntityRenderState` **不携带实体引用**，无法直接查附件；
- 因此新增了一个 **NeoForge render state modifier** 扩展点：
  监听 `RegisterRenderStateModifiersEvent`，给 `LivingEntityRenderer` 注册 modifier，
  在**抽取阶段**把该生物的 `IMobEnchantments` 快照写进 render state
  （`setRenderData(MOB_ENCHANTMENTS, data)`，key 是 `eej:mob_enchantments`）；
- `RenderLivingEvent.Post` 里读回快照，若 `hasAny()` 则向同一个 `SubmitNodeCollector`
  **再提交一次模型**，渲染类型用 `RenderTypes.entityGlint()`；
- `LivingEntityRenderer#getRenderType` 是 protected，无法从外部调用 →
  改为 `model.renderType().apply(renderer.getTextureLocation(state))` 取基础渲染类型
  （只用于判空），实际提交用 glint 类型；
- 仍然用 `ThreadLocal` 递归标记避免无限重入。
- ⚠ **与 1.20.1 的差异**：只重放「主体模型」这一层，**盔甲/手持物等后续 render layer
  不会重新提交**，因此不会带光效（1.20.1 的整帧重放会让这些层也带光效）。

#### `impl/compat/geckolib/*`（**降级为可安全加载的兼容壳**）
- GeckoLib 4.7.2 → 5.5.2 的 API 变化：
  包名 `software.bernie.geckolib` → **`com.geckolib`**；
  `GeoRenderEvent` **不再有 `.Post`，只剩 `.Pre`**（`GeoEntityPreRenderEvent` /
  `GeoReplacedEntityPreRenderEvent`）；`GeoRenderer#reRender(...)` **已被删除**，
  换成 `performRenderPass(RenderPassInfo, PoseStack, SubmitNodeCollector, CameraRenderState)`。
- ⚠ **结论：1.20.1 的 GeckoLib 附魔光效无法在 GeckoLib 5 上等价复现**，原因有三：
  1. `.Post` 事件不存在，只有 `.Pre`；
  2. `reRender` 不存在，唯一重放入口 `performRenderPass` 需要事件内部持有的
     `RenderPassInfo`，而事件只暴露 `getRenderPassInfo()`… 但它同时需要 `BoneSnapshots`
     等运行时上下文，且本会话无法在真实环境验证；
  3. 事件与 render state **都不携带实体引用**（`GeoRenderState` 只有
     `getDataMap()` + 若干 `DataTicket`，没有 ANIMATABLE→Entity 映射，
     而 `GeoAnimatable` 本身也不是 `Entity`），无法判断该模型属于哪个生物，
     也就无法查魔咒。
- 在不新增 Mixin / 不引入反射（本移植明确不新增 Mixin）的前提下无法实现，
  因此 `GeckoLibGlintEvents` 保留事件监听器与方法结构，**行为为 no-op**，
  保证 eej 在装有 GeckoLib 时**能正常加载运行而不崩溃**。类注释里写明了将来如何补上。
- `GeckoLibGlintCompat`：`MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`（注册方式不变）。

### 8. JEI 兼容（`impl/compat/jei/`）

#### `impl/compat/jei/AltarCraftingRecipe.java`
- `Ingredient#getItems()` **已被删除** → 改用 `Ingredient#items()`
  （返回 `Stream<Holder<Item>>`，标注 deprecated 但可用），
  从 Holder 造显示栈。`count` 语义不变。

#### `impl/compat/jei/AltarCraftingCategory.java`
- `getRecipeType()` 的返回类型由 `RecipeType<T>` 改为 **`IRecipeType<T>`**
  （JEI 29 把接口放宽了；`RecipeType` 仍实现它）。
- `draw(...)` 的图形上下文由 `GuiGraphics` 改为 **`GuiGraphicsExtractor`**
  （26.1.2 把 GUI 绘制改成「抽取」模型，`GuiGraphics` 已不存在）。
- 箭头贴图改为 JEI 自带的 `IGuiHelper#getRecipeArrow()`
  （原实现手写 `textures/jei/atlas/gui/recipe_arrow.png` 的 UV，该路径在 JEI 29 不再保证存在）。
- `getWidth/getHeight/getIcon/getTitle/setRecipe` 的槽位坐标**全部保留**。

#### `impl/compat/jei/EejJeiPlugin.java`
- `getPluginUid()` 返回 `Identifier`（原 `ResourceLocation`）。
- `RecipeManager#getAllRecipesFor(RecipeType)` **已删除** →
  `RecipeManager#getRecipes()`（`Collection<RecipeHolder<?>>`）后按
  `value() instanceof CraftingRecipe` 过滤并跳过 `isSpecial()`。
- 配方结果不再用 `getResultItem(registryAccess)`，改为 `assemble(CraftingInput)`。
- 客户端拿到的 `level.recipeAccess()` 在多人数服务器上是 `ClientRecipeContainer`
  （不暴露配方列表）→ 直接 return（与原实现「客户端没有完整配方表」表现一致）。
  **单机/局域网**上 `recipeAccess()` 就是 `RecipeManager`，可正常枚举。
- `registration.addRecipes(AltarCraftingCategory.TYPE, wrappers)` 不变。

### 9. 数据生成（`impl/datagen/**`，**资源侧改动最大**）

| 文件 | 改动摘要 |
|---|---|
| `EejDataGenEvent.java` | `GatherDataEvent` 变成抽象类并拆成 **`GatherDataEvent.Client` / `GatherDataEvent.Server`** 两个子类 → 两个监听方法；`includeClient()/includeServer()` 不再存在；`@Mod.EventBusSubscriber` 删除（见文件头注释），改由 `eej` 构造器 `addListener`。 |
| `gen/EejBlockStateData.java` | 从 Forge `BlockStateProvider` 改为原版 **`ModelProvider`**（`registerModels(BlockModelGenerators, ItemModelGenerators)`）；同时生成 blockstates / 模型 / **`assets/eej/items/*.json`**（26.1.x 新增的一层）。**不能调用 `blockModels.run()/itemModels.run()`**（那是原版生成器，会与 mod 生成冲突）。覆写 `getKnownBlocks()/getKnownItems()` 只声明需要生成的条目（`packed_mud_altar_stone` 方块 + 其方块物品 + `small_item_frame`），避免 `ModelProvider` 的「必须有定义」校验因为手工维护的 `packed_mud_pedestal` 而失败。 |
| `gen/EejItemModelData.java` | 从 Forge `ItemModelProvider#basicItem` 改为 `ModelProvider` + `ItemModelGenerators#generateFlatItem(..., ModelTemplates.FLAT_ITEM)`；`getKnownBlocks()` 返回 `Stream.empty()`（只生成物品模型）。 |
| `gen/EejBlockTagData.java` | `ExistingFileHelper` **已从 `BlockTagsProvider` 构造器删除**；构造器变成 `(PackOutput, CompletableFuture<HolderLookup.Provider>, String modId)`。`addTags/tag(...)` 不变。 |
| `gen/EejLootTableData.java` | `LootTableProvider` 新增第 4 个参数 `CompletableFuture<HolderLookup.Provider>`；`SubProviderEntry` 的 provider 由 `Supplier` 变为 `Function<HolderLookup.Provider, LootTableSubProvider>`；`BlockLootSubProvider` 新增第 3 个参数（registries）。 |
| `gen/EejRecipeData.java` | **拆成内容类 + `Runner`**：1.20.1 的 `RecipeProvider(PackOutput)` + `buildRecipes(Consumer<FinishedRecipe>)` 已删除；26.1.2 的 `RecipeProvider` 是内容基类（`(HolderLookup.Provider, RecipeOutput)` + 抽象 `buildRecipes()`），`DataProvider` 是 `RecipeProvider.Runner` 子类。`ShapedRecipeBuilder.shaped(...)` 新增 `HolderGetter<Item>` 参数；`save(consumer, ResourceLocation)` → `save(RecipeOutput, ResourceKey<Recipe<?>>)`。配方内容（图案、材料、解锁条件、id）**完全不变**。 |
| `gen/lang/EejLangCN.java`、`EejLangEN.java` | `net.minecraftforge.common.data.LanguageProvider` → **`net.neoforged.neoforge.common.data.LanguageProvider`**；构造器与方法签名不变。所有 lang key 与文案**逐字不变**。 |
| `EejAltarPointData.java` | `ForgeRegistries.BLOCKS` → `BuiltInRegistries.BLOCK`；`DataProvider#saveStable` 用法不变；输出路径 `data/eej/altar_points/eej_altar_points.json` 不变。 |

### 10. 资源（`src/main/resources`，**必须重写而不能只复制**）

| 资源 | 改动 |
|---|---|
| `pack.mcmeta` | `"pack_format": 15` 在 26.1.2 会直接报错（>64 必须写 `min_format`/`max_format`），改为 `{"pack":{"description":"eej resources","min_format":84,"max_format":[101,1]}}`（数值取自 26.1.2 `version.json`：`resource_major=84`、`data_major=101`、`data_minor=1`）。 |
| **`assets/eej/items/*.json`（新增 3 个）** | 26.1.x 新增「物品模型定义」层：`{"model":{"type":"minecraft:model","model":"eej:item/<name>"}}`。仅靠 `models/item/*.json` 已不够，物品会没有模型。 |
| `data/**` 目录名 | **注册表目录在 26.1.2 改成了单数**：`recipes/` → **`recipe/`**、`loot_tables/` → **`loot_table/`**、`advancements/` → **`advancement/`**（已重命名；`tags/blocks/` 不变）。 |
| `data/eej/recipe/*.json` | **格式变了**：`key` 的值从 `{"item":"..."}` 改为**裸 id 字符串**（或 id 数组）；`result` 从 `{"item":"...","count":N}` 改为 `{"id":"...","count":N}`（`ItemStackTemplate` 的 `id` 字段）。4 个配方已按此重写，图案/材料/数量不变。 |
| `data/eej/loot_table/**`、`data/eej/advancement/**`、`data/eej/tags/**`、`data/minecraft/tags/blocks/mineable/pickaxe.json`、`data/eej/altar_points/**` | 内容格式与 26.1.2 一致，**未改动**（已用原版 26.1.2 的 `packed_mud.json` loot table 与 `yellow_terracotta.json` 配方对照确认）。 |
| `assets/eej/blockstates/*.json` | `{"variants":{"":{"model":...}}}` 形式在 26.1.2 仍被 `BlockStateModelDispatcher` 支持（`variants` / `multipart` 两个字段名未变），**未改动**。 |
| `assets/eej/models/**`、`textures/**` | 模型 JSON 格式未变，**逐字复制**（纹理 png 二进制复制）。 |
| `assets/eej/lang/*.json` | 内容未变（与 datagen 输出一致），**逐字复制**。 |
| `src/main/templates/META-INF/neoforge.mods.toml`（新增） | 1.20.1 的 `src/main/resources/META-INF/mods.toml` 改为模板位置 + 新字段：`modLoader="javafml"` / `loaderVersion="[3,)"` / `minecraftVersion` / `neoVersion`；依赖里 `forge` → **`neoforge`**，`mandatory = true` → **`type = "required"`**；JEI 依赖从 `[15,)` 改为 `[29,)` 且 `type = "optional"`；新增 GeckoLib 可选依赖。**本工程没有 Mixin，所以没有 `[[mixins]]` 段。** |

---

## 二、新增文件（原 1.20.1 工程没有对应物）

| 文件 | 为什么必须新增 |
|---|---|
| `impl/altar/client/PedestalRenderState.java` | 26.1.2 的方块实体渲染必须有一个 `BlockEntityRenderState` 子类承载抽取出来的状态（物品模型、过滤标记、旋转角）。 |
| `impl/registry/EejDataComponents.java` | 1.20.1 把展示框的过滤列表放在物品 NBT（`item_ids`）里；26.1.2 移除了 `ItemStack` 的直接 NBT 存取，物品数据改为数据组件，必须注册一个 `eej:item_ids`（`DataComponentType<List<String>>`）。 |
| `impl/registry/EejEnchantments.java` | 26.1.2 的附魔是 datapack 注册表，`BuiltInRegistries` 里没有 `ENCHANTMENT`，必须经 `RegistryAccess` 解析；多处（数据类、网络包、命令、事件）都需要同一个解析入口。 |

---

## 三、公共 API 上被迫的取舍（**供调用方 `epca` 参考**）

| 1.20.1 | 26.1.2 现状 |
|---|---|
| `EejCapabilities.MOB_ENCHANTMENTS` : `Capability<IMobEnchantments>` | 改为 `DeferredHolder<AttachmentType<?>, AttachmentType<IMobEnchantments>>`。读取方式从 `entity.getCapability(...)` 改为 `entity.getData(EejCapabilities.MOB_ENCHANTMENTS)`（或 `MobEnchantmentApi.get(entity)`）。 |
| `IMobEnchantments extends INBTSerializable<CompoundTag>` | 改为 `extends ValueIOSerializable`；`save()/load(CompoundTag)` **保留**。 |
| `IMobEnchantments.getLevel/apply/remove(Enchantment)` | 参数类型改为 `Holder<Enchantment>`。 |
| `MobEnchantment.getEnchantment()` : `Enchantment` | 返回 `Holder<Enchantment>`。 |
| `EejCapabilityAttachmentEvents.attachEntityCapabilities(...)` | 删除（附件不需要逐实体附加）；`MOB_ENCHANTMENTS_ID` 常量保留。 |
| `EejCapabilityEvents.registerCapabilities(...)` | 删除（26.1.2 无「声明能力接口」这一步）；类保留为空壳。 |
| `MobEnchantmentProvider implements ICapabilitySerializable<CompoundTag>` | 不再实现该接口（平台已删除）；保留 `getData/serializeNBT/deserializeNBT` 三个方法。 |
| `EejNetwork.INSTANCE` : `SimpleChannel` | 删除；改为 `EejNetwork.register(IEventBus)` + `CustomPacketPayload`。 |
| `EejBlocks.PACKED_MUD_PEDESTAL` : `RegistryObject<Block>` | 改为 `DeferredBlock<Block>`（`DeferredHolder` 子类）；`.get()` 用法不变。 |
| `PackedMudPedestal` / `PackedMudAltarStone` 构造器 | 各多了一个 `(Properties)` 构造器（`simpleCodec` 需要），原有的无参构造器保留。 |

**`epca` 实际用到的 API 全部保持兼容**（已核对 `G:\epca\epca-26.1.2neo`）：
`AltarInteractionRegistry.register/dispatch`、`AltarInteractionHandler#onAltarUse`、
`AltarItemContainer#getItem/clearItem/hasItem`、`AltarStructure` 的公开字段、
`AbstractAltarBlock#findAltarStructure` —— 签名与语义一字未改。

---

## 四、未能复现 / 有差异的行为（诚实清单）

1. **GeckoLib 附魔光效**：GeckoLib 5 移除了 `.Post` 事件与 `reRender`，
   且事件与 render state 都不携带实体引用 → **降级为 no-op 兼容壳**（类/监听器保留，不崩溃）。
2. **Java 模型附魔光效**：只重放主体模型，**盔甲/手持物等 render layer 不带光效**
   （1.20.1 是整帧重放，所有层都带）。
3. **冰霜行者（Frost Walker）**：`FrostWalkerEnchantment` 已被原版删除，需要重写
   「走动放置霜冰」的方块逻辑 → **未实现**。魔咒本身仍可施加/保存/显示，只是没有结霜效果。
4. **冲击（Punch）**：`AbstractArrow#setKnockback(int)` 已删除 → **未实现**。
5. **抢夺（Looting）**：事件 `LootingLevelEvent` 已删除，改为在 `LivingDropsEvent` 里按等级
   复制掉落物 → **数量近似**，不是原版的「额外一次掉落判定」。
6. **展示框的旧存档**：`item_ids` NBT 不会迁移到 `eej:item_ids` 组件，旧展示框显示为空。
7. **`player.displayClientMessage(..., true)` 的 actionbar 提示**：该类 API 已删除，
   改用 `sendSystemMessage` → 提示进入**聊天栏**而不是物品栏上方。
8. **`LivingHurtEvent` → `LivingIncomingDamageEvent` 的时机**：伤害事件从
   「无敌帧检查前」变为「之后」（这与 YawningNekoAPI 移植记录里同一处变化一致），
   本模组的攻击加成逻辑在两处都是「读取 source 与 attacker」，行为无实质差异。
9. **能力形态**：祭台侧面的物品能力从 `IItemHandler` 变为
   `ResourceHandler<ItemResource>`（`Capabilities.Item.BLOCK`），
   外部模组若仍按旧 `IItemHandler` 能力查询会拿不到。
10. **旧版的 `SmallItemFrameDataPacket`**：1.20.1 里从未被发送过（只注册了）。
    本移植保留并改为写数据组件，但**仍然没有发送方**，行为与 1.20.1 相同（休眠状态）。

---

## 五、警告说明（16 条，全部可忽略）

| 来源 | 条数 | 说明 |
|---|---|---|
| `PackedMudPedestalBlockEntity`（`IItemHandler` / `ItemStackHandler`） | 12 | NeoForge 26.1.2 把这两个类标记为 `@Deprecated(forRemoval)`（指向 `ItemStacksResourceHandler`），但**它们仍然存在且可用**。保留旧 API 是为了让侧面过滤物品栏的行为与 1.20.1 完全一致。 |
| JEI `RecipeType` / `RecipeType.create` | 3 | JEI 29 把 `RecipeType` 标记为 `@Deprecated(forRemoval)`（指向 `types.IRecipeType`），但 `IRecipeCategory#getRecipeType()` 仍接受它，且 `AltarCraftingCategory.TYPE` 是 public 字段、被 `registration.addRecipes` 使用。 |
| JEI `IIngredientAcceptor#addItemStack` | 1 | 同样是 `forRemoval` 弃用，功能正常。 |

---

## 六、需要真实游戏才能验证的部分

以下内容本次**只做了静态核对 / 编译验证**，没有在游戏里跑过：

1. **数据生成**（`runData`）：`EejBlockStateData` / `EejItemModelData` 的
   `getKnownBlocks()/getKnownItems()` 覆写是否让 `ModelProvider` 的校验通过；
   `EejRecipeData.Runner` 是否正确写出配方+进度。**目前 `src/main/resources` 里的
   recipe/advancement/loot_table/items/blockstates 都是手工写的、已按 26.1.2 格式重写**，
   即使 datagen 有问题，资源本身也是完整的（datagen 只是会重写同样内容）。
2. **负载注册**：`PayloadRegistrar` 的 3 个注册是否都被 NeoForge 接受
   （尤其是 `playToServer` 的 `SmallItemFrameDataPacket` 是否需要设置
   `optional()` 才能与未装 eej 的客户端互通）。
3. **数据附件**：`AttachmentType.serializable(MobEnchantments::new)` 是否能在实体上
   正确持久化/同步（`Holder<Enchantment>` 的反序列化依赖 `EejEnchantments.registry()`
   能拿到附魔注册表 —— 服务端有、客户端在收到同步包时也应该有）。
4. **RenderState modifier**：`RegisterRenderStateModifiersEvent#registerEntityModifier(
   new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>(){}, ...)`
   是否对所有生物渲染器（含子类）生效。
5. **附魔光效重放**：向同一个 `SubmitNodeCollector` 二次提交模型是否真的产生
   可见的附魔闪光（可能被批次顺序/深度测试影响）。
6. **祭坛合成**：26.1.2 的 `CraftingInput` 语义（3x3、`assemble` 只用输入）
   与 1.20.1 的 `CraftingContainer` 是否在「材料匹配 → 消耗 → 余量」这条链上完全等价。
7. **方块掉落**：`affectNeighborsAfterRemoval` 里 `level.getBlockEntity(pos)` 是否
   还能拿到（尚未被清空的）方块实体，以及 `level.getBlockState(pos).is(state.getBlock())`
   这个「是否真被替换」的替代判断是否在所有移除路径上都成立。
8. **JEI 分类渲染**：`GuiGraphicsExtractor` 上下文的 `draw` 与
   `IGuiHelper#getRecipeArrow()` 的组合是否与原来的手写箭头视觉一致。
9. **祭台渲染**：26.1.2 的 `ItemStackRenderState` 在 `ItemDisplayContext.GROUND/FIXED`
   下的变换是否与 `ItemRenderer#renderStatic` 一致（理论上由物品模型自带的
   display transform 决定，应当一致）。

---

## 七、目录与落地

```
G:\epca\eej\eej-port\
  src\main\java\...            54 个 java 文件（51 移植 + 3 新增）
  src\main\resources\          资源（含新增 assets/eej/items/*.json、单数化的 data 目录）
  src\main\templates\META-INF\neoforge.mods.toml
  PORTING.md                   本文件
  build-gradle-notes.md        build.gradle 需要加的依赖/仓库行 + compileOnly 说明
  sources.txt                  javac 源文件清单（自带，供复现）
  cp-port.txt                  javac classpath argfile（自带，供复现）
  compile.log                  最后一次 javac 输出（0 errors / 16 warnings）
```

落地到 `G:\epca\eej-26.1.2neo` 时：

```cmd
robocopy /E /IS /IT "G:\epca\eej\eej-port\src" "G:\epca\eej-26.1.2neo\src"
```

⚠ **注意**：目标工程现有的 `src/generated/resources` 里还是 1.20.1 时代的产物
（`data/eej/recipes/`、`data/eej/loot_tables/`、`data/eej/advancements/` 复数目录，
以及 `models/item/*.json` 但没有 `items/*.json`）。robocopy 是增量覆盖，
**不会删除**旧目录，建议先把目标的 `src/generated/resources` 清空或改名，
再跑一次 `gradlew runData` 重新生成；否则旧的复数目录会和新写的单数目录并存，
旧目录里的 `{"item": ...}` 格式配方会解析失败并刷错误日志。
（本次已把必需的资源写进 `src/main/resources`，所以即使不跑 datagen 也能运行。）
