# 魔法手札（Enchanter Letter）

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

[简体中文](./README.md) | [English](./README.en.md)

《魔法手札》是《UsefulMagic》的一个可选联动增强模组，添加可成长、可定制的**魔法手札（Enchanter Letter）**与**手札合订本（Letter Binder）**：放入背包、饰品栏或合订本内即可获得伤害增幅与防御属性，无需手持。四个分支功能完全同步，仅底层实现不同；无强制依赖（UsefulMagic、Curios、Accessories 均为可选反射兼容）。完整玩家向功能介绍见 `模组介绍.txt`。

本仓库同时是开发/构建文档，面向希望从源码构建、二次开发或集成本模组 API 的开发者。

---

## 版本与分支

四个分支功能完全相同，仅加载器、Java 版本与物品数据存储方式不同：

| 目录 | Minecraft | 加载器 / 运行时 | Java | 物品数据存储 |
| --- | --- | --- | --- | --- |
| `neoforge 1.21.1` | 1.21.1 | NeoForge 21.1.x | Java 21 | Data Components |
| `fabric 1.21.1` | 1.21.1 | Fabric Loader 0.16.10 + Fabric API 0.110.0+1.21.1 | Java 21 | Data Components |
| `forge 1.20.1` | 1.20.1 | Forge 47.1.x | Java 17 | NBT |
| `fabric 1.20.1` | 1.20.1 | Fabric Loader 0.15.11 + Fabric API 0.92.2+1.20.1 | Java 17 | NBT |

按你的 Minecraft 版本与加载器选择对应目录即可。1.21.1 使用**数据组件（Data Components）**，1.20.1 使用 **NBT**；二者在玩法上完全一致。

---

## 从源码构建

四个分支均为标准 Gradle 项目。进入对应目录后执行：

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

使用正确的 JDK：

- `neoforge 1.21.1` / `fabric 1.21.1`：Java 21。
- `forge 1.20.1` / `fabric 1.20.1`：Java 17（也可用更新的 JDK 编译 `release 17`）。

产物位于各分支的 `build/libs/`，命名统一为

```
enchanter_letter_<loader>-<minecraft_version>-<mod_version>.jar
```

其中 `<loader>` 为 `neoforge` / `fabric` / `forge`，版本号取自该分支 `gradle.properties` 的 `mod_version`。例如 neoforge 1.21.1 在 `mod_version=3.0.0` 时产物为 `enchanter_letter_neoforge-1.21.1-3.0.0.jar`。

---

## 开发备注

### 版本号

- 升级版本时，在**每个分支**的 `gradle.properties` 中修改 `mod_version`（当前为 `3.0.0`）。
- 各分支的加载器元数据通过 `${mod_version}` 引用该值（如 `neoforge.mods.toml` / `mods.toml` / `fabric.mod.json`），无需重复手改，构建时自动展开。
- 请四个分支保持 `mod_version` 一致。

### 分支间同步

- 四个分支需保持**功能对等**：任何功能调整都应同步到四个实现。
- 逻辑共享的文件（`ModConfig`、`LetterCommands`、`ModCommonEvents`、`ModConfigClient`、`lang` 等）可从 `neoforge 1.21.1` 直接复制。
- 但 1.20.1 分支必须把 **Data Components API 替换为 NBT API**（详见各分支 `version-diffs.md`）；直接照搬 1.21.1 的组件 API 到 1.20.1 无法编译。

### 数据生成与 Mixin

- 分支需要运行数据生成（datagen）以输出标签与模型，注意确认各分支的生成任务与输出路径。
- Fabric 分支通过 Mixin 修改伤害参数（`LivingEntity.hurt()`），Mixin 映射需与对应加载器/MC 版本的映射通道一致，否则运行时崩溃。

---

## 开发者 API

本模组开放少量静态 API 供其他模组集成：

### `cn.autoforged.enchanter_letter.enchantment.ModEnchantments`

| 方法 | 说明 |
| --- | --- |
| `addVanishing(stack[, registries])` | 给物品附加强制消失附魔（原版消失诅咒）。1.21.1 分支多传 `RegistryAccess`，1.20.1 分支仅传 `ItemStack`。 |
| `registerForceVanishWhitelist(stack)` | 把物品加入强制消失白名单。 |
| `addMagicConversion(stack[, registries])` | 给物品附加魔法转化附魔。签名同上（1.21.1 带 `RegistryAccess`）。 |
| `addGlowing(stack[, registries])` | 给物品附加光灵附魔。 |
| `addMagicBinding(stack[, registries])` | 给物品附加魔法绑定附魔。 |
| `getEffectiveDamageType(stack)` | 返回物品当前生效的伤害类型 `ResourceLocation`（未转化时取默认类型）。 |

注意：1.21.1 分支的 `add*` 方法需要 `RegistryAccess`（用于解析伤害类型注册表）；1.20.1 分支仅需要 `ItemStack`。

### `cn.autoforged.enchanter_letter.event.ModCommonEvents`

| 方法 | 说明 |
| --- | --- |
| `shouldVanishClear(stack)` | 判断物品是否应被强制消失处理。 |

### 数据存储与事件/钩子

- **Data Components vs NBT**：1.21.1 分支用数据组件（`DataComponentType`）存储手札等级、倍率、绑定、药水条目等；1.20.1 分支用 NBT。集成时按分支选用对应读写方式。
- **事件/钩子**：伤害结算在 NeoForge 走 `LivingIncomingDamageEvent` / `LivingDamageEvent.Pre`，Forge 走 `LivingHurtEvent`，Fabric 走 Mixin。绑定/弹出、死亡保留、掉落保护等走各分支对应的事件点。

---

## 安装

1. 按 **Minecraft 版本 + 加载器** 选择对应 jar（见上表）。
2. 放入 `.minecraft/mods/`（或整合包/服务端的 `mods/` 目录）。
3. 启动游戏/服务端，首次启动会生成配置文件（`config/enchanter_letter.json` 与 `config/enchanter_letter_client.json`）。

无强制依赖。NeoForge / Forge 版仅需本体；Fabric 版需 Fabric Loader 与 Fabric API；UsefulMagic、Curios、Accessories 等均为可选联动。

> 更新时先删除旧版本同 modid 的 jar，避免冲突。

---

## 开源许可

本项目以 **Mozilla Public License 2.0（MPL-2.0）** 开源发布。

- 官方全文：<https://www.mozilla.org/en-US/MPL/2.0/>
- 许可摘要：<https://opensource.org/licenses/MPL-2.0>

二次开发者请保留原始版权与许可声明；对 MPL-2.0 覆盖的源文件的修改仍需在 MPL-2.0 下提供，并注明改动。本项目按"AS IS"提供，不附带任何明示或默示担保。