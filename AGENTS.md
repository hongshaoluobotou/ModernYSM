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
- 编译错误从 ~3300 降到 ~1670。

## 遗留 TODO（功能保留但未注册，需 mixin 恢复）

1. `event/CapabilityEvent.onPlayerCloned`（原 Architectury PLAYER_CLONE）→ 需 mixin `ServerPlayerList` respawn 流程。
2. `client/event/ClientPlayerCloneEvent.onClientPlayerRespawn` → 需 mixin 客户端 respawn。
3. `event/CommonEvent.register` 原 `LifecycleEvent.SETUP` 已内联到 entrypoint 初始化。
4. 新 mixin 的目标方法签名（`keyPress(JILKeyEvent;)V`、`onButton(J MouseButtonInfo,I)V`）需 runClient 运行期验证。

## 已知错误规模（2026.09.25，Architectury 移除后重测）

按类别（2026.09.25 二次统计，总 ~1670）：

1. **Architectury：已清零。**（保留此条目供历史参考，勿再查）
2. **compat 反向引用（~150 处）**：compat 包虽被排除，但 `com.elfmcys.yesstevemodel` 主代码里有 20+ 文件 import `rip.ysm.compat.*`（oculus、slashblade、touhoulittlemaid、curios 等）。处理策略：恢复某个 compat 时一起修；若主代码文件同时调多个未恢复 compat，可临时加存根类（`rip.ysm.compat.<x>` 空实现 + TODO）解耦。
3. **配置系统：已完成**（见"已完成"）。注意：Night Config 需显式依赖，MC 26.3 classpath 上没有它（此前记载有误）。
4. **Cardinal Components（~30 处，下一个目标）**：5 个组件（star_models / auth_models / model_info / projectile_model / vehicle_model，见 fabric.mod.json custom）。26.3 无 CCA——评估：迁移到 1.21+ 原生 entity DataAttachments 等价物，或自研简单附加数据存储。
5. **MC API 变更（剩余"找不到符号"大头）**：渲染管线重写（`MultiBufferSource` 18 处、自定义 shader `bone_skin.fsh/.vsh`、`rip.ysm.gpu`、`@BufferBuilderMapping`/BufferBuilder mixin）——1.21.5+ GpuBuffer/RenderPipeline 体系，改动量最大。另有大量逐文件的小 API 变更（`ResourceLocation→Identifier`、`UseAnim`、`Parrot`、`KeyMapping.matches`、`GameProfile` 等）。
6. **已禁用/待按需恢复**：`rip.ysm.compat.*`、iris（需 26.3 版依赖）、ImageStream（JitPack，已在 build.gradle，待验证）、natives（预编译在 `common/src/main/resources/natives`）。

## 建议的推进顺序（每步可独立提交）

1. 移除 Architectury（机械替换，独立可编译性最好）
2. 配置系统重写
3. Cardinal Components → 原生方案
4. 非渲染类 MC API 适配（实体/物品/网络 CustomPayload/命令）
5. 渲染层（最大坑，最后做；先非 shader 路径，再 GPU/shader）
6. compat 逐个恢复（每个单独提交）
