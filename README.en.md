# Enchanter Letter

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

**Enchanter Letter** is a Minecraft mod supporting **Minecraft 1.21.1 / 1.20.1** with four branches for **NeoForge / Forge / Fabric**. It adds a set of growable, customizable **Enchanter Letters** and **Letter Binders**: simply keep them in your inventory, accessory slots, or a binder to gain damage bonuses and defensive attributes — no need to hold them.

[简体中文](./README.md) | [English](./README.en.md)

- All four branches are feature-synchronized; only the underlying implementations differ.
- No mandatory mod dependencies. `UsefulMagic`, `Curios`, `Accessories`, `Terra Curio`, etc. are all optional, reflection-based integrations.
- The mod works on `usefulmagic:magic` damage by default, and can be extended to any registered damage type through configuration and commands.

---

## Table of Contents

- [Feature Overview](#feature-overview)
- [Versions & Branches](#versions--branches)
- [1.20.1 vs 1.21.1 Differences](#1201-vs-1211-differences)
- [Installation](#installation)
- [Building from Source](#building-from-source)
- [Configuration Files](#configuration-files)
- [Command Reference](#command-reference)
- [Open Source License & Usage Notice](#open-source-license--usage-notice)

---

## Feature Overview

### 1. Letter Types & Progression

Each letter has a level (Lv). Higher levels grant stronger bonuses, and levels are automatically gained by performing the corresponding activity:

| Letter | Level-Up Condition (default) | Damage Bonus per Level |
| --- | --- | --- |
| Experience Letter | Gain 1,000 XP | +10% |
| Kill Letter | Kill 50 monsters | +10% |
| Fishing Letter | Catch 100 fish | +10% |
| Travel Letter | Walk 5,000 blocks / fly 20,000 blocks | +10% |
| Treasure Letter | Open 20 naturally-generated chests | +10% |
| Time Letter | 3,600 seconds of world uptime | +10% |
| Tenacity Letter | Take 10,000 damage | +10% |
| Hero Letter | Win 1 raid | +10% at low levels, +20% from level 4 |
| Stage Letter | Fixed multiplier, no progression | 100% – 1000% (configurable) |
| Custom Letter | Always level 0 | Reads item data directly; no progression/config participation |

All values above are configuration defaults and are adjustable, except for the Custom Letter.

For the eight growth letters (experience/kill/fishing/travel/treasure/time/tenacity/hero), the per-level parameters (required count per level, multiplier per level, per-level defense growth) are stored in the item's own data components: `/letterset` run with an empty main hand modifies and writes back the config, while holding the matching letter writes directly into that item's data (holding any other item does nothing). The config defaults only serve as initial values for freshly obtained letters (Creative tab / `/give`). After `/letterset` modifies an item, its displayed/computed values recompute immediately, and the item tooltip shows the per-level multiplier (four rates — damage/armor/toughness/resistance — hidden when 0 or empty; the Travel letter's flight part and the Hero letter's high-level part additionally show a secondary set of four rates) and the required count per level (hidden when non-positive), stored in that item's data.

### 2. Damage Bonus Rules

- A letter in your **inventory, accessory slots, or a Letter Binder** counts as carried — you do not need to hold it.
- By default, it boosts `usefulmagic:magic` damage. You can add any registered damage type through `default_bonus_damage_types`.
- Actual multiplier = letter level × bonus per level. The item tooltip and HUD show the current real multiplier.
- Stacking rules are supported: `allow_multiple_letters` (whether multiple letters sum together) and `allow_same_letters` (whether identical letters each count).

### 3. Binding System

- **Sneak + long-press right-click for about 2 seconds** while holding a letter (or an empty Letter Binder) to bind/unbind it.
- A bound letter only works while held by its bound player. If someone else picks it up, it will be automatically "ejected" as an item entity.
- Inventory, accessory slots, and Letter Binder contents are all continuously checked.

### 4. Item Entity Protection

Dropped letters and binders:

- Are immune to all damage, including explosions and cacti;
- Have no gravity, are not pushed by water, and always have zero velocity;
- Cannot be sucked up by hoppers / hopper minecarts.

### 5. Letter Binder

- Works like the vanilla Bundle: right-click to store/retrieve, inventory interactions, and hover to preview contents.
- Letters are not stackable and occupy one slot each; the binder natively holds up to 64 letters.
- Letters inside a binder count as "carried" and participate in damage bonuses and ejection checks.
- An empty binder can be bound; a non-empty binder ejects all contents at once when sneak-right-clicked.

### 6. Magic Conversion Enchantment (`magic_conversion`)

- Once enchanted, the letter no longer uses the default damage type; instead it resolves against a specified damage type.
- Use `/letterdamage <damage_type>` to set the conversion type.
- In Creative mode, you can right-click an enchanted book to open a client-side screen and directly type a damage type ID.
- Converted damage of the same type is merged and resolved with a delayed tick; different conversion types do not interfere with each other.

### 7. Accessory Slot Compatibility

- **NeoForge**: Compatible with Curios API, including third-party slots such as Terra Curio.
- **Fabric**: Compatible with Accessories and Curios (Fabric).
- All integrations are reflection-based and have no effect when the corresponding mod is not installed.

### 8. HUD Display

- Shows the currently active letters in the top-left corner: item name, level, and actual multiplier; converted letters also show their damage type.
- Press `N` (default) to cycle through displays: damage bonus → armor → armor toughness → resistance → off.
- Gray text means "currently inactive" (suppressed by a higher multiplier); white text means active.

### 9. Defensive Attributes

All letters provide three defensive attributes while carried:

- **Armor**: Directly adds armor as a vanilla attribute modifier, visible in the armor bar.
- **Armor Toughness**: Directly adds armor toughness.
- **Resistance**: Reduces incoming damage by a percentage (before armor calculation). Disabled by default; the server cap is 80% by default and can be changed with `/letterresistance limit`.
- When `allow_multiple_letters` is disabled, each attribute independently uses the highest value among all letters; when enabled, they add together.
- Defensive attributes are not affected by the `magic_conversion` enchantment.

### 10. Enchantment System

- **Glowing**: Can be applied to letters/binders to make the carrier glow. The glow color is controlled by `/lettercolor`; mobs are also supported.
- **Magic Binding (`magic_binding`)**: Keeps the item on death and returns it after respawn; accessory slot items are restored to their original slot where possible.
- **Vanishing Curse (`vanishing_curse`)**: The mod's items can receive the vanilla Curse of Vanishing and are destroyed on death. Use `/lettervanish` to enable forced vanishing.

### 11. Mob Wielding & Admin Commands

- When a mob carries letters in its main hand, off hand, armor, or accessory slots, **all damage it deals** is boosted (players still strictly follow the configured damage types).
- `/letterstorage` lets you capture an entity, swap items with its main hand/off hand/armor slots, and export its NBT.
- Management commands such as `/letterback` (retrieve dropped items), `/letterclean` (scheduled cleanup), and `/letterbinding` (ejection whitelist) are also provided.

### 12. Potion-Effect Entries (`/lettereffect`)

- Every magic letter can carry any number of potion-effect entries in its own NBT (`letter_potions`, empty by default — no config entries needed); the effects apply to the holder while the letter is carried (inventory / armor / off hand / accessory slots / binder contents).
- Manage entries with `/lettereffect add|delete|ls` (requires holding the letter in the main hand). `add` writes into a given `<slot>` (overwriting it if already present, otherwise creating it); `delete` removes the entry at a `<slot>` (the slot can be reused afterward). Two trigger modes: by world day-time (multiple points per day) or by world total game time (start tick + interval).
- Timing follows the vanilla `/time query` sources: `gametime` uses the world total game time (`getGameTime()`, monotonic, never rewinds), `daytime` uses the world day time (`getDayTime()`, affected by `/time set`); both are re-read every poll slot — there is no per-player timer.
- If a trigger falls while the holder is offline or its chunk is unloaded, on login/load the mod back-computes the most recent expected trigger time; if the recorded `seconds` duration has not yet elapsed, it re-applies the effect with the remaining duration (works for both daytime and gametime modes).
- Each re-apply recalibrates the duration (it first removes the existing effect and re-adds it with the current remaining duration), so even if world time jumps (e.g. `/time set`) the effect duration is corrected to the exact value — it cannot run longer or shorter than it should.

---

## Versions & Branches

| Directory | Minecraft | Loader / Runtime | Java | Item Data Storage | Damage Modification |
| --- | --- | --- | --- | --- | --- |
| `neoforge-1.21.1` | 1.21.1 | NeoForge 21.1.x | Java 21 | Data Components | Events (`LivingIncomingDamageEvent` + `LivingDamageEvent.Pre`) |
| `fabric-1.21.1` | 1.21.1 | Fabric Loader 0.16.10 + Fabric API 0.110.0+1.21.1 | Java 21 | Data Components | Mixin modifies damage parameter in `LivingEntity.hurt()` |
| `forge-1.20.1` | 1.20.1 | Forge 47.1.x | Java 17 (can build `release 17` with JDK 21) | NBT | Event (`LivingHurtEvent`) |
| `fabric-1.20.1` | 1.20.1 | Fabric Loader 0.15.11 + Fabric API 0.92.2+1.20.1 | Java 17 (can build `release 17` with JDK 21) | NBT | Mixin modifies damage parameter in `LivingEntity.hurt()` |

The **items, mechanics, configuration, commands, HUD, binding/ejection, item entity protection, magic conversion, and enchantments** are identical across all four branches. Pick the directory matching your loader and Minecraft version.

---

## 1.20.1 vs 1.21.1 Differences

Both versions are feature-complete and synchronized; the main differences are in the underlying implementation:

| Aspect | 1.21.1 (NeoForge / Fabric) | 1.20.1 (Forge / Fabric) |
| --- | --- | --- |
| Data storage | **Data Components** | **NBT** |
| Damage calculation | NeoForge uses events; Fabric uses Mixin | Forge uses `LivingHurtEvent`; Fabric uses Mixin |
| Curios item tag path | `data/curios/tags/item/` (singular) | `data/curios/tags/items/` (plural) |
| Binder capacity implementation | `BundleContents.getWeight` returns `Fraction`; Mixin changes it to `1/64` | `BundleItem.getWeight` returns `int`; Mixin changes it to `1` |
| Forge port adaptation | Not needed | Automatically compatible with MesdagPortLib, syncing amplified damage into its `PortDamageContainer` |
| Fabric third-party compatibility | Normal Accessories integration | Extra handling for Accessories beta.48's unconditional Sodium dependency crash (registers an empty renderer) |

> For developer-focused API differences (attribute registration, `ResourceLocation`, `AttributeModifier`, Mixin mappings, etc.), refer to the source comments and development documentation in each branch.

---

## Installation

1. Choose the JAR for your **Minecraft version + loader**.
2. Put the JAR into `.minecraft/mods/` (or the `mods/` folder of a modpack / server).
3. Start the game / server. Configuration files are generated on first launch.

Dependency notes:

- **NeoForge 1.21.1**: Requires only NeoForge and Minecraft; no mandatory dependencies.
- **Forge 1.20.1**: Requires only Forge and Minecraft; no mandatory dependencies.
- **Fabric 1.21.1 / 1.20.1**: Requires Fabric Loader and Fabric API; no other mandatory dependencies.
- `UsefulMagic`, `Curios`, `Accessories`, `Terra Curio`, and `MesdagPortLib` are all **optional integrations**; the mod runs standalone without them.

> When updating, remove old JARs with the same mod ID first to avoid conflicts.

---

## Building from Source

All four branches are standard Gradle projects. Run this in the corresponding directory:

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Use the correct JDK when building:

- `neoforge-1.21.1` / `fabric-1.21.1`: Java 21.
- `forge-1.20.1` / `fabric-1.20.1`: Java 17 (a newer JDK can be used to build `release 17`).

Build outputs are located in each branch's `build/libs/`, e.g. `enchanter_letter-1.0.0.jar`.

---

## Configuration Files

Generated after the first launch:

- `config/enchanter_letter.json`: server / main configuration.
  - Letter growth values, stage letter multipliers, stacking rules, magic conversion blacklist, default damage types, default glow colors, scheduled cleanup, respawn restore delay, resistance cap, etc.
  - `letter_cleanup`: in addition to the `target_uuids` UUID list, supports `clean_all_binding` (clean all bound) and `clean_all_normal` (clean all unbound) letter/binder drops; both can be enabled at the same time.
  - `letter_enchanted`: toggle for the enchanted book injection and the appearance probabilities of the three enchantments. Defaults: `glowing_chance` 0.1, `magic_binding_chance` 0.08, `magic_conversion_chance` 0.0.
- `config/enchanter_letter_client.json`: client configuration.
  - HUD default visibility and toggle key (default `N`, GLFW key code 78).

Changing the config files requires restarting the game/server. Changes made through commands take effect immediately and are written back to the config file (both `/letterenchanted` and `/letterclean` support this; for `/letterset`, `stage_1`~`stage_10` and the eight growth letters run with an empty main hand write back to the config, while growth letters held in the main hand and `custom` letters write directly into the held item's data).

---

## Command Reference

All mod commands require permission level 2/3/4:

| Command | Description |
| --- | --- |
| `/lettermulti [true|false]` | Toggle multiple letters being active at once |
| `/lettersame [true|false]` | Toggle identical letters each counting |
| `/letterset <type> <param> <value>` | Modify letter growth / multiplier / defensive attributes (growth letters: with an empty main hand the command modifies and writes back the config; holding the matching letter writes into its item data; holding any other item does nothing. custom requires holding the matching letter and writes into its item data; stage letters write to config) |
| `/lettertype add|delete <damage_type>` | Add / remove default bonus damage types |
| `/letterentity add|delete <entity_type>` | Add / remove magic conversion blacklist entities |
| `/letterdamage <damage_type>` | Set the damage type of the held conversion letter |
| `/letterback [player]` | Retrieve a player's bound letter/binder item entities |
| `/lettercolor [red green blue]` | Query / set the glowing color |
| `/letterenchanted [true\|false\|glowing <p>\|binding <p>\|conversion <p>]` | Query / toggle the enchanted book injection and adjust the three enchantment probabilities |
| `/letterclean ...` | Query / toggle / configure scheduled cleanup (including `allbinding` / `allnormal` bulk cleanup) |
| `/letterdelay [tick]` | Query / set the respawn restore delay |
| `/letterresistance ...` | Query / toggle / set the resistance cap |
| `/lettervanish ...` | Query / toggle the forced vanishing mechanic |
| `/letterbinding ...` | Manage binding whitelist and modify bound UUID |
| `/lettereffect add\|delete\|ls` | Manage potion-effect entries on the held magic letter (`add` writes/overwrites a given `<slot>`, `delete` removes by slot; stored in the item's own NBT, empty by default — no config; triggers on the world clock) |
| `/letterstorage ...` | Capture an entity, swap items, export NBT (`nbt <uuid\|player>` locks a target entity directly by UUID or online player name) |

---

## Open Source License & Usage Notice

This project is released as open source under the **Mozilla Public License 2.0 (MPL-2.0)**.

Full license text:

- Official text: <https://www.mozilla.org/en-US/MPL/2.0/>
- License summary: <https://opensource.org/licenses/MPL-2.0>

Users and secondary developers are asked to follow these basic rules:

1. **Retain notices**: When copying, modifying, or distributing this project, do not remove or alter the original copyright, author, and license notices.
2. **Share modifications**: If you modify source files covered by MPL-2.0, the modified files must continue to be provided under MPL-2.0, and your changes should be noted.
3. **Source availability**: If you distribute binaries / build artifacts, you must also provide a way to obtain the corresponding source code (e.g. a repository link or source archive).
4. **No strong copyleft on the whole pack**: MPL-2.0 is a relatively permissive file-level license. It does not force an entire modpack or addon to be open source; however, files derived from this project must still follow these rules.
5. **Disclaimer**: This project is provided "AS IS", without warranty of any kind, express or implied.