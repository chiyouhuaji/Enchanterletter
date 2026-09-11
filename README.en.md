# Enchanter Letter

[![License: MPL 2.0](https://img.shields.io/badge/License-MPL_2.0-brightgreen.svg)](https://opensource.org/licenses/MPL-2.0)

[简体中文](./README.md) | [English](./README.en.md)

**Enchanter Letter** is an optional companion mod for *UsefulMagic* that adds growable, customizable **Enchanter Letters** and **Letter Binders**: simply keep them in your inventory, accessory slots, or a binder to gain damage bonuses and defensive attributes — no need to hold them. All four branches are feature-equivalent (only the underlying implementation differs) and have no mandatory dependencies (UsefulMagic, Curios, and Accessories are all optional, reflection-based integrations). See `模组介绍.txt` for the full player-facing feature guide.

This repository also serves as the build / development documentation for developers who want to build from source, extend, or integrate the mod's API.

---

## Versions & Branches

All four branches are feature-equivalent; they differ only in loader, Java version, and how item data is stored:

| Directory | Minecraft | Loader / Runtime | Java | Item Data Storage |
| --- | --- | --- | --- | --- |
| `neoforge 1.21.1` | 1.21.1 | NeoForge 21.1.x | Java 21 | Data Components |
| `fabric 1.21.1` | 1.21.1 | Fabric Loader 0.16.10 + Fabric API 0.110.0+1.21.1 | Java 21 | Data Components |
| `forge 1.20.1` | 1.20.1 | Forge 47.1.x | Java 17 | NBT |
| `fabric 1.20.1` | 1.20.1 | Fabric Loader 0.15.11 + Fabric API 0.92.2+1.20.1 | Java 17 | NBT |

Pick the directory matching your Minecraft version and loader. The 1.21.1 branches use **Data Components**; the 1.20.1 branches use **NBT** — identical in gameplay.

---

## Building from Source

All four branches are standard Gradle projects. Run this inside the matching directory:

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

Use the correct JDK:

- `neoforge 1.21.1` / `fabric 1.21.1`: Java 21.
- `forge 1.20.1` / `fabric 1.20.1`: Java 17 (or a newer JDK compiling `release 17`).

Outputs are in each branch's `build/libs/`, named uniformly as

```
enchanter_letter_<loader>-<minecraft_version>-<mod_version>.jar
```

where `<loader>` is `neoforge` / `fabric` / `forge`, and the version comes from that branch's `mod_version` in `gradle.properties`. For example, neoforge 1.21.1 at `mod_version=3.0.0` produces `enchanter_letter_neoforge-1.21.1-3.0.0.jar`.

---

## Development Notes

### Versioning

- To bump the version, change `mod_version` in **each branch's** `gradle.properties` (currently `3.0.0`).
- Each branch's loader metadata references it via `${mod_version}` (e.g. `neoforge.mods.toml` / `mods.toml` / `fabric.mod.json`), so it expands automatically at build time — no manual edits.
- Keep `mod_version` identical across all four branches.

### Branch Synchronization

- Keep all four branches **feature-equivalent**: any feature change should be applied to all four implementations.
- Shared logic files (`ModConfig`, `LetterCommands`, `ModCommonEvents`, `ModConfigClient`, `lang`, etc.) can be copied directly from `neoforge 1.21.1`.
- However, the 1.20.1 branches must replace the **Data Components API with the NBT API** (see each branch's `version-diffs.md`); copying the 1.21.1 component API directly into the 1.20.1 branches will not compile.

### Datagen & Mixin

- Branches need to run data generation (datagen) to emit tags and models; confirm each branch's generation task and output path.
- Fabric branches modify damage via Mixin (`LivingEntity.hurt()`); Mixin mappings must match the loader / MC version mapping channel, or it crashes at runtime.

---

## Developer API

A small set of static APIs is exposed for integration:

### `cn.autoforged.enchanter_letter.enchantment.ModEnchantments`

| Method | Description |
| --- | --- |
| `addVanishing(stack[, registries])` | Adds the forced-vanishing enchantment to an item (vanilla Curse of Vanishing). The 1.21.1 branches take an extra `RegistryAccess`; the 1.20.1 branches take only `ItemStack`. |
| `registerForceVanishWhitelist(stack)` | Adds the item to the force-vanish whitelist. |
| `addMagicConversion(stack[, registries])` | Adds the magic conversion enchantment to an item. Signature as above (1.21.1 carries `RegistryAccess`). |
| `addGlowing(stack[, registries])` | Adds the glowing enchantment to an item. |
| `addMagicBinding(stack[, registries])` | Adds the magic binding enchantment to an item. |
| `getEffectiveDamageType(stack)` | Returns the item's currently effective `ResourceLocation` damage type (default type when not converted). |

Note: the 1.21.1 branches' `add*` methods need `RegistryAccess` (to resolve the damage-type registry); the 1.20.1 branches need only `ItemStack`.

### `cn.autoforged.enchanter_letter.event.ModCommonEvents`

| Method | Description |
| --- | --- |
| `shouldVanishClear(stack)` | Returns whether the item should be handled by forced vanishing. |

### Data Storage & Events / Hooks

- **Data Components vs NBT**: the 1.21.1 branches use data components (`DataComponentType`) to store letter level, multiplier, binding, potion entries, etc.; the 1.20.1 branches use NBT. Use the matching read/write approach per branch when integrating.
- **Events / Hooks**: damage lands on `LivingIncomingDamageEvent` / `LivingDamageEvent.Pre` in NeoForge, `LivingHurtEvent` in Forge, and a Mixin in Fabric. Binding/ejection, death retention, and drop protection use each branch's corresponding event points.

---

## Installation

1. Choose the JAR for your **Minecraft version + loader** (see the table above).
2. Put it into `.minecraft/mods/` (or a modpack / server's `mods/` folder).
3. Start the game / server; config files generate on first launch (`config/enchanter_letter.json` and `config/enchanter_letter_client.json`).

No mandatory dependencies. NeoForge / Forge versions only need the platform itself; Fabric versions need Fabric Loader and Fabric API. UsefulMagic, Curios, and Accessories are all optional integrations.

> On update, remove old JARs with the same mod ID first to avoid conflicts.

---

## Open Source License

This project is released under the **Mozilla Public License 2.0 (MPL-2.0)**.

- Full text: <https://www.mozilla.org/en-US/MPL/2.0/>
- Summary: <https://opensource.org/licenses/MPL-2.0>

Secondary developers must keep the original copyright and license notices; modifications to MPL-2.0-covered source files must still be offered under MPL-2.0 with the changes noted. This project is provided "AS IS", without warranty of any kind, express or implied.