# Game Design — Boldoo: Guardians of the Gobi

## Player promise

The player becomes a young Guardian of the Gobi. Every stage combines readable platforming with a concrete act of environmental restoration: return water, repair a migration route, and clear pollution from the sky.

## Core loop

1. Read the stage objective and move through the landscape.
2. Collect restoration items and avoid visible hazards.
3. Use a newly introduced Guardian ability to solve the stage's harder section.
4. Activate checkpoints, reach the goal and earn one to three stars.
5. Improve collection, survival and completion time on a replay.

## Campaign progression

### Level 1 — Oasis Rescue

The opening level teaches movement, variable jumping and enemy stomps on a forgiving continuous route. The oasis opens only after all 10 water drops are collected. One midpoint checkpoint limits repetition after mistakes.

### Level 2 — Migration Path

Short gaps and moving platforms introduce timing without demanding pixel-perfect jumps. Wind areas visibly stream in their push direction. The goal requires eight trail markers; trash and water provide optional score and stars. Hulan Dash offers a fast horizontal burst with a cooldown. Two checkpoints divide the route into manageable sections.

### Level 3 — Storm Over Gobi

Sand, wind and falling-rock zones build pressure while remaining telegraphed. Takhi Shield absorbs one hit and refreshes at checkpoints. Khavtgai Ground Pound breaks three pollution barriers. The Great Smog has three health phases; each lost health point increases its movement and arena rock frequency. Defeating it clears the goal and reveals the restored sky ending.

## Movement rules

- Horizontal movement accelerates to a capped run speed and decelerates predictably.
- Coyote time lasts 130 ms after leaving a platform.
- Jump input is buffered for 140 ms before landing.
- Releasing jump early reduces upward velocity for a shorter arc.
- A downward stomp bounces Boldoo away from enemies.
- Player damage grants temporary invincibility; shielded damage does not remove a life.
- Falling respawns at the latest checkpoint and removes one life.

The simulation runs in fixed 1/60-second steps. Rendering can vary without changing movement distance, collision or timers.

## Scoring and stars

- Water drop: 100
- Trail marker: 140
- Trash: 90
- Smogling stomp: 250
- Checkpoint: 150
- Ability pickup: 200
- Pollution barrier: 300
- Great Smog hit: 400, plus 1400 defeat bonus
- Completion: 900 plus remaining-life and par-time bonuses

One star requires a meaningful portion of the stage collectibles. Two stars require at least 90% and survival. Three stars require every collectible, at least two lives, and a completion time at or below par.

## Accessibility and mobile ergonomics

- Controls occupy the lower corners and support simultaneous movement plus jump/ability.
- Left-handed mode swaps movement and action clusters.
- Control opacity cycles through 42%, 62%, 82% and 100%.
- Sound effects, ambient audio and haptics can be toggled independently.
- Objectives and blocked finishes explain what is still missing in text.
- Collectibles use distinct silhouettes and colors; gameplay does not rely on emoji.

## Technical boundaries

- Native Android Java and Canvas only.
- No network permission; progress is stored locally with `SharedPreferences`.
- Level geometry and object placement live in validated JSON assets.
- Original project imagery and generated procedural audio only; no Mario/Nintendo assets or designs.
