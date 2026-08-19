# 魔法手札（Enchanter Letter / Magic Letter）

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

《魔法手札》是一个支持 **Minecraft 1.21.1 / 1.20.1** 的模组，同时提供 **NeoForge / Forge / Fabric** 四个分支。模组添加了一系列可成长、可定制的 **魔法手札（Magic Letter）** 与 **手札合订本（Letter Binder）**：把它们放进背包、饰品栏或合订本中即可获得伤害增幅与防御属性，无需手持。

- 四个分支功能完全同步，仅底层实现不同。
- 无强制模组依赖，`UsefulMagic`、`Curios`、`Accessories`、`Terra Curio` 等均为可选的反射兼容联动。
- 默认对 `usefulmagic:magic` 伤害类型生效，也可通过配置与命令扩展支持任意已注册伤害类型。

---

## 目录

- [功能总览](#功能总览)
- [版本与分支](#版本与分支)
- [1.20.1 与 1.21.1 的版本区别](#1201-与-1211-的版本区别)
- [安装方法](#安装方法)
- [从源码构建](#从源码构建)
- [配置文件](#配置文件)
- [命令一览](#命令一览)
- [开源协议与使用提醒](#开源协议与使用提醒)

---

## 功能总览

### 1. 手札种类与成长方式

每张手札都有等级（Lv），等级越高增益越高；等级通过对应行为自动积累：

| 手札 | 升级条件（默认值） | 每级伤害增幅 |
| --- | --- | --- |
| 经验魔法手札 | 每获得 1000 点经验 | +10% |
| 杀戮魔法手札 | 每击杀 50 只怪物 | +10% |
| 垂钓魔法手札 | 每钓起 100 条鱼 | +10% |
| 旅行魔法手札 | 行走 5000 格 / 飞行 20000 格 | +10% |
| 宝藏魔法手札 | 每开启 20 个自然宝箱 | +10% |
| 时间魔法手札 | 每 3600 秒世界开启时间 | +10% |
| 坚韧魔法手札 | 每承受 10000 点伤害 | +10% |
| 英雄手札 | 每赢得 1 次袭击 | 低等级 +10%，第 4 级起 +20% |
| 阶段魔法手札 | 固定倍率，不成长 | 100% ~ 1000%（可配置） |
| 定制手札 | 等级恒为 0 | 直接读取物品数据，不参与成长/配置 |

以上均为配置文件默认值，除定制手札外全部可调。

### 2. 伤害增幅生效规则

- 手札放在 **背包、饰品栏或合订本内** 即视为携带，无需手持。
- 默认增幅 `usefulmagic:magic` 类型伤害，可通过 `default_bonus_damage_types` 添加任意已注册伤害类型。
- 实际倍率 = 手札等级 × 每级增幅；物品详情与 HUD 显示当前实际倍率。
- 支持同名手札堆叠规则：`allow_multiple_letters`（多张是否求和）与 `allow_same_letters`（完全相同手札是否重复生效）。

### 3. 绑定系统

- 手持手札（或空合订本）**潜行 + 长按右键约 2 秒** 可绑定/解绑。
- 绑定后仅绑定玩家持有才生效；他人持有会被自动“弹出”为掉落物。
- 背包、饰品栏、合订本内容物都会持续检测。

### 4. 掉落物保护

手札与合订本掉落物：

- 免疫爆炸、仙人掌等一切伤害；
- 无重力、不受水流推动、速度始终为 0；
- 不会被漏斗 / 漏斗矿车吸入。

### 5. 手札合订本（Letter Binder）

- 用法与原版收纳袋一致：右键存取、物品栏交互、悬停查看内容物。
- 手札不可堆叠，一张占一格容量，原生最多容纳 64 张。
- 合订本内的手札视为“已携带”，正常参与伤害增幅与绑定弹出检测。
- 空合订本可绑定；非空时潜行右键一次性放出全部内容物。

### 6. 魔法转化附魔（magic_conversion）

- 附魔后，手札不再按默认伤害类型结算，而是按指定伤害类型结算。
- 使用 `/letterdamage <伤害类型>` 设置转化类型。
- 创造模式下可对附魔书右键打开客户端设置界面输入伤害类型 ID。
- 同类型转化伤害会合并后延迟结算，不同转化类型互不干扰。

### 7. 饰品栏兼容

- NeoForge：兼容 Curios API（包括 Terra Curio 等第三方槽位）。
- Fabric：兼容 Accessories 与 Curios（Fabric）。
- 全部通过反射实现，未安装对应模组时完全不影响运行。

### 8. HUD 显示

- 屏幕左上角实时显示当前生效手札：物品名、等级、实际倍率；转化手札额外显示伤害类型。
- 默认按键 `N` 循环切换显示：伤害增益 → 护甲值 → 护甲韧性 → 抗性提升 → 关闭。
- 灰字表示“当前未生效”（被更高倍率压制），白字表示正在生效。

### 9. 防御属性

所有手札随身携带即提供三种防御属性：

- **护甲值（armor）**：以原版属性 modifier 形式直接增加护甲，护甲条可见。
- **护甲韧性（toughness）**：直接增加护甲韧性。
- **抗性提升（resistance）**：按比例减免伤害（护甲结算前），默认不生效，服务器上限默认 80%，可用 `/letterresistance limit` 调整。
- 未开启 `allow_multiple_letters` 时，各属性独立取最大值；开启时相加。
- 防御属性不受“魔法转化”附魔影响。

### 10. 附魔系统

- **光灵（glowing）**：手札/合订本可附魔，携带者发光颜色由 `/lettercolor` 控制；生物同样支持。
- **魔法绑定（magic_binding）**：死亡不掉落，重生后自动归还；饰品栏物品尽量还原原位。
- **消失诅咒（vanishing_curse）**：本模组物品可附原版消失诅咒，死亡时销毁；可用 `/lettervanish` 开启强制消失。

### 11. 生物持有与运维命令

- 生物主手/副手/装备/饰品槽携带手札时，其 **所有伤害** 都会获得增益（玩家仍严格匹配配置伤害类型）。
- `/letterstorage` 可截取实体并与其主手/副手/盔甲槽交换物品、导出 NBT。
- 提供 `/letterback` 找回掉落物、`/letterclean` 定时清理、`/letterbinding` 弹出白名单等管理命令。

---

## 版本与分支

| 目录 | Minecraft | 加载器 / 运行环境 | Java | 物品数据存储 | 伤害修改方式 |
| --- | --- | --- | --- | --- | --- |
| `neoforge-1.21.1` | 1.21.1 | NeoForge 21.1.x | Java 21 | Data Components | 事件（`LivingIncomingDamageEvent` + `LivingDamageEvent.Pre`） |
| `fabric-1.21.1` | 1.21.1 | Fabric Loader 0.16.10 + Fabric API 0.110.0+1.21.1 | Java 21 | Data Components | Mixin 修改 `LivingEntity.hurt()` 伤害参数 |
| `forge-1.20.1` | 1.20.1 | Forge 47.1.x | Java 17（可用 JDK 21 构建 release 17） | NBT | 事件（`LivingHurtEvent`） |
| `fabric-1.20.1` | 1.20.1 | Fabric Loader 0.15.11 + Fabric API 0.92.2+1.20.1 | Java 17（可用 JDK 21 构建 release 17） | NBT | Mixin 修改 `LivingEntity.hurt()` 伤害参数 |

四个分支的 **物品、机制、配置、命令、HUD、绑定/弹出、掉落物保护、魔法转化、附魔** 均保持一致，使用者只需按自己的加载器和 MC 版本选择对应目录的构建产物。

---

## 1.20.1 与 1.21.1 的版本区别

两者功能完全同步，主要差异在底层实现：

| 对比项 | 1.21.1（NeoForge / Fabric） | 1.20.1（Forge / Fabric） |
| --- | --- | --- |
| 数据存储 | **Data Components** | **NBT** |
| 伤害结算 | NeoForge 用事件、Fabric 用 Mixin | Forge 用 `LivingHurtEvent`、Fabric 用 Mixin |
| Curios 物品标签路径 | `data/curios/tags/item/`（单数） | `data/curios/tags/items/`（复数） |
| 合订本容量实现 | `BundleContents.getWeight` 返回 `Fraction`，Mixin 改为 `1/64` | `BundleItem.getWeight` 返回 `int`，Mixin 改为 `1` |
| Forge 移植适配 | 不需要 | 自动兼容 MesdagPortLib，把放大后伤害同步进其 `PortDamageContainer` |
| Fabric 第三方兼容 | Accessories 正常集成 | 额外处理 Accessories beta.48 对 Sodium 的无条件依赖崩溃（注册空渲染器） |

> 更多面向开发者的 API 差异（属性注册、`ResourceLocation`、`AttributeModifier`、Mixin 映射等）请参考各分支源码中的注释与开发文档。

---

## 安装方法

1. 根据你的 **Minecraft 版本 + 加载器** 选择对应分支的 jar。
2. 将 jar 放入 `.minecraft/mods/`（或整合包 / 服务端 `mods/` 目录）。
3. 启动游戏 / 服务端，首次启动会生成配置文件。

依赖说明：

- **NeoForge 1.21.1**：只需 NeoForge 本体与 Minecraft，无强制依赖。
- **Forge 1.20.1**：只需 Forge 本体与 Minecraft，无强制依赖。
- **Fabric 1.21.1 / 1.20.1**：需要 Fabric Loader 与 Fabric API；无需其他强制依赖。
- `UsefulMagic`、`Curios`、`Accessories`、`Terra Curio`、`MesdagPortLib` 均为 **可选联动**，未安装时模组可独立运行。

> 安装到旧版本目录前，请先移除同 modid 的旧 jar，避免冲突。

---

## 从源码构建

四个分支都是标准 Gradle 工程，在对应目录执行：

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

构建时需设置正确的 JDK：

- `neoforge-1.21.1` / `fabric-1.21.1`：Java 21。
- `forge-1.20.1` / `fabric-1.20.1`：Java 17（可直接使用高版本 JDK 构建 `release 17`）。

构建产物位于各分支的 `build/libs/` 下，例如 `enchanter_letter-1.0.0.jar`。

---

## 配置文件

首次启动后生成：

- `config/enchanter_letter.json`：服务端 / 主配置。
  - 各手札成长数值、阶段手札倍率、堆叠规则、魔法转化黑名单、默认伤害类型、光灵默认颜色、定时清理、重生返还延迟、抗性上限等。
- `config/enchanter_letter_client.json`：客户端配置。
  - HUD 默认显示状态与切换按键（默认 `N`，GLFW 键码 78）。

修改配置文件需重启游戏 / 服务端生效；通过命令修改会立即生效并写回配置文件。

---

## 命令一览

模组命令均需要权限等级 2/3/4：

| 命令 | 作用 |
| --- | --- |
| `/lettermulti [true|false]` | 开关多张手札同时生效 |
| `/lettersame [true|false]` | 开关完全相同手札重复生效 |
| `/letterset <type> <param> <value>` | 修改手札成长 / 倍率 / 防御属性 |
| `/lettertype add|delete <伤害类型>` | 增删默认增益伤害类型 |
| `/letterentity add|delete <实体类型>` | 增删魔法转化黑名单实体 |
| `/letterdamage <伤害类型>` | 设置手持转化手札的伤害类型 |
| `/letterback [玩家]` | 召回绑定的手札/合订本掉落物 |
| `/lettercolor [红 绿 蓝]` | 查询 / 设置光灵发光颜色 |
| `/letterclean ...` | 查询 / 开关 / 配置定时清理 |
| `/letterdelay [tick]` | 查询 / 设置重生返还延迟 |
| `/letterresistance ...` | 查询 / 开关 / 设置抗性减免上限 |
| `/lettervanish ...` | 查询 / 开关强制消失机制 |
| `/letterbinding ...` | 绑定白名单与修改绑定 UUID |
| `/letterstorage ...` | 截取实体、交换物品、导出 NBT |

---

## 开源协议与使用提醒

本项目以 **Mozilla Public License 2.0（MPL-2.0）** 开源发布。

完整的许可证文本请参见：

- 官方文本：<https://www.mozilla.org/en-US/MPL/2.0/>
- 开源许可证简介：<https://opensource.org/licenses/MPL-2.0>

请使用者与二次开发者遵守以下基本规则：

1. **保留声明**：复制、修改或分发本项目时，不得移除或篡改原始版权、作者与许可证声明。
2. **修改开源**：对 MPL-2.0 覆盖的源文件进行修改后，修改后的文件应继续以 MPL-2.0 提供，并说明修改内容。
3. **源码可得**：若发布二进制 / 构建产物，应同时提供可获取对应源代码的方式（例如附带仓库链接或源码包）。
4. **兼容组件不受传染**：MPL-2.0 是较宽松的“文件级”开源协议，不会强制整个整合包 / 附属模组全部开源；但涉及本项目文件的部分仍需遵守上述规则。
5. **免责声明**：本项目按“原样（AS IS）”提供，不附带任何明示或默示担保。