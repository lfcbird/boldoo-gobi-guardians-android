# Changelog

## 0.2.0

### Campaign

- Expanded the MVP from one short stage to three data-driven levels.
- Added Oasis Rescue tutorial, Migration Path platform challenge and Storm Over Gobi finale.
- Added the three-phase Great Smog boss and restored-sky ending.

### Gameplay

- Replaced frame-dependent movement with a fixed 60 Hz simulation.
- Added acceleration, deceleration, coyote time, jump buffering and variable-height jumps.
- Added stomp bounce, damage invincibility, fair checkpoint respawns and smooth camera look-ahead.
- Added moving platforms, jumpable gaps, wind zones, spikes, falling rocks and breakable pollution barriers.
- Added Hulan Dash, Takhi Shield and Khavtgai Ground Pound abilities.
- Added water drops, trail markers and trash cleanup objectives.

### Experience

- Added splash, main menu, level select, pause, settings, results, game-over, final victory and credits screens.
- Added saved progression, best scores/times, three-star ratings and total water.
- Added multi-touch controls, an ability button, left-handed layout and adjustable control opacity.
- Replaced emoji HUD symbols with Canvas-drawn icons.
- Added particles, camera shake, animated character transforms, boss health and environment effects.
- Added reproducible original sound effects and ambient wind.

### Engineering

- Split the original single view into engine, player, renderer, level loader, audio, input, particles and persistence modules.
- Added JSON level validation, pure game-rule unit tests and Android GitHub Actions CI.
- Updated Android version to `versionCode 2` / `versionName 0.2.0`.

## 0.1.0

- Initial playable Android MVP with one level, 10 water drops, three Smoglings, score, lives and touch controls.
