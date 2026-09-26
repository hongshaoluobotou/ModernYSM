# AGENTS.md

Multi-loader → Fabric-only Minecraft mod: open-source replacement for Yes Steve Model (YSM). **Currently mid-port to Minecraft 26.3 on branch `port/26.3`** — see the port section at the bottom for status and ground rules.

## Structure (post-2026.09 refactor)

- **Single Gradle project** (no more common/fabric/forge subprojects). Root name `yes_steve_model` in `settings.gradle`.
- Source dirs are mapped into the single `main` source set via `sourceSets` in `build.gradle` — **the old directory layout is kept on disk**:
  - `common/src/main/java` — all shared code
  - `fabric/src/main/java` — Fabric glue (entrypoints `com.elfmcys.yesstevemodel.fabric.YesSteveModelFabric` / `...client.YesSteveModelFabricClient`)
  - Resources: `common/src/main/resources` + `fabric/src/main/resources` (both on the resource path)
- Two package roots in the codebase:
  - `com.elfmcys.yesstevemodel` — core inherited from LegacyYSM (model, geckolib3, molang, client, mixin, config...)
  - `rip.ysm` — newer OpenYSM code (api, compat, gpu, gui, legacy, security, zstd)
  - Third-party vendored: `org.concentus`, `org.gagravarr`, `net.sourceforge` (audio/opus)
- `forge/` dir is dead code (excluded from build); kept on disk for reference only. `libs/` holds 1.20.1 compile-only jars for the old compat system — **not used by the 26.3 build anymore** except nothing; compat (`rip.ysm.compat.*`) is disabled during the port and restored on demand.

## Build

- `./gradlew build` — verify with `./gradlew compileJava` first.
- Gradle **9.7.1** (loom 1.18.2 requires ≥9.7; the template's 9.5.1 is too old). Wrapper dist URL points to Tencent mirror (`mirrors.cloud.tencent.com/gradle/`) because services.gradle.org times out on this network. Maven Central etc. work but are flaky — retry Gradle on transient "Could not find" errors before assuming a version doesn't exist.
- **fabric-loom 1.18 API changes vs old architectury-loom builds**: use `net.fabricmc.fabric-loom` plugin; mod deps go in plain `implementation` (group `net.fabricmc.fabric-api:fabric-api` — note the double `fabric-api`); `include` still exists for jar-in-jar; `modImplementation`/`modCompileOnly` are gone. No `architectury` block, no shadow plugin, no shadowBundle/remapJar pipeline.
- Java **25** (`options.release = 25`), Minecraft **26.3**, loader 0.19.5, Fabric API 0.161.0+26.3 — all in `gradle.properties`. Loom plugin version also lives in `gradle.properties` (`loom_version`) and is interpolated in the `plugins` block.
- Reference template (official fabric example for 26.3): `/home/asus/IdeaProjects/template-mod-template-26.3.zip` (also extracted at `/tmp/opencode/tmpl`). It uses `splitEnvironmentSourceSets()` (main/client source sets) — **we deliberately do NOT**; everything stays in `main` to avoid moving hundreds of files.

## Mixins / runtime

- Mixin configs: `yes_steve_model.mixins.json` (common, in `common/src/main/resources`) and `yes_steve_model_fabric.mixins.json` (in `fabric/src/main/resources`), both registered in `fabric/src/main/resources/fabric.mod.json`.
- `fabric.mod.json`: depends java>=25, minecraft ~26.3, fabricloader >=0.19.5. Cardinal-components entrypoint + `custom.cardinal-components` still present but CCA is not on the 26.3 classpath yet.
- No CI, no tests, no formatter. Verification = compile + in-game `./gradlew runClient`.

## Conventions

- Commit messages / comments / docs are in Chinese. README.md is historical prose (project "sunset" note) — trust build files over it.

---

# 移植到 Minecraft 26.3（进行中）

## 已完成（分支 `port/26.3`，按时间序提交）

- Gradle 单项目改造、wrapper 9.7.1（腾讯镜像）、loom 1.18-SNAPSHOT、fabric-api 0.161.0+26.3 依赖解析通过（`./gradlew build -x compileJava` 绿）。
- `fabric.mod.json` 版本依赖已更新；`common`+`fabric` 源码目录通过 sourceSets 合并进 `main`；`rip/ysm/compat/**` 已从编译排除。
- **Architectury 依赖全部移除**（提交 daf3210）+ **配置系统已重写**（提交 4e34867）：
  - 46 处 `@ExpectPlatform` 全部改为直接委托同签名 `XxxImpl`（fabric 侧实现类，路径见各 `fabric/src/main/java/**/fabric/`）；事件换 fabric-api（`ServerLifecycleEvents`、`ServerPlayConnectionEvents`、`ClientTickEvents`、`ClientPlayConnectionEvents`、`CommandRegistrationCallback`、`ClientCommandRegistrationCallback`、`KeyMappingHelper`（注意 26.3 是 `client.keymapping.v1` 包）、`ResourceLoader.get(CLIENT_RESOURCES)`（`ResourceManagerHelper` 已移除））。
  - 配置：新 `com.elfmcys.yesstevemodel.config.ConfigSpec` 复刻 ForgeConfigSpec Builder/值类型 API（`BooleanValue/IntValue/DoubleValue/StringValue/EnumValue`，带 get/set/min-max 钳制），基于 Night Config TOML（`com.electronwill.night-config:core/toml:3.8.1`，**需显式依赖，MC 26.3 不自带**，已 include）。文件：config 目录下 `yes_steve_model-client.toml` / `yes_steve_model-server.toml`，键名/默认值/注释与旧 ForgeConfigSpec 一致，`set()` 立即写回。
- 新增基建（后续阶段会用到）：
  - `com.elfmcys.yesstevemodel.util.ServerInstanceHolder` — 用 `ServerLifecycleEvents` 跟踪 `MinecraftServer` 实例（替代 Architectury `GameInstance`），全局取 server 用它。
  - `com.elfmcys.yesstevemodel.client.event.ClientRawInputBridge` + mixin `KeyboardHandlerMixin`/`MouseHandlerMixin` — 替代 Architectury `ClientRawInputEvent`，所有输入钩子挂在 Bridge 上（26.3 输入事件是 `KeyEvent`/`MouseButtonInfo` record）。
- **Cardinal Components 已替换**（提交 96c4cb3）：新 `com.elfmcys.yesstevemodel.capability.fabric.YsmAttachments`（`YsmComponent` 接口 + weak-key `MapMaker` map 挂实体，`getNullable` 语义同原 ComponentKey）；存档 mixin `Entity#saveWithoutId/load`（26.3 是 `ValueOutput/ValueInput`，经 `TagValueOutput/InputAccessor` 拿 CompoundTag），新数据写在实体 NBT `yes_steve_model` 子 tag，兼容读旧 CCA 平铺键；`PlayerList#respawn` mixin 恢复 ALWAYS_COPY 复制 + `CapabilityEvent.onPlayerCloned`（Forge PLAYER_CLONE 等价）。YsmComponents.java 已删除。**注意 26.3 NBT API：`getList/getCompound` → `getListOrEmpty/getCompoundOrEmpty`，`contains(name,type)` → `contains(name)`。**
- **非渲染 MC API 全面适配**（提交 ca2ea2d，错误 1445 → 484，其中 ~400 为排除区连锁）：
  - 26.3 API 要点：实体包移动（`AbstractArrow/Arrow/SpectralArrow`→`projectile.arrow.*`、`Parrot`→`animal.parrot.Parrot`、`Pig`、`Boat` 同理）；`LivingEntity` 挥动字段删除 → 新 `util/SwingCompat`（`getCurrentSwing()/getSwingAnimation()`）；`getDayTime()` 删除 → `getOverworldClockTime()`；`Minecraft#setScreen` → `setScreenAndShow`；权限 → 新 `util/PermissionsCompat`（`PermissionSet`）；`InputConstants.Type.KEYSYM` → `KEYBOARD`；`KeyMapping.matches(KeyEvent)`；fabric-api 网络 `PayloadTypeRegistry.playC2S/S2C` → `serverboundPlay()/clientboundPlay()`，receiver 上下文 `ctx.packetContext().orElseThrow(PacketContext.CONNECTION)`；`OggAudioStream` 移除 → 自写 `OggVorbisAudioStream`（基于 `JOrbisAudioStream`）。
  - **build.gradle 现有渲染排除块（恢复顺序提示）**：目录级排除 `client/renderer/`、`rip/ysm/gpu/`、`client/gui/`、`rip/ysm/gui/`、`geckolib3/geo/`，另有 ~40 个文件级排除（链根在 `geckolib3/geo/render/built/*` 顶点数据类和 `OuterFileTexture`）。**先修这两处即可解锁 ~250 连锁错误（predicate/controller/keyframe/ClientModelManager 链）**，再做 GUI。
  - 6+2 个渲染 mixin 已在两个 mixins.json 注释留档（`__disabled_render_mixins_TODO_port_26.3`）。
  - `HudRenderCallback` 注册暂注释（26.3 GuiGraphics 变更，评估 `HudLayerRegistrationCallback`）。

## 遗留 TODO（功能保留但未注册/未恢复）

1. `client/event/ClientPlayerCloneEvent.onClientPlayerRespawn` → 需 mixin 客户端 respawn（服务端 PLAYER_CLONE 已在 `PlayerListMixin` 恢复）。
2. `event/CommonEvent.register` 原 `LifecycleEvent.SETUP` 已内联到 entrypoint 初始化（语义近似）。
3. ~~GUI 内 3D 模型预览降级为空实现~~（2026.09.26 已恢复）：`ModelPreviewRenderer` 重写为 GuiEntityRenderState（PiP）路径——状态经 `EntityRenderDispatcher#extractEntity` 提取（填充 state→entity 映射使 YSM 接管生效）+ `guiGraphics.entity(...)`，`renderFollowsMouse`（PlayerModelScreen 主预览，替代 `extractEntityInInventoryFollowsMouse` 直调——后者绕过 mixin 导致 GUI 内不显示 YSM 模型）与 `renderFixed`（ModelButton/TextureGrid/ModelSettingsScreen/ModernPlayerTextureScreen）；PiP translation 的 +y 是**屏幕向下**。附带修复：`PlayerPreviewEntity.DummyPlayer` 需 `setId()`（26.3 extract 要求实体 ID 非零）；`OptionScreen` 双重 blur 崩溃（vanilla `extractBlurredBackground` 与 BlurStack 各 blur 一次 → 一帧两次，现覆写置空 vanilla blur）。ExtraPlayerOverlay/HudOverlay 仍在排除列表（依赖已删 GuiGraphics），恢复 HUD 时改用 renderPlayerOverlay（已实现，走 renderFixed）。**2026.09.26 追加：GUI 预览原显示史蒂夫——`PlayerCapabilityClientStore.get` 对预览 DummyPlayer 新建空 capability → mixin 不接管；已改为 `PlayerPreviewEntity extends PlayerCapability`（按 UUID 登记 `PREVIEW_WRAPPERS`），store 命中预览实体直接返回已加载模型的包装器（92921ea）。**2026.09.26 追加 2：原版模型叠画——`EntityRenderDispatcherMixin.ysm$wrapSubmit` 接管后曾无条件调 `original.call`（原注释以为只提交名牌/缰绳，实际 26.3 的 `LivingEntityRenderer.submit` 末尾才 invokespecial 基类 submit，即原版模型本体）；已改为 handled 时跳过 original，手动补缰绳（state.leashStates → submitLeash）与名牌（新 `EntityRendererInvoker` @Invoker `submitNameDisplay` 虚分派重载）（959b5a0）。**
4. `rip.ysm.gpu` GPU 加速路径 + GeoModel SIMD 顶点构建禁用中；预编译 natives 是 1.20.1 产物（JNI 注册旧 `GeoModel.nInitSIMD` 签名，System.load 即 NoSuchMethodError，恢复 GPU/SIMD 前需重编 native）。原生库失败**不再阻断 `initConfig()`**（1617b12），`YesSteveModel.isAvailable()` 仍以原生库可用为准——需要原生库的功能在真机会静默降级。键位注册失败现在会记 ERROR 日志（`Failed to register key mapping ...`）。
5. 真机验证项：mixin 注入点实际命中（startRiding TAIL、onEffectsRemoved 等）、输入/HUD 事件触发、进存档后模型替换与 GeoBufferSource 提交。
6. **26.3 输入是 SDL（不是 GLFW）**：`InputConstants.isKeyDown` 直接用 SDL scancode 索引键盘状态缓冲且无 UNKNOWN 防护；`InputConstants.UNKNOWN` = scancode **0**。**任何以 -1 为 keyCode 的 KeyMapping 会在进入世界时（KeyMapping.setAll）IndexOutOfBounds 崩溃**（已修 ExtraAnimationKey，提交 8e1e394）。**KeyMapping 的 KEYBOARD 域是 SDL scancode**（vanilla 语言文件全是 `key.keyboard.*` scancode 名；`KeyMapping.matches(KeyEvent)` 比较的是 `event.key()` 即 scancode 域，`event.keycode()` 是布局键码，勿混用——提交 e10189f 修桥、1617b12 修默认值）。GLFW 键码→SDL scancode 对照：A=4 起字母表顺序 +3，即 B=5、L=15、P=19、Y=28、Z=29。**键位分类**：26.3 `KeyMapping.Category` 是 record，自定义分类必须 `KeyMapping.Category.register(Identifier)`（public，加入 SORT_ORDER；fabric 的 KeyMappingCategoryMixin 注册后重排序）——直接 `new Category(id)` 不会进 SORT_ORDER。label 语言键 = `key.category.<ns>.<path>`（本项目 `key.category.yes_steve_model.main`，已在各 lang 文件补全）。已验证 5 个 YSM 键位均进入 `Options.keyMappings`（headless diag 日志 `[YSM diag]`，后续可删）。
7. **动画驱动链依赖 WorldRendererMixin（2026.09.25 已重接）**：动画控制器时间线的推进（`controller.process`）只在 `AnimatableEntity.setCustomAnimations` 的 `z3=true`（即 `processAnimationImpl(partialTick, isFirstPerson=true)`，`event.isFirstPerson()` 为 true）路径执行；渲染时同步路径永远 z3=false，只消费 `EntityRenderCache.tick` 异步预计算的结果。1.20.1 由 `WorldRendererMixin` 在 renderLevel 开头 `setFirstPersonMode(true)+EntityRenderCache.tick(partialTick)` 驱动整个帧。26.3 移植时该 mixin 曾被整体禁用 → `RenderBridge.firstPersonOnRenderThread` 恒 false → 控制器永不推进 → **模型渲染但完全静止**。已按 `LevelRenderer#render`（HEAD/RETURN，26.3 无 renderLevel 旧签名）重接，`ModelPreviewRenderer` 存根补 `setFirstPersonMode`（回写 RenderBridge.firstPerson/firstPersonOnRenderThread），build.gradle 对应文件级排除已移除。真机待验证：行走/挥手/idle 动画恢复、第一人称（FirstPersonHandsAndItemsRenderer 未移植，第一人称相机下自模型不渲染属预期）。
8. **26.3 纹理上传时机**：`TextureManager.register(id, AbstractTexture)` 不再触发加载/上传（旧 `registerAndLoad` 仅限 `ReloadableTexture`）。自管理纹理（OuterFileTexture）必须在注册前于渲染线程完成 `GpuDevice.createTexture + createTextureView + writeToTexture`，否则渲染帧 `RenderSetup.prepareTextures` 抛 "Texture view does not exist"（已修：`OuterFileTexture.ensureLoaded()` + UploadManager.registerTexture 前置调用，提交 a421800）。注意 `createTexture` 的 usage 必须含 `USAGE_COPY_DST`，否则 `writeToTexture` 抛 "Color texture must have USAGE_COPY_DST"（e59b9d7）。
9. **特效面片（ysmGlow 前缀骨骼，零厚度平面，如酒狐疾跑魔法阵）不显示 / 模型缺约 1/4 面——同源，已按绕过方案修复（9f815ee）**：根因是 `NativeModelRenderer` 的 CPU 背面剔除（投影空间三点行列式，等价 GPU 屏幕环绕方向判定）在 26.3 拿不到 `RenderSystem.getProjectionMatrix()`，`projBoneMat.identity()` 兜底下 det 退化为模型空间 XY 静态有符号面积 → 按几何朝向静态误剔除（既剔掉魔法阵，也剔掉普通模型部分面）。两次"取回真实投影矩阵"修复尝试（0b9a1ca view-space det、1f0b4f1 accessor 取 lastUploadedProjection，后者字段验证正确但真机仍无效）均被真机证伪并 revert。**现方案：直接删除 CPU 剔除判定，cullable cube 全部双面渲染**——不透明面由深度测试自然遮蔽，视觉严格改善；魔法阵两面都画（TODO 9 顺带解决）。若将来需恢复单面渲染：可试 view-space 法线·视线 dot 判定（需与 1.20.1 逐模型对照），或等 GPU 路径恢复后在 shader 侧重做剔除（native 剔除本就在 GPU）。

## 当前状态与剩余错误（2026.09.25 六次更新）

**`./gradlew build` 全绿，无头 `xvfb-run ./gradlew runClient` 已成功启动到主菜单**（LWJGL/OpenAL/Sound engine 正常，资源加载含本 mod，无 crash report；提交 669ce81 修复 6 处运行期 mixin 问题）。`rip.ysm.gpu` GPU 加速路径仍排除、部分功能降级（见 TODO）。

- **mixins.json 注释规范**：`mixins`/`client` 数组内不要放 `//` 条目（会被当类名加载 crash），废弃 mixin 一律挪到额外 key（如 `__disabled_render_mixins_TODO_port_26.3`）。
- runClient 修复要点：`LivingEntity#onEffectRemoved`→`onEffectsRemoved(Collection)`；`ServerPlayer#startRiding`→`(Entity,Z,Z)`+TAIL 注入；`Arrow.effects` 字段没了→`EffectLevel` 静态辅助从 `POTION_CONTENTS` 组件读；`AbstractArrow.inGround` private 化→@Invoker；`connection` 字段上移 `ServerCommonPacketListenerImpl`。

- 主代码 → `rip.ysm.compat.*` 的反向引用已用**编译存根**解耦（`common/src/main/java/rip/ysm/compat/<modname>/` 内空实现 + TODO，恢复该 compat 时替换真实现）。
- **渲染层状态**（c4cc6b1 解锁数据链）：`geckolib3/geo/render/built`（GeoBone/GeoModel，SIMD 路径已禁用待按 renderpearl 重做）、`geckolib3/geo/animated`、`OuterFileTexture`（已按 26.3 GpuTexture/CommandEncoder.writeToTexture 重写）已恢复；渲染相关状态桥见 `client/bridge/RenderBridge.java`。
- **client/renderer 标准渲染路径已恢复**（提交 65fb153）：`CustomPlayerRenderer` 等按 26.3 `EntityRenderer<T,S extends EntityRenderState>` 重写，新 `GeoBufferSource` shim 走 `submitCustomGeometry`（GPU 加速分流点）；`EntityRenderDispatcherMixin` @WrapOperation 包 `EntityRenderer.submit` 接管模型替换（旧 PlayerRenderer mixin 目标已改名 AvatarRenderer，废弃留档）。26.3 API 要点：`LightTexture` 删除→常量 0xF000F0；`PoseStack.mulPose(Quaternionf)`→`rotate(...)`；盔甲判定 `DataComponents.EQUIPPABLE`；披风/鞘翅走 `PlayerSkin` record；肩膀鹦鹉强类型 API；`getTextureLocation` 只在 LivingEntityRenderer 上。
- **GUI 包已恢复编译**（2026.09.25 第五轮，未提交）：`rip/ysm/gui/`、`client/gui/`、keybinding、`ModScreenEvent`、`PauseScreenMixin`、`PlayerSkinTextureManager` 全部回接。26.3 GUI API 要点：`GuiGraphics`→`GuiGraphicsExtractor`（`render`→`extractRenderState`、`drawString`→`text`、`renderTooltip`→`setTooltipForNextFrame`、scissor 用 `guiGraphics.enableScissor(x0,y0,x1,y1)`——RenderSystem scissor 已删）；事件签名 `mouseClicked(MouseButtonEvent,boolean)`/`mouseReleased(MouseButtonEvent)`/`mouseDragged(MouseButtonEvent,dx,dy)`/`mouseScrolled(x,y,scrollX,scrollY)`/`keyPressed(KeyEvent)`/`charTyped(CharacterEvent)`（`MouseButtonEvent`/`KeyEvent` record 实现 `InputWithModifiers`，有 hasAltDown 等；`CharacterEvent` 没有）；`Checkbox` 只能走 `Checkbox.builder(...).pos().selected().onValueChange()`；`resize(int,int)`；`InputConstants.getKey(KeyEvent)`；`PlayerSkin` 是 record（`body().texturePath()`）；实体 GUI 预览用 `InventoryScreen.extractEntityInInventoryFollowsMouse(g,x0,y0,x1,y1,size,offsetY,mouseX,mouseY,entity)`（自带 Pictures-in-Picture 裁剪）。
- **3D 模型预览已降级为空实现**（TODO 注释留位）：`ModelPreviewRenderer` 为同 FQN 空实现存根；`ModelSettingsScreen`/`ModernPlayerTextureScreen`/`TextureGrid.renderHolderPreview` 的预览体只留 `guiGraphics.enableScissor/disableScissor` 占位——依赖已删的 RenderSystem model-view scissor、`MultiBufferSource.BufferSource`、`Lighting` API，待 `GeoBufferSource → GuiRenderState` 提交路径完成后用 `guiGraphics.entity(GuiEntityRenderState)` / `guiGraphics.skin(...)` 重做。

## 建议的推进顺序（前 4 步已完成，每步独立提交）

1. ~~移除 Architectury~~（daf3210）
2. ~~配置系统重写~~（4e34867）
3. ~~Cardinal Components → 自研 YsmAttachments~~（96c4cb3）
4. ~~非渲染类 MC API 适配~~（ca2ea2d）
5. ~~GUI 包~~（2026.09.25 第五轮恢复编译，未提交）：`rip/ysm/gui/` + `client/gui/` 大部分；HUD 此前已迁移 `HudElement`/`HudElementRegistry`；keybinding/命令文件已回接。**遗留：GUI 内 3D 模型预览已于 2026.09.26 恢复（见 TODO 第 3 条）**。`rip.ysm.gpu` GPU 加速路径（钩子在 GeoBufferSource/NativeModelRenderer 头部 TODO）仍排除，仅 BlurStack/Pie/GpuCapability 以 shim 恢复。GeoModel SIMD 顶点构建禁用中（TODO）。
6. compat 逐个恢复（存根已就位，每个单独提交替换真实现）
7. runClient 运行期验证（所有新 mixin 签名、HUD、事件时机）

## GUI 与 1.20.1 行为差异清单（2026.09.26 对照 1.20.1-forge 逐类检查，只记录未实现）

**输入链全局（已修）**：`KeyboardHandlerMixin` 原本在 `keyPress` HEAD 派发 `ClientRawInputBridge` 后**不取消**原版后续处理；26.3 原版在 HEAD 之后才读 `gui.screen()`，桥接监听器打开的 Screen 会立刻收到同一个按键事件 → `PlayerModelScreen#handleToggleKey` 对 Y 执行 `onClose()` → **界面"打开即闪关"**（真机复现确认）。已修：mixin 记录 bridge 调用前后的 screen，若变化则 `ci.cancel()`。1.20.1 无此问题（Architectury 事件路径/screen 读取时机不同）。**该修复同时覆盖 AnimationRouletteKey（开+关 toggle）、ExtraPlayerRenderKey 等所有经 bridge 开屏的键位**——这些键在 Screen 内的"再按关闭"逻辑仍走 Screen 自己的 keyPressed，行为正常。

逐类（除注明外均为纯机械 API 迁移，语义一致）：
- **DisclaimerScreen / OpenModelFolderScreen / ModelInfoScreen / ModelUploadScreen / ExtraPlayerConfigScreen / PlayerTextureScreen / PlayerModelScreen / AnimationRouletteScreen / OptionScreen**：仅 `setScreen→setScreenAndShow`、`render→extractRenderState`、`drawString→text`、`renderTooltip→setTooltipForNextFrame`、`Checkbox→builder`、`Util.getPlatform().openUri/openFile→Blaze3D.openUri/openPath`、`pose().pushPose/popPose/translate(x,y,z)/scale(x,y,z)→pushMatrix/popMatrix/translate(x,y)/scale(x,y)`、`fill→fillGradient`（AnimationRouletteScreen renderPageInfo，半透明底色）、`ResourceLocation→Identifier` 等；init/onClose/keyPressed 关闭条件与 1.20.1 逐分支一致。OptionScreen 额外覆写 `extractBlurredBackground` 置空 vanilla blur（一帧一次 blur 限制，语义等价于 1.20.1 的整屏 renderBackground）。
- **ModelInfoScreen**：作者头像 `textureManager.register` 前需 `avatar.ensureLoaded()`（26.3 register 不触发上传）——已实现，无缺失。
- **ModernPlayerTextureScreen**：⚠️ 预览纵向偏移 `offsetY` 由 1.20.1 的 `-60.0f` 改为 `0.0f`（init 与字段均改）——PiP 路径坐标语义不同所致，真机如发现预览位置偏上/偏下需回查此值。
- **ModelSettingsScreen**：旧 `renderPreview/renderPlayerForSettings`（RenderSystem model-view + scissor + dispatcher.overrideCameraOrientation + Lighting + bufferSource.endBatch）已重写为 PiP 路径；⚠️ 旧版在预览中通过 `TouhouLittleMaidCompat.getMaidPreviewRenderer` 支持 TLM 模型预览，26.3 版（compat 排除期）仅支持 CustomPlayerEntity 路径——compat 恢复时补回。
- **ModelPreviewRenderer**：407 行 → 178 行重写为 GuiEntityRenderState(PiP) 路径；旧版 `renderLivingEntityPreview`（静态、外部传 renderer、renderShape 参数）签名不同，TextureGrid 已适配 `renderFixed`。旧版 `setPreviewMode(true)` 全局开关的语义（影响模型接管/跟随）在 PiP 重写中如何对应**未逐项核对**。
- **TextureGrid.renderHolderPreview**：旧 RenderSystem scissor + 直接调 ModelPreviewRenderer → 新 `renderFixed`（PiP 自带裁剪）；缩放/朝向参数为重调值（30/-10/20/6），与 1.20.1（35.0f、+24y）非逐值对应，观感待真机确认。
- **模型按钮"灰色/半个身体"排查结论（2026.09.26 头无头复现 + 1.20.1 对照）**：灰色填充是**模型自带 gui 预览动画里的 background/curtain 背景面片**（ysm.json `preview_animation: "gui"`，骨骼如 `background scale [50,50,1]`、`white_curtain scale [39.5,50.1,1]`），不是 vanilla 皮肤也不是 ModelButton 背景图。骨骼变换层与 1.20.1 **逐行一致**（RenderUtils 仅 mulPose→rotate 改名；AnimatableEntity/PlayerModelScreen/ModelButton 预览参数、动画时间线 `ClientTickEvent.getTickCount` 均相同），移植层无方向/单位回归。已落地 `ModelPreviewRenderer.fitScale()`：PiP 按 bbox 定位，bbox（0.6x1.8）远小于 YSM 实际几何+gui 动画位移时模型溢出 52x70 按钮视口被裁，现按 bbox×余量（高 1.35/宽 1.5）收缩 scale 保证全模可见。**遗留未定论**：部分模型 cell（每 run 随机子集，如 wine_fox 01–05）渲染出远小于预期的几何碎片（gui 动画 MRoot/Root 大位移+scale2 关键帧与 PiP 变换合成的结果疑似与 1.20.1 取景不一致）——需 1.20.1 真机截图逐模型对照才能定论是包作者设计还是 26.3 动画求值回归；gui 动画是否应在 GUI 预览中冻结/重定位未定。头无头复现基建（Xvfb + YSM_DIAG 环境变量驱动 createFreshLevel→开屏→Screenshot.grab）已验证可用但按规约已删除，需要时从 git 历史本次会话找回。**2026.09.26 追加 3（提交 3e1dade）：碎片化真因不是动画求值——1.20.1 预览直接改写预览实体 yBodyRot/yRot/yHeadRot（geo 路径从实体字段消费旋转），26.3 PiP 的 renderFixed 只改渲染 state 对 geo 路径无效 → DummyPlayer 旋转恒 0、按钮预览背面/碎片（跨 run 随机的"碎片子集"实为不同模型背面朝向+gui 动画位移的观感）。修复：ModelPreviewRenderer `PREVIEW_YAW` map（提取时按 DummyPlayer 登记），CustomPlayerRenderer.submit 内同步改写实体旋转、finally 还原。截图确认 gui 预览动画推进、各 cell 可见角色模型。遗留：ModelButton vOff 取景与 1.20.1（+20px 中心偏移）未逐值对齐。**
- **键位类（client/input/*）**：全部 `KEYSYM+布局键码` → `KEYBOARD+SDL scancode`（A=4 起字母序：Y=28、Z=29、B=5、L=15、P=19）；`ClientRawInputEvent→ClientRawInputBridge`；`Minecraft.screen→gui.screen()`。**InputStateKey 的 `MOUSE_CLICKED_PRE` 返回值语义**：1.20.1 `EventResult.pass()` 可拦截，26.3 Bridge 是否保留 PRE 拦截语义未核对。ExtraAnimationKey 未绑定键 scancode 0（=SDL UNKNOWN），与 1.20.1 的 -1 语义近似但**scancode 0 是合法键位**，用户若绑定到 0 号 scancode 会与"未绑定"冲突（边缘情况，待定）。
- **molang/动画驱动**：预览实体走 `PlayerPreviewEntity` + `WorldRendererMixin→RenderBridge.firstPersonOnRenderThread` 驱动链（TODO 第 7 条），GUI 打开时 renderLevel 仍执行、控制器照常推进——与 1.20.1 一致，无已知差异；待真机验证预览动画是否流畅。
