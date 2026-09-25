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

## 已完成（骨架已提交到 `port/26.3`）

- Gradle 单项目改造、wrapper 9.7.1、loom 1.18-SNAPSHOT、fabric-api 0.161.0+26.3 依赖解析通过（`./gradlew build -x compileJava` 绿）。
- `fabric.mod.json` 版本依赖已更新；`common`+`fabric` 源码目录通过 sourceSets 合并进 `main`。

## 已知错误规模（约 3000+，javac 全量统计法：`-Xmaxerrs 10000` 已加进 build.gradle）

按类别（2026.09.25 统计）：

1. **Architectury 依赖（~300 处，必须移除，Fabric-only 不再需要）**：
   - `dev.architectury.injectables.annotations.ExpectPlatform/PlatformOnly`（94 处）→ 直接改成调用 Fabric 实现类；旧平台实现类在 `fabric/src/main/java`，`@ExpectPlatform` 的宿主方法改为直接调用。
   - `dev.architectury.event.events.*`（~90 处）→ 换 Fabric API 对应事件（`ClientTickEvents`, `LifecycleEvents`, `ClientCommandRegistrationCallback` 等）。
   - `dev.architectury.registry.registries.DeferredRegister` → `net.minecraft.core.registries.RegistrableRegister`/Fabric API 注册方式。
   - `dev.architectury.platform.Platform` → 环境判断改用 `FabricLoader.getInstance()`.
2. **配置系统（ForgeConfigSpec ~106 处 + forgeconfigapiport）**：`common/src/main/java/com/elfmcys/yesstevemodel/config/` 整个包基于 ForgeConfigSpec Builder。方案：抽象出轻量 config 层或改用 TOML（Night Config 是 MC 自带依赖）重写 `config` 包。
3. **Cardinal Components（~30 处）**：5 个组件（star_models / auth_models / model_info / projectile_model / vehicle_model，见 fabric.mod.json custom）。26.3 无 CCA——评估：迁移到 1.21+ 原生 entity DataAttachments 等价物，或自研简单附加数据存储。
4. **MC API 变更（2748 "找不到符号" 的大头）**：渲染管线重写（`MultiBufferSource`、`VertexFormat`、自定义 shader `bone_skin.fsh/.vsh`、`rip.ysm.gpu`、`@BufferBuilderMapping`/BufferBuilder mixin）——1.21.5+ GpuBuffer/RenderPipeline 体系，改动量最大。小项：`UseAnim`、`Parrot`、`TickEvent`(Forge)→Fabric `ClientTickEvents`。
5. **已禁用/待按需恢复**：`rip.ysm.compat.*`（原依赖 `libs/` 里 1.20.1 jar）、`net.irisshaders.iris`（需换 26.3 版 iris 依赖）、ImageStream（JitPack `com.github.TartaricAlkaline:ImageStream`，已在 build.gradle，待验证 26.3 兼容）、natives（`common/src/main/resources/natives` 预编译，旧 `compileNative` CMake 任务已随 forge 构建一起废弃）。

## 建议的推进顺序（每步可独立提交）

1. 移除 Architectury（机械替换，独立可编译性最好）
2. 配置系统重写
3. Cardinal Components → 原生方案
4. 非渲染类 MC API 适配（实体/物品/网络 CustomPayload/命令）
5. 渲染层（最大坑，最后做；先非 shader 路径，再 GPU/shader）
6. compat 逐个恢复（每个单独提交）
