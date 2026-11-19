# Reference App Screenshot & Video Analysis

**Date:** 2026-11-19
**Source:** User-supplied captures in `reference/ui-screenshots/` (8 PNG, 3 MP4)
plus REA live observation in `REA_LIVE_BEHAVIOR_OBSERVATION.md`.

These notes record **observed behavior only**. No proprietary code, assets,
sprites or Lottie files are copied into Nothing Island. All features described
here are to be reimplemented originally in Compose.

## 1. Dynamic Island Pro (Reference B)

### Feature menu (`ref B features.png`)
Eight feature entries:
1. **Notifications** — Notification Settings
2. **Music** — Music Control
3. **Calls** — Call Notification
4. **Connection** — Headset Connection
5. **Ringing Mode** — Ring Mode Notification
6. **Silent** — Silent Mode Notification
7. **Face ID** — Face Unlock Popup
8. **Device Unlock** — Unlock Notification

Mapping to our app: 1–3 and 5–8 are implemented (R6 notifications, R7 media,
R9 calls, Sprint 2 ringer/device-unlock). **Headset Connection with battery
ring is still missing** — our Bluetooth work is only a settings stub.

### Home screen (`ref B home view.png`)
- Header "Dynamic Island PRO" + UPGRADE button
- "Setup Required" banner
- **Mascot** card — "Add to Notch", preview shows an orange pixel cat inside the pill
- **Assistive** card — "Quick tools": flashlight, QR code, music, call, settings icons; toggle OFF
- **ASK AI** card (NEW badge) — "Tap notch to ask something to the AI" with floating robot button
- Bottom tabs: Home / charging / palette (styles) / settings

### Island styles (`ref B island styles.png`)
~24 pill themes: Classic Black, Minimal Gray, Midnight, Ametyst (crown/premium),
Sunset, Neon Cyber, Royal Gold, Ocean Breeze, Crimson Fire, Deep Space,
Forest Rain, Purple Haze, Cherry Blossom, Golden Hour, The Matrix, Cyber Pink,
Mint Fresh, Silver Lining, Carbon Fiber, Electric Blue, Sweet Candy, Northern
Lights, Pure White, Vampire Red, Emerald City, Cosmic Void. Some are crown-gated.

Our `ThemeStudioScreen` + `NothingPaletteChips` cover the concept; premium
gating is irrelevant for our open-source build.

### Mascots (`ref B mascots.png`)
"Enable Pixel Mascot" master toggle plus ~40 pixel mascots: Cat, Dog, Ghost,
Slime, UFO, Heart, Music, Race Car, Fighter Jet, Epic Sword, Fire, Skull,
Potion, Rocket, Robot, Ninja, Fox, Owl, Wizard, Zombie, Snail, Cloud, Diamond,
Gamer, Rainbow Cat, Watcher, Runner, Code Rain, Pacman, Crewmate, Plumber,
Pickaxe, Pokeball, Burger, Invader, Sun, Coffee, Bat. Several crown-gated.

**User-provided behavior note:** these mascots **walk forward and backward
inside the pill**, and **jump when tapped**. This is a concrete animation spec
for R13 (Pixel Mascot Subsystem): a sprite moving left↔right along the compact
pill, with a tap-triggered jump animation.

## 2. Dynamic Notch (Reference A)

### Full feature list (`refA Home view.png`)
Beyond what we already knew:
- Display, Hide System (Double) Notification ✅ (now implemented), Quick Controls
- Notification, Call, Weather Prediction
- **Live Football Score, Live Cricket Score, Live Baseball Score, Live Basketball Score, Live Hockey Score**
- Bluetooth Connection, Music Player
- Flashy Border Animation, **Live Order Tracking**, System Live Alerts
- Single/Long Press Gesture, Hide Notch In Apps, Help, Check For Update, What's New
- **Charging Animation** gallery (large themed scenes)
- **Small Notch Animations** (compact pill themes)

### Charging animations (`refA charging animations*.png`)
Two distinct families:
1. **Large scene themes** — colored scene fills the left portion of the pill up
   to the battery percentage; a character sits at the fill boundary (dragonfly,
   pumpkin, pear, fairy, robot, shoes, snowman, running figure, etc.). Right
   wing shows `50%` in white on black.
2. **Compact themes** — minimal styles: lightning icon + colored percentage
   bubble (green/blue/yellow/pink), "Charging 50%" with a green battery bar,
   and small emoji/kawaii scenes.

Our `ui/charging/*` (17 animations) covers the abstract/kinetic family but
**not** the percentage-fill scene family. That is a visual gap worth an
original implementation (progress-filled pill + moving character at boundary).

### Call video (`ref A call incoming compact view.mp4`, 12 frames, 9 s)
- Incoming call pill: circular caller photo + "Daisy Williams" + mute icon + **red end-call** icon
- When answered: pill turns **green**, shows a **live 00:01 timer**, and the
  end button remains red
- A floating control bar above the pill: **X (close), settings gear, red Stop**
- The pill persists over other apps (shown over Android Settings during onboarding)

**Key learning for our R9:** the live timer starts immediately on answer, and
the answer/decline controls are compact icon buttons inside the pill. Our
current implementation emits `CallConnected` with a static `00:00` and has no
ticking timer — that is the top known flaw to fix.

### Charging video (`ref A charging animations.mp4`, 12 frames, 12 s)
Live theme switching: tapping a charging-theme card instantly previews the
selected theme on the large pill at the top (bubble color changes). Our
`ChargingStyleStudio` should keep that instant-preview behavior.

### Notification demo video (`ref A slide notification.mp4`, 12 frames, 10 s)
This is a **showcase animation on the onboarding screen**, not real notifications:
the empty pill cycles through demo states — timer "90m", WiFi, notification
badge "1", music, incoming call — followed by the X/settings/Stop control bar.
Useful as a reference for our onboarding/preview lab.

## 3. Consolidated feature gap list (observed, not yet in our app)

| Feature | Source | Our status |
|---|---|---|
| Earbud connection pill with battery ring | Ref B | Missing (settings stub only) |
| Pixel mascots that walk in pill + jump on tap | Ref B | Missing (R13 planned) |
| Live call timer ticking after answer | Ref A video | Partial (static 00:00) |
| Percentage-fill charging scene themes | Ref A | Missing (abstract themes only) |
| Live sports scores (5 sports) | Ref A | Missing (P4) |
| Weather prediction | Ref A | Missing |
| Live order tracking | Ref A | Missing |
| Flashy border animation | Ref A | Missing |
| Hide notch in selected apps | Ref A | Missing |
| Assistive quick tools in pill | Ref B | Missing (Peripherals stub) |
| ASK AI in notch | Ref B | Gemini screen exists, not wired to pill |

## 4. Method note (two-way check)
- **REA side:** device list, package versions/permission counts, component names
  from `dumpsys package`, window focus, artifact inventory/hash for Ref A APK.
  Deep method decompilation not done (no JADX provider configured).
- **Visual side:** our own live `screencap` captures + the 11 user-supplied
  screenshots/videos, with frames extracted via ffmpeg for the videos.
