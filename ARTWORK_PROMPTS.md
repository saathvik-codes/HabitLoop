# HabitLoop — Production Artwork Brief

HabitLoop must have its own visual identity: structured, optimistic and progress-oriented. The artwork should express repetition, rhythm, recovery and gradual growth rather than puzzle/game imagery.

## Shared art direction

> Editorial vector illustration built from flowing loops, segmented paths and repeated shapes that communicate routine and progress. Warm off-white background, crisp geometry softened with selective organic curves, subtle paper grain and restrained dimensional shading. Mature, inclusive and calm; no mascot-game styling, no words, no photorealism. Background #FAF8F4 when required. Core palette: sage #84A98C and ink #2F2F2F. Peach #F6B89E is reserved for streaks and recovery. Supporting colors: mist blue #BDE0FE, butter #FFE8A3 and mint #CDECCF. Avoid puzzle symbols, floating blobs, neon colors and thick cartoon outlines.

## Required assets

### 1. Onboarding hero

> A calm looping path moving through four small moments: a check mark, a growing sprout, a gentle flame, and a protected snowflake shield. Balanced centered composition with generous empty space around it.

**Output:** 1200×900 PNG, transparent background.

### 2. Empty-day celebration

> A small friendly flame and sprout character resting together after completing a day, with a restrained peach-and-butter confetti arc. The mood is satisfied and peaceful, not explosive.

**Output:** 900×700 PNG, transparent background.

### 3. No-habits empty state

> An empty rounded planter with one new sage sprout and a small circular sun behind it. Simple centered composition.

**Output:** 800×800 PNG, transparent background.

### 4. Streak-protection header

> A soft translucent snowflake shield floating above a small peach flame, communicating that the streak is protected. Wide banner composition with subjects kept inside the central 70%.

**Output:** 1200×420 PNG, background #FAF8F4.

### 5. Milestone burst

> A restrained radial celebration made from small peach, butter, sage, and lavender rounded shapes. Clear transparent center so a milestone card can sit above it.

**Output:** 1080×1080 PNG, transparent background.

### 6. Mascot states

> A small abstract loop-shaped character made from a sage outer loop and peach inner spark. Produce four consistent poses: welcoming, focused, celebrating, and gently resting.

**Output:** Four separate 800×800 transparent PNGs.

## Template icon requirements

Keep the existing seven concepts—gym, study, coding, reading, meditation, sobriety, and prayer—but redraw them in the shared style. Each icon should use one supporting color plus sage, occupy about 64% of the canvas, and remain readable at 32dp.

**Output:** 256×256 transparent PNG per icon.

## Priority

Generate #1, #2, #3, and #4 first. The current app already contains usable placeholders for the remaining artwork, so these four create the biggest improvement to onboarding, completion, empty-state, and rewards comprehension.

## Profile avatar character set

These replace the temporary mascot crops currently used in Profile customization. Generate all six as one consistent family.

### 7. Focused loop character

> A friendly HabitLoop character built from one soft sage loop shape, focused expression, upright posture, small peach progress spark near the chest. Head-and-shoulders avatar composition.

### 8. Cheerful loop character

> The same HabitLoop loop character with a warm cheerful expression, subtle celebratory peach accent and welcoming posture. Head-and-shoulders avatar composition.

### 9. Calm loop character

> The same HabitLoop loop character with a relaxed, grounded expression, small mist-blue breathing/rhythm accent and calm posture. Head-and-shoulders avatar composition.

### 10. Adventurous loop character

> The same HabitLoop loop character with a confident curious expression, slightly forward posture and a small butter-yellow directional spark. Head-and-shoulders avatar composition.

### 11. Minimal flame character

> A mature friendly flame character representing consistency and recovery, peach outer flame with a butter-yellow center, expressive face, no limbs required. Head-and-shoulders avatar composition.

### 12. Minimal sprout character

> A mature friendly sprout character representing gradual growth, two sage leaves and a subtle face integrated into the stem/leaf form. Head-and-shoulders avatar composition.

**Shared avatar output specification:**

- Six separate 512×512 PNG files.
- Genuine transparent background with alpha.
- Do not render a checkerboard pattern.
- No text, logo, signature, stock mark or watermark.
- Character centered within the inner 72% of the canvas.
- Keep important facial features within the inner 55% so Android circular cropping never removes them.
- Consistent lighting, texture, line weight and scale across all six.
- Readable at 48–64dp.
- Palette: sage #84A98C, dark sage #5F8268, peach #F6B89E, butter #FFE8A3, mist blue #BDE0FE and ink #2F2F2F.
- Avoid childish mascot proportions, complex backgrounds, props extending outside the circular crop-safe area and excessive facial detail.

**Filename mapping:**

1. `avatar_focused_loop.png`
2. `avatar_cheerful_loop.png`
3. `avatar_calm_loop.png`
4. `avatar_adventurous_loop.png`
5. `avatar_flame.png`
6. `avatar_sprout.png`

## Growth Lab visual system

### 13. Growth Lab environment header

> An immersive abstract training space built from concentric sage loops forming a pathway through five skill stations: focus beam, memory tiles, decision fork, reflection pool and cooperative circle. Warm editorial lighting, subtle depth, calm futuristic atmosphere, no game-console styling.

**Output:** `growth_lab_header.png`, 1400×700 PNG with genuine transparency. Keep subjects inside the central 80% and leave negative space at top-left for interface text.

### 14. Skill badge family

> Six consistent mature achievement emblems using HabitLoop loop geometry: Focus, Memory, Impulse Control, Perspective, Reflection and Community. Each badge has one simple central symbol surrounded by a segmented progress loop, with a sage foundation and one restrained accent.

**Output:** Six separate 512×512 transparent PNGs: `badge_focus.png`, `badge_memory.png`, `badge_impulse.png`, `badge_perspective.png`, `badge_reflection.png`, and `badge_community.png`.

### 15. Session completion portal

> A restrained circular portal made from repeating sage and peach loop segments, softly opening outward to communicate a trained skill becoming stronger. Clear transparent center for score and level UI.

**Output:** `growth_session_complete.png`, 1080×1080 transparent PNG.

**Constraints:** No words, scores, anatomical brains, childish trophies, esports imagery, neon cyberpunk, casino visuals, checkerboards, signatures or watermarks. Match the existing HabitLoop palette and paper-grain dimensional finish.

## Production adaptive app icon

The current flame source must be replaced because it contains a baked checkerboard and bottom-right stock mark.

> A distinctive HabitLoop app mark: one continuous sage loop forming a subtle upward sprout at the top and protecting a small peach progress spark in its center. Extremely simple silhouette, confident rounded geometry, mature and optimistic, recognizable at 24px, balanced inside Android adaptive-icon safe zones. This represents repetition, growth and recovery—not only streak fire.

**Required output:**

- `ic_launcher_foreground_v2.png`: 1024×1024 genuine transparent PNG.
- Artwork contained entirely within the central 66% adaptive-icon safe area.
- No background, shadow outside the safe area, words, letters, watermark, signature or checkerboard.
- Also provide `ic_launcher_monochrome.svg` as a single-color path for Android themed icons.
- Suggested adaptive background: warm off-white `#FAF8F4` or deep sage `#52796F`.

The app’s avatar selector is already structured so these files can replace the temporary options without changing profile behavior.
