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
3. GUI 内 3D 模型预览降级为空实现（ModelPreviewRenderer 存根 + 3 处调用点），待 GPU/GuiRenderState 路径后用 `guiGraphics.entity/skin` 重做。
4. `rip.ysm.gpu` GPU 加速路径 + GeoModel SIMD 顶点构建禁用中；原生库 libysm-core 缺失仅 ERROR 不 crash（NativeLibLoader try/catch）。
5. 真机验证项：mixin 注入点实际命中（startRiding TAIL、onEffectsRemoved 等）、输入/HUD 事件触发、进存档后模型替换与 GeoBufferSource 提交。
6. **26.3 输入是 SDL（不是 GLFW）**：`InputConstants.isKeyDown` 直接用 SDL scancode 索引键盘状态缓冲且无 UNKNOWN 防护；`InputConstants.UNKNOWN` = scancode **0**。**任何以 -1 为 keyCode 的 KeyMapping 会在进入世界时（KeyMapping.setAll）IndexOutOfBounds 崩溃**（已修 ExtraAnimationKey，提交 8e1e394）。类似地 `matches` 用 `KeyEvent(keyCode, scanCode, 0)`。

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
5. ~~GUI 包~~（2026.09.25 第五轮恢复编译，未提交）：`rip/ysm/gui/` + `client/gui/` 大部分；HUD 此前已迁移 `HudElement`/`HudElementRegistry`；keybinding/命令文件已回接。**遗留：GUI 内 3D 模型预览降级为空实现**（ModelPreviewRenderer 存根 + 3 处调用点 TODO），待 GPU 路径恢复后用 `guiGraphics.entity/skin` 重做。`rip.ysm.gpu` GPU 加速路径（钩子在 GeoBufferSource/NativeModelRenderer 头部 TODO）仍排除，仅 BlurStack/Pie/GpuCapability 以 shim 恢复。GeoModel SIMD 顶点构建禁用中（TODO）。
6. compat 逐个恢复（存根已就位，每个单独提交替换真实现）
7. runClient 运行期验证（所有新 mixin 签名、HUD、事件时机）
