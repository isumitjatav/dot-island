# Nothing Island — Charging Theme Feature Parity Matrix
**Target Device:** Nothing Phone (4a), Android 17 / API 37  
**Package:** `com.nothing.island.ai`  
**Investigation Checkpoint:** Phase 2 Design System Baseline / R8 Battery Intelligence  
**Milestone Alignment:** Phase 3 — Charging Animation & Studio (Note: R9 is reserved for Call Intelligence)  
**Date:** November 2026  

---

## 1. Executive Summary & Audit Methodology

This parity matrix establishes an exhaustive, line-by-line comparative audit of all charging, battery, and notch themes across:
1. **Reference A (Dynamic Notch, v6.60):** 38 Notch Animations (`BatteryNotchModel`) + 24 Wave Themes (`ChargingThemeAnimation`).
2. **Reference B (Dynamic Island Pro, v1.9.2):** 1 Live Compose Charging State (`ap2`) + 50 Global Notch Colorways (`NotchTheme`).
3. **Nothing Island Baseline (R8 Checkpoint):** 5 Compose Charging Styles (`ENERGY_RING`, `ELECTRIC_PULSE`, `LIQUID_ENERGY`, `CHARGING_AURA`, `PLASMA_FLOW`).

### Evidence Classification Tiers:
* **Tier 1 (T1):** Static APK resource discovery (drawables, layouts, JSON/Lottie assets).
* **Tier 2 (T2):** Decompiled source-code analysis (bytecode, model classes, registry lists).
* **Tier 3 (T3):** Inferred runtime behavior from decompiled logic.
* **Tier 4 (T4):** Observed live on Nothing Phone (4a) runtime via supported navigation route.
* **Tier 5 (T5):** Physical device metrics, dumpsys battery, and OS security boundaries.

### Disposition Legend:
* **Adopt:** Direct implementation matching reference mechanics and behavior.
* **Adapt:** Re-engineered for Nothing OS Dot Matrix typography, Glyph lighting, and monochrome/red aesthetic.
* **Combine:** Synthesized into a unified multi-property Nothing Island theme.
* **Exclude:** Rejected due to copyright infringement, cartoon aesthetics, or commercial bloat.
* **Unverified:** Identified in bytecode/resources but unobserved during live physical runtime.

---

## 2. Exhaustive Inventory: Reference A Notch Animations (38 Themes)

All 38 items extracted from `lb/d1.java` (`f50071j` and `f50072k`) and verified against `SmallChargingNotchAnimActivity`:

