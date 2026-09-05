# Boldoo: Guardians of the Gobi

An original, offline 2D platform adventure for Android. Guide Boldoo across the Gobi, restore an oasis and a migration route, then clear the final sandstorm by defeating the Great Smog.

Version **0.2.0** expands the original MVP into a three-level campaign with responsive platforming, checkpoints, abilities, progression, settings, original sound effects, and a final boss.

## Campaign

| Level | Goal | New challenge |
|---|---|---|
| 1. Oasis Rescue | Collect all 10 water drops | Tutorial route, Smoglings, spikes, checkpoint |
| 2. Migration Path | Restore all 8 trail markers | Gaps, moving platforms, wind, trash, Hulan Dash |
| 3. Storm Over Gobi | Break pollution barriers and defeat the Great Smog | Falling rocks, sandstorm, Takhi Shield, Khavtgai Ground Pound, three-phase boss |

## Highlights

- Native Android Java and Canvas rendering; no WebView or external game engine
- Fixed 60 Hz simulation with acceleration, deceleration and smooth camera look-ahead
- Coyote time, jump buffering, variable-height jump, stomp bounce and damage invincibility
- Multi-touch landscape controls with an ability button and left-handed layout
- Data-driven levels in `app/src/main/assets/levels`
- Splash, main menu, level select, pause, settings, results, game-over, victory and credits screens
- Saved unlocks, completion state, best score, best time, stars, water total and settings
- Procedurally generated, original sound effects and ambient Gobi wind
- Offline operation with no network permission

The game does not contain Nintendo/Mario assets, names, music, characters or level designs.

## Requirements

- Android Studio with Android SDK 35
- JDK 17
- Android 6.0 (API 23) or newer device/emulator
- Landscape display

## Run in Android Studio

1. Clone this repository.
2. Open the repository root in Android Studio.
3. Allow Gradle sync to finish.
4. Choose an Android device or emulator.
5. Press **Run**.

## Build and test

```bash
python3 tools/validate_levels.py
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The Gradle build writes the debug package to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions runs level validation, unit tests, Android lint and the debug build, then uploads the APK as a workflow artifact. APKs, signing keys and build directories are intentionally excluded from source control.

## Controls

- Left/right buttons: move
- Up button: jump; release early for a shorter jump
- Ability button: dash on level 2, or ground pound while airborne on level 3
- Pause button: open pause menu
- Stomp a Smogling from above to defeat it

Takhi Shield absorbs one hit and is recharged at checkpoints. Settings include sound effects, ambient sound, haptics, control opacity and a left-handed layout.

## Project layout

```text
app/src/main/java/.../GameEngine.java   gameplay simulation and rules
app/src/main/java/.../Player.java       movement, abilities and collision
app/src/main/java/.../GameRenderer.java Canvas world and UI rendering
app/src/main/java/.../GameView.java     screen flow and touch routing
app/src/main/assets/levels/             three JSON level definitions
tools/validate_levels.py                level-data validation
tools/generate_sfx.py                   reproducible original audio generation
```

See [GAME_DESIGN.md](GAME_DESIGN.md), [INSTALL-MN.md](INSTALL-MN.md), [CHANGELOG.md](CHANGELOG.md), and [ASSET_CREDITS.md](ASSET_CREDITS.md) for more detail.

## Credits

- Story and original characters: Oyunbold Ganbold
- Development: Bold Technology Solutions
- Android game implementation: OpenAI Codex collaboration