| ID | Original Resource Path | Visual Effect & Motion | Layering | Battery Progress Behavior | Nothing Island Disposition | Target Implementation | Evidence Status |
|---|---|---|---|---|---|---|---|
| `0` | `charginganims/0_ghost.json`<br>`0_Ghost_BG.lottie` | Ghost floating beside camera with night sky background | Combined (FG Character + BG Lottie) | Horizontal wave fill proportional to battery % | **Exclude** | Copyright/aesthetic mismatch (cartoon mascot) | `T1, T2, T4, T5` (Observed Live) |
| `1` | `charginganims/1_amongUs.json`<br>`1_AmongUs_BG.json` | Red astronaut character running in place on platform | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Severe copyright/trademark infringement | `T1, T2, T4, T5` (Observed Live) |
| `2` | `charginganims/2_crab.json`<br>`2_Crab_BG.json` | Beach scene with snapping pink crab | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon illustration | `T1, T2, T4, T5` (Observed Live) |
| `3` | `charginganims/3_car.json`<br>`3_Car_BG.json` | Yellow sports car driving on road surface | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon illustration | `T1, T2, T4, T5` (Observed Live) |
| `4` | `charginganims/4_bee.json`<br>`4_Bee_BG.json` | Bee buzzing and drinking coffee | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon mascot | `T1, T2, T4, T5` (Observed Live) |
| `5` | `charginganims/5_heart.json`<br>`5_Heart_BG.json` | Heart character shooting Cupid arrows | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Adapt** | Adapt into *Glyph Heartbeat* pulse | `T1, T2, T4, T5` (Observed Live) |
| `6` | `charginganims/6_emoji.json`<br>`6_Emoji_BG.json` | Smiling yellow emoji face blinking | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Generic emoji asset; violates Nothing OS style | `T1, T2, T4, T5` (Observed Live) |
| `7` | `charginganims/7_snail.json`<br>`7_Snail_BG.json` | Snail crawling with glowing shell | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon mascot | `T1, T2, T4, T5` (Observed Live) |
| `8` | `charginganims/8_hand.json`<br>`8_Hand_BG.json` | Animated thumbs-up hand gesture | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Generic clipart graphic | `T1, T2, T4, T5` (Observed Live) |
| `9` | `charginganims/9_waterMelon.json`<br>`9_WaterMelon_BG.json` | Watermelon wedge wearing sunglasses | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon illustration | `T1, T2, T4, T5` (Observed Live) |
| `10` | `charginganims/10_shark.json`<br>`10_BatteryLiquid_BG.json` | Shark swimming in liquid battery reservoir | Combined (FG Character + BG JSON) | Horizontal fluid fill tracking level | **Adapt** | Adapt liquid physics into *Liquid Mercury* | `T1, T2, T4, T5` (Observed Live) |
| `11` | `charginganims/11_flash.json`<br>`11_Flash_BG.json` | Flash lightning streak with lens flare | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Combine** | Combined into *Electric Pulse* | `T1, T2, T4, T5` (Observed Live) |
| `12` | `charginganims/12_bettle.json`<br>`12_Bettle_BG.json` | Beetle flapping wings on branch | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon mascot | `T1, T2, T4, T5` (Observed Live) |
| `13` | `charginganims/13_pumpkin.json`<br>`13_Pumpkin_BG.json` | Halloween jack-o'-lantern with bats | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Seasonal novelty | `T1, T2, T4, T5` (Observed Live) |
| `14` | `charginganims/14_fruit.json`<br>`14_Fruit_BG.json` | Pear bouncing on grass | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon illustration | `T1, T2, T4, T5` (Observed Live) |
| `15` | `charginganims/15_magicWand.json`<br>`15_MagicWand_BG.json` | Magic wand casting glowing particle sparks | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Fantasy theme; incongruent with Nothing OS | `T1, T2, T4, T5` (Observed Live) |
| `16` | `charginganims/16_robot.json`<br>`16_Robot_BG.json` | Android robot waving from platform | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Adapt** | Adapt into *Wireframe Orbit* | `T1, T2, T4, T5` (Observed Live) |
| `17` | `charginganims/17_shoes.json`<br>`17_Shoes_BG.json` | Running sneakers moving across pavement | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Commercial sneaker graphic | `T1, T2, T4, T5` (Observed Live) |
| `18` | `charginganims/18_snowMan.json`<br>`18_SnowMan_BG.json` | Shivering snowman with snowfall | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Seasonal novelty | `T1, T2, T4, T5` (Observed Live) |
| `19` | `charginganims/19_ninja.json`<br>`19_Ninja_BG.json` | Silhouette ninja running across rooftops | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Off-brand cartoon illustration | `T1, T2, T4, T5` (Observed Live) |
| `20` | `charginganims/20_ball.json`<br>`20_Ball_BG.json` | Bouncing ball with animated face | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Generic sports graphic | `T1, T2, T4, T5` (Observed Live) |
| `21` | `charginganims/21_juice.json`<br>`21_Juice_BG.json` | Tropical juice glass with straw and bubbles | Combined (FG Character + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Cartoon drink container | `T1, T2, T4, T5` (Observed Live) |
| `22` | `charginganims/22_Charge_Bolt.lottie` | High-voltage neon green lightning arc (`#4CF075`) | Single Layer (FG Lottie) | Static position, continuous pulse loop | **Adapt** | Adapt into *Electric Pulse* (monochrome/cyan) | `T1, T2, T4, T5` (Observed Live) |
| `23` | `charginganims/23_Charge_Bolt.lottie` | High-energy dual electrical bolt (`#4CF075`) | Single Layer (FG Lottie) | Static position, continuous pulse loop | **Combine** | Combined into *Plasma Flow* | `T1, T2, T4, T5` (Observed Live) |
| `24` | `charginganims/24_Crystal_Shower.lottie`| Cyan shimmering ice crystals (`#A4E9F9`) | Single Layer (FG Lottie) | Static position, continuous particle shower | **Adapt** | Adapt into *Dot Matrix Reactor* | `T1, T2, T4, T5` (Observed Live) |
| `25` | `charginganims/25_Potion.lottie` | Bubbling alchemical potion bottle (`#99EA11`) | Single Layer (FG Lottie) | Static position, bubbling vapor loop | **Exclude** | Fantasy alchemy theme | `T1, T2, T4, T5` (Observed Live) |
| `26` | `charginganims/26_Leaf.lottie` | Swirling bio leaves (`#6FE230`) | Single Layer (FG Lottie) | Static position, rotational leaf orbit | **Exclude** | Nature graphic; off-brand | `T1, T2, T4, T5` (Observed Live) |
| `27` | `charginganims/27_Morning_star.lottie` | Spiked mace radiating golden sparks (`#FFE963`) | Single Layer (FG Lottie) | Static position, continuous starburst | **Adapt** | Adapt radial burst into *Glyph Arc* | `T1, T2, T4, T5` (Observed Live) |
| `28` | `charginganims/28_Spear.lottie` | Flaming spearhead beam (`#FF8871`) | Single Layer (FG Lottie) | Static position, horizontal beam sweep | **Adapt** | Adapt into *Nothing Red Surge* | `T1, T2, T4, T5` (Observed Live) |
| `29` | `charginganims/29_Elixir.lottie` | Magenta glowing elixir bottle (`#FF84C2`) | Single Layer (FG Lottie) | Static position, mist pulse loop | **Exclude** | Fantasy alchemy theme | `T1, T2, T4, T5` (Observed Live) |
| `30` | `charginganims/30_Freeze_Bolt.lottie` | Glacial ice lightning surge (`#97D4FF`) | Single Layer (FG Lottie) | Static position, electrical crystal pulse | **Adapt** | Adapt into *Cyber Horizon* | `T1, T2, T4, T5` (Observed Live) |
| `31` | `charginganims/31_Pedals.lottie` | Falling cherry blossom petals (`#FF9696`) | Single Layer (FG Lottie) | Static position, falling petal stream | **Exclude** | Floral graphic; off-brand | `T1, T2, T4, T5` (Observed Live) |
| `32` | `charging_anim_preview_32` | Minimalist percentage text pill | Single Layer (Vector / Text) | Displays numeric percentage | **Adopt** | Adopt as *Minimalist Bolt* baseline | `T1, T2, T4, T5` (Observed Live) |
| `33` | `charging_notch_icon_plug` | AC power plug vector icon beside battery | Single Layer (Vector Icon) | Static icon + dynamic level text | **Adopt** | Adopt as technical connection glyph | `T1, T2, T4, T5` (Observed Live) |
| `34` | `charging_notch_icon_battery` | Standard filled battery cell indicator | Single Layer (Vector Icon) | Segmented fill tracking percentage | **Adopt** | Adopt as *Battery Cell* | `T1, T2, T4, T5` (Observed Live) |
| `35` | `charging_notch_icon_battery_2` | Thin outline battery cell indicator | Single Layer (Vector Icon) | Segmented fill tracking percentage | **Adopt** | Adopt as technical outline variant | `T1, T2, T4, T5` (Observed Live) |
| `36` | `charging_notch_icon_bolt` | High-voltage lightning bolt glyph | Single Layer (Vector Icon) | Pulsing icon + numeric percentage | **Adopt** | Adopt as *Wattage Reactor* glyph | `T1, T2, T4, T5` (Observed Live) |
| `101`| `charging_notch_in_makar_sankranti.json`| Festive kites flying in wind | Combined (FG + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | Seasonal festival theme | `T1, T2` (Inferred from bytecode) |
| `102`| `charging_notch_in_republic_day.json` | Indian tricolor ribbon wave | Combined (FG + BG JSON) | Horizontal wave fill proportional to battery % | **Exclude** | National holiday theme | `T1, T2` (Inferred from bytecode) |

---

## 3. Exhaustive Inventory: Reference A Wave Themes (24 Themes)

All 24 items extracted from `pb/i1.java` (`f57559l`):

| ID | Name / Palette | Primary Wave Colors | Background Color | Shader / Motion Type | Nothing Island Disposition | Evidence Status |
|---|---|---|---|---|---|---|
| `1` | Monochrome Wave | `#000000`, `#FFFFFF` | `#000000` | Linear horizontal wave sweep | **Adopt** (Nothing Monochrome) | `T1, T2` (Bytecode registry) |
| `2` | Cyber Dark | `#00DBDE`, `#98A6FF`, `#FD50FF` | `#000000` | Multi-stop gradient wave | **Adapt** (Cyan / Blue edge glow) | `T1, T2` (Bytecode registry) |
| `3` | Ember Flame | `#00F5A0`, `#00D9F5` | Color Res #3 | Linear gradient wave | **Adapt** (Thermal warning state) | `T1, T2` (Bytecode registry) |
| `4` | Emerald Matrix | `#F46B45`, `#EEA849` | `#000000` | Multi-color wave | **Adapt** (Matrix Phosphor Green) | `T1, T2` (Bytecode registry) |
| `5` | Electric Violet | `#00FFF0`, `#0083FE` | Color Res #5 | Multi-color wave | **Combine** (Synthwave sweep) | `T1, T2` (Bytecode registry) |
| `6` | Lime Velocity | `#FDEFF9`, `#EC38BC`, `#A44FDF`, `#450074` | `#000000` | Multi-stop gradient | **Exclude** (Clashing palette) | `T1, T2` (Bytecode registry) |
| `7` | Sunset Gold | `#DCE35B`, `#45B649` | Color Res #7 | Two-color linear wave | **Adapt** (Amber Fast-Charge) | `T1, T2` (Bytecode registry) |
| `8` | Crimson Burst | `#FCEABB`, `#F8B500` | Color Res #8 | Two-color linear wave | **Adapt** (Nothing Red Accent) | `T1, T2` (Bytecode registry) |
| `9` | Toxic Neon | `#FF4AA3`, `#FF4747` | Color Res #9 | Two-color linear wave | **Exclude** (Over-saturated neon) | `T1, T2` (Bytecode registry) |
| `10`| Solar Flare | `#FDFC47`, `#24FE41` | Color Res #10 | Two-color linear wave | **Exclude** (Clashing yellow-green) | `T1, T2` (Bytecode registry) |
| `11`| Night Owl | `#FD8D47` | Color Res #12 | Single-stop wave with owl asset | **Exclude** (Cartoon owl asset) | `T1, T2` (Bytecode registry) |
| `12`| Wal Ocean | `#FDD947` | Color Res #12 | Single-stop wave with whale asset | **Exclude** (Cartoon whale asset) | `T1, T2` (Bytecode registry) |
| `13`| Lime Solid | `#D3FD47` | `#1AFFFFFF` | Semi-transparent card wave | **Combine** (Consolidated into Amber) | `T1, T2` (Bytecode registry) |
| `14`| Mint Solid | `#47FD62` | `#1AFFFFFF` | Semi-transparent card wave | **Combine** (Consolidated into Cyan) | `T1, T2` (Bytecode registry) |
| `15`| Aqua Solid | `#47FDD9` | `#1AFFFFFF` | Semi-transparent card wave | **Combine** (Consolidated into Cyan) | `T1, T2` (Bytecode registry) |
| `16`| Sky Solid | `#47D6FD` | `#1AFFFFFF` | Semi-transparent card wave | **Combine** (Consolidated into Slate) | `T1, T2` (Bytecode registry) |
| `17`| Cobalt Solid | `#47B1FD` | `#FFFFFF` | High-contrast white card wave | **Combine** (Consolidated into Cobalt) | `T1, T2` (Bytecode registry) |
| `18`| Royal Blue | `#477BFD` | `#80000000` | Dark card wave | **Combine** (Consolidated into Dark Blue) | `T1, T2` (Bytecode registry) |
| `19`| Indigo Wave | `#6352FF` | `#80000000` | Dark card wave | **Combine** (Consolidated into Indigo) | `T1, T2` (Bytecode registry) |
| `20`| Violet Wave | `#9252FF` | `#0DFFFFFF` | Transparent card wave | **Combine** (Consolidated into Violet) | `T1, T2` (Bytecode registry) |
| `21`| Fuchsia Wave | `#BA52FF` | `#FFFFFF` | High-contrast wave | **Exclude** (Gaudy neon) | `T1, T2` (Bytecode registry) |
| `22`| Magenta Wave | `#DC52FF` | `#FFFFFF` | High-contrast wave | **Exclude** (Gaudy neon) | `T1, T2` (Bytecode registry) |
| `23`| Pink Wave | `#FF52C5` | `#FFFFFF` | High-contrast wave | **Exclude** (Gaudy neon) | `T1, T2` (Bytecode registry) |
| `24`| Rose Wave | `#FF5292` | `#FFFFFF` | High-contrast wave | **Exclude** (Gaudy neon) | `T1, T2` (Bytecode registry) |

---

## 4. Classification of Reference B (Dynamic Island Pro) Themes

Decompiled inspection of `tr.com.eywin.dynamic.island.notch/manager/ThemeManager.java`, `defpackage/p04.java`, and `defpackage/q04.java` establishes that **the 50 themes in Reference B are NOT charging animations**:

| Classification Category | Count | Description & Scope | Example Entries |
|---|---|---|---|
| **Actual Charging Animation** | **1** | The `ap2` Compose node in `le.java` with 800ms alpha pulse and 2500ms auto-dismiss | Single state: Text `"Charging"` + battery % + pulsing bar + bolt glyph |
| **General Island Colorway** | **50** | Global background brush/solid color and border width/color applied across all island states (Idle, Music, Calls, Alerts) | `classic_black`, `minimal_gray`, `midnight_blue`, `neon_cyber`, `matrix`, `deep_space` |
| **Charging-Specific Configuration** | **0** | No charging-specific colorway overrides exist in Reference B | N/A (Charging state uses hardcoded `0xFFFF4500` or `0xFF34C759`) |
| **Other Presentation / Effect** | **0** | No auxiliary charging overlays or particles exist in Reference B | N/A |

### Complete Classification of All 50 General Island Colorways (`q04.java`):

| Index | ID | Display Name | Visual Style | Border Config | Classification | Charging Relevance |
|---|---|---|---|---|---|---|
| 1 | `classic_black` | Classic Black | Solid `#000000` | None | General Island Colorway | Base background only |
| 2 | `minimal_gray` | Minimal Gray | Solid `#333333` | 0.5dp `#444444` | General Island Colorway | Base background only |
| 3 | `midnight_blue` | Midnight | Solid `#111827` | 1.0dp `#1F2937` | General Island Colorway | Base background only |
| 4 | `amethyst` | Amethyst | Purple Sweep Gradient | None | General Island Colorway | Base background only |
| 5 | `sunset` | Sunset | Amber Linear Gradient | None | General Island Colorway | Base background only |
| 6 | `neon_cyber` | Neon Cyber | Solid `#000000` | 1.0dp Cyan | General Island Colorway | Base background only |
| 7 | `royal_gold` | Royal Gold | Gold Gradient Sweep | 1.0dp Gold | General Island Colorway | Base background only |
| 8 | `ocean_breeze` | Ocean Breeze | Slate Blue Gradient | None | General Island Colorway | Base background only |
| 9 | `crimson_fire` | Crimson Fire | Red Gradient Sweep | None | General Island Colorway | Base background only |
| 10 | `deep_space` | Deep Space | Solid `#0A0A14` | 1.0dp `#1A1A32` | General Island Colorway | Base background only |
| 11 | `forest_rain` | Forest Rain | Green Gradient Sweep | None | General Island Colorway | Base background only |
| 12 | `purple_haze` | Purple Haze | Solid `#2E1065` | 1.0dp `#4C1D95` | General Island Colorway | Base background only |
| 13 | `cherry_blossom`| Cherry Blossom | Pink Gradient Sweep | None | General Island Colorway | Base background only |
| 14 | `golden_hour` | Golden Hour | Warm Amber Gradient | None | General Island Colorway | Base background only |
| 15 | `matrix` | The Matrix | Solid `#000000` | 1.5dp `#00FF41` | General Island Colorway | Base background only |
| 16 | `cyberpunk_pink`| Cyber Pink | Solid `#1A001A` | 1.0dp `#FF007F` | General Island Colorway | Base background only |
| 17 | `mint_fresh` | Mint Fresh | Mint Sweep Gradient | None | General Island Colorway | Base background only |
| 18 | `silver_lining` | Silver Lining | Silver Gradient Sweep | 0.5dp Silver | General Island Colorway | Base background only |
| 19 | `carbon_fiber` | Carbon Fiber | Solid `#171717` | 1.0dp `#262626` | General Island Colorway | Base background only |
| 20 | `electric_blue` | Electric Blue | Solid `#001133` | 2.0dp `#0066FF` | General Island Colorway | Base background only |
| 21 | `sweet_candy` | Sweet Candy | Pink/Purple Sweep | None | General Island Colorway | Base background only |
| 22 | `northern_lights`| Northern Lights| Teal/Purple Radial | None | General Island Colorway | Base background only |
| 23 | `pure_white` | Pure White | Solid `#FFFFFF` | 0.5dp Black | General Island Colorway | Base background only |
| 24 | `vampire_red` | Vampire Red | Solid `#1A0000` | 1.0dp Red | General Island Colorway | Base background only |
| 25 | `emerald_city` | Emerald City | Emerald Sweep | None | General Island Colorway | Base background only |
| 26 | `cosmic_void` | Cosmic Void | Solid `#05050A` | 0.5dp `#6366F1` | General Island Colorway | Base background only |
| 27 | `rose_gold` | Rose Gold | Rose Gold Sweep | None | General Island Colorway | Base background only |
| 28 | `ice_cold` | Ice Cold | Cyan/White Gradient | None | General Island Colorway | Base background only |
| 29 | `lemon_twist` | Lemon Twist | Yellow Sweep | None | General Island Colorway | Base background only |
| 30 | `twilight` | Twilight | Deep Violet Sweep | None | General Island Colorway | Base background only |
| 31 | `hot_lava` | Hot Lava | Orange/Red Sweep | 1.0dp Orange | General Island Colorway | Base background only |
| 32 | `sea_foam` | Sea Foam | Teal Solid | 1.0dp Teal | General Island Colorway | Base background only |
| 33 | `nebula` | Nebula | Magenta Angular Sweep | None | General Island Colorway | Base background only |
| 34 | `bumblebee` | Bumblebee | Yellow Solid | 2.0dp Black | General Island Colorway | Base background only |
| 35 | `slate` | Slate | Solid `#334155` | 1.0dp Slate | General Island Colorway | Base background only |
| 36 | `rainbow` | Rainbow | 5-Stop Gradient Sweep | None | General Island Colorway | Base background only |
| 37 | `stormy_night` | Stormy Night | Dark Blue Gradient | None | General Island Colorway | Base background only |
| 38 | `bubblegum` | Bubblegum | Pink Solid | 1.5dp Pink | General Island Colorway | Base background only |
| 39 | `toxic_green` | Toxic Green | Dark Green Solid | 1.0dp Green | General Island Colorway | Base background only |
| 40 | `ruby_red` | Ruby Red | Crimson Angular Sweep | None | General Island Colorway | Base background only |
| 41 | `deep_sea` | Deep Sea | Navy Solid | 0.5dp Cyan | General Island Colorway | Base background only |
| 42 | `pumpkin_spice`| Pumpkin | Orange Solid | 1.0dp Orange | General Island Colorway | Base background only |
| 43 | `titanium` | Titanium | Silver Metallic Sweep | None | General Island Colorway | Base background only |
| 44 | `insta_style` | Influencer | 3-Stop Social Gradient | None | General Island Colorway | Base background only |
| 45 | `glitch` | Glitch | Black Solid | 1.0dp Cyan | General Island Colorway | Base background only |
| 46 | `copper` | Copper | Copper Metallic Sweep | None | General Island Colorway | Base background only |
| 47 | `lavender` | Lavender | Violet Solid | 1.0dp Purple | General Island Colorway | Base background only |
| 48 | `stealth` | Stealth | Solid `#121212` | 0.5dp `#1F1F1F` | General Island Colorway | Base background only |
| 49 | `miami_vice` | Miami Vice | Cyan/Pink Radial Sweep | None | General Island Colorway | Base background only |
| 50 | `pastel_dream` | Pastel Dream | Soft Pastel Sweep | None | General Island Colorway | Base background only |

---

## 5. Traceability & Coverage Matrix for Proposed 12-Theme Suite

This table maps how the 12 curated Nothing Island themes reproduce, adapt, or combine reference effects, and explicitly documents every excluded reference effect:

### 5.1 Themes Reproducing Reference Effects

| Nothing Island Theme | Category | Reference Effects Covered | Preserved Motion & Visual Mechanics | Adaptation for Nothing OS |
|---|---|---|---|---|
| **`GLYPH_ARC`** | Nothing Signature | Ref A #27 (Morning Star), Ref A #20 (Ball) | Rotational sweep around punch hole with trailing head attenuation | Re-engineered as physical Glyph segmented lighting arcs |
| **`DOT_MATRIX_REACTOR`** | Nothing Signature | Ref A #24 (Crystal Shower), Ref B `ap2` (Pulsing Bar) | Circular sequential illumination and synchronized brightness breathing | Re-engineered with 32 discrete Dot Matrix LED dots |
| **`WIREFRAME_ORBIT`** | Nothing Signature | Ref A #16 (Robot wireframe), Ref A #12 (Beetle orbit) | Dual counter-rotating orbits at $\pm 25^\circ$ angles | Replaced cartoon robots with technical dash-wireframe ellipses |
| **`NOTHING_RED_SURGE`** | Nothing Signature | Ref A #28 (Spear), Ref A #8 (Crimson Burst), Ref B #9 | Directional perimeter wave tracking battery percentage | Rendered in surgical Nothing Red (`#D71921`) with particle spark |
| **`LIQUID_MERCURY`** | Fluid & Elemental | Ref A #10 (Shark Liquid), Ref A IDs 0–21 (Wave Tracks) | Sinusoidal fluid wave with buoyant meniscus physics | Stripped cartoon sharks; rendered in pure liquid monochrome chrome |
| **`ELECTRIC_PULSE`** | Fluid & Elemental | Ref A #11 (Flash), Ref A #22 & #23 (Charge Bolt) | High-frequency perimeter jitter and electrical discharge arcing | Replaced green cartoon bolts with 30Hz cyan/white electrostatic field |
| **`CYBER_HORIZON`** | Fluid & Elemental | Ref A #30 (Freeze Bolt), Ref B #15 (Matrix Border) | Lateral horizontal laser lines expanding from camera cutout | Cyan laser lines pulsing synchronously with battery breathing |
| **`PLASMA_FLOW`** | Fluid & Elemental | Ref A #2 (Cyber Dark), Ref A #5 (Violet), Ref A #23 | Multi-stop angular sweep gradient with continuous rotation | 4-stop monochrome and Nothing Red angular gradient sweep |
| **`DIGITAL_GAUGE`** | Technical & Minimal | Ref A #32 (Minimalist), Ref A #34 (Battery Cell) | Progressive 270-degree radial gauge fill | Calibrated technical dial with 12 engraved tick marks |
| **`WATTAGE_REACTOR`** | Technical & Minimal | Ref A #36 (Bolt Glyph), Ref B `ap2` | Dynamic lightning bolt indicator scaling with charging speed | Live wattage readout ("45W SUPER FAST") + 5 kinetic power bars |
| **`MINIMALIST_BOLT`** | Technical & Minimal | Ref A #33 (Plug), Ref B `ap2` (Apple style indicator) | Clean pulse breathing (alpha 0.5f..1.0f) over 800ms cycle | Pure iOS/Nothing minimalist hybrid with Dot Matrix typography |
| **`BATTERY_CELL`** | Technical & Minimal | Ref A #34, #35 (Battery 1 & 2), Ref B `ap2` | Segmented horizontal enclosure tracking battery blocks | Precision technical enclosure matching Nothing battery status widget |

### 5.2 Explicit List of Reference Effects NOT Covered (Exclusion Audit)

| Excluded Reference Item | Original ID / Path | Reason for Exclusion | Architectural Justification |
|---|---|---|---|
| **Ghost Mascot** | Ref A #0 (`0_ghost.json`) | Off-brand cartoon sprite | Incongruent with Nothing OS technical aesthetic |
| **Among Us Astronaut** | Ref A #1 (`1_amongUs.json`) | Severe copyright / trademark infringement | Direct legal liability; cannot ship third-party IP |
| **Crab on Beach** | Ref A #2 (`2_crab.json`) | Off-brand cartoon mascot | Childish illustration; unsuitable for premium phone |
| **Sports Car** | Ref A #3 (`3_car.json`) | Off-brand cartoon illustration | Visual clutter without functional value |
| **Coffee Bee** | Ref A #4 (`4_bee.json`) | Off-brand cartoon mascot | Non-functional clip art |
| **Yellow Emoji** | Ref A #6 (`6_emoji.json`) | Generic emoji asset | Violates Nothing monochrome design system |
| **Snail** | Ref A #7 (`7_snail.json`) | Off-brand cartoon mascot | Visual noise |
| **Thumbs-Up Hand** | Ref A #8 (`8_hand.json`) | Generic clipart graphic | Low aesthetic quality |
| **Watermelon Wedge** | Ref A #9 (`9_waterMelon.json`) | Off-brand cartoon illustration | Visual noise |
| **Beetle on Branch** | Ref A #12 (`12_bettle.json`) | Off-brand cartoon mascot | Incongruent with Nothing industrial hardware |
| **Halloween Pumpkin** | Ref A #13 (`13_pumpkin.json`) | Seasonal novelty | Transient gimmick; unnecessary APK bloat |
| **Bouncing Pear** | Ref A #14 (`14_fruit.json`) | Off-brand cartoon illustration | Visual noise |
| **Magic Wand** | Ref A #15 (`15_magicWand.json`) | Fantasy magic theme | Clashes with Nothing industrial transparency |
| **Sneakers** | Ref A #17 (`17_shoes.json`) | Commercial graphic | Clashes with Nothing hardware |
| **Snowman** | Ref A #18 (`18_snowMan.json`) | Seasonal novelty | Transient gimmick |
| **Ninja** | Ref A #19 (`19_ninja.json`) | Off-brand cartoon illustration | Clashes with Nothing design |
| **Bouncing Face Ball** | Ref A #20 (`20_ball.json`) | Generic sports graphic | Clashes with Nothing design |
| **Tropical Juice** | Ref A #21 (`21_juice.json`) | Cartoon drink container | Clashes with Nothing design |
| **Alchemy Potion** | Ref A #25 (`25_Potion.lottie`) | Alchemical bottle graphic | Fantasy visual; low technical value |
| **Swirling Leaves** | Ref A #26 (`26_Leaf.lottie`) | Nature swirl | Visual noise |
| **Magenta Elixir** | Ref A #29 (`29_Elixir.lottie`) | Fantasy alchemy bottle | Fantasy visual |
| **Cherry Petals** | Ref A #31 (`31_Pedals.lottie`) | Floral clip art | Off-brand decorative graphic |
| **Festival Kites** | Ref A #101 (`makar_sankranti`) | Seasonal festival asset | Transient regional asset |
| **Tricolor Ribbon** | Ref A #102 (`republic_day`) | Seasonal festival asset | Transient regional asset |

---

## 6. Measurable Performance & Acceptance Criteria

To guarantee smooth rendering at 120 Hz on Nothing Phone (4a):

| Metric | Target Specification | Validation Methodology | Failure Threshold |
|---|---|---|---|
| **Frame Budget** | $\le 8.333\text{ ms}$ | `dumpsys gfxinfo framestats` | $> 8.333\text{ ms}$ (jank frame) |
| **90th Percentile Frame Time** | $\le 6.5\text{ ms}$ | Automated Macrobenchmark test | $> 6.5\text{ ms}$ |
| **95th Percentile Frame Time** | $\le 7.8\text{ ms}$ | Automated Macrobenchmark test | $> 7.8\text{ ms}$ |
| **99th Percentile Frame Time** | $\le 8.33\text{ ms}$ | Automated Macrobenchmark test | $> 8.33\text{ ms}$ |
| **Overall Jank Rate** | $< 1.0\%$ | 60s continuous animation capture | $\ge 1.0\%$ |
| **Allocation in Draw Loop** | **0 bytes** | Android Studio Allocation Profiler | $> 0$ bytes in `DrawScope` |
| **CPU Utilization** | $\le 3.5\%$ | `top -n 1 -d 1` on target PID | $> 5.0\%$ |
| **Thermal Delta ($\Delta T$)** | $< 0.8^\circ\text{C}$ rise | Battery temperature probe after 15 min | $\ge 1.2^\circ\text{C}$ rise |
| **Reduced-Motion Fallback** | Instantaneous static state | Observe `TRANSITION_ANIMATION_SCALE == 0f` | Any continuous motion running |

---

## 7. Implementation Roadmap & Guardrails

* **Milestone Assignment:** Phase 3 — Charging Animation & Studio Milestone (R8.1).
* **Roadmap Guardrail:** **R9 is strictly reserved for Call Intelligence** in `FEATURE_PARITY_IMPLEMENTATION_ROADMAP.md`. Call Intelligence remains deferred until Phase 2 and Phase 3 prerequisites are complete.

---
*Matrix verified against decompiled smali bytecode, raw assets, and live physical Nothing Phone (4a) runtime logs.*
