# Genart animation ideas

Ideas backlog for on-device procedural **Genart** (Compose Canvas in `:genart`). Not a ship commitment, not AI image generation, not stock photo packs.

**Goal:** simple, satisfying, relaxing Ambient loops for phone preview, Auto album art, and TV Dream.

## Principles

- Slow motion, soft loops, low cognitive load.
- No sudden flashes or high-contrast flicker (car-safe).
- **Live creatures** = silhouettes, flocks, schools, tiny agents — not detailed anatomy sims.
- Status: `idea` until an engine ships (then add a stable `genart.*` id in `GenartCatalog`).

**Shipped today (reference):** drifting particles, wire lattice, soft shadows, vanishing tunnel, tonal geometry, orbiting sphere, layered waves, volumetric rays, falling snow (`genart.snow`), grass in wind (`genart.grass`), bird flock (`genart.birdflock`), layered mountains (`genart.mountains`), aurora ribbons (`genart.aurora`), pond ripples (`genart.pondripples`), falling leaves (`genart.fallingleaves`), breath circles (`genart.breathcircles`), fireplace embers (`genart.fireembers`), wind-blown dunes (`genart.dunes`), constellation twinkle (`genart.constellation`), drifting clouds (`genart.clouds`), soft rain (`genart.rain`), soft fog (`genart.fog`), school of fish (`genart.fishschool`), fireflies (`genart.fireflies`), sunbeams through haze (`genart.sunbeams`), sparse meteors (`genart.meteors`), rising bubbles (`genart.bubbles`), cherry blossom petals (`genart.cherryblossoms`), soft ribbons (`genart.ribbons`), nebula drift (`genart.nebula`).

## Backlog by category

### Weather

- Snow — shipped as `genart.snow`
- Clouds — shipped as `genart.clouds`
- Rain — shipped as `genart.rain`
- Storm (soft; no strobing lightning)
- Water drops on window
- Sunshine
- Rainbow
- Smog
- Smoke
- Fog / brouillard — shipped as `genart.fog`
- Light drizzle
- Heat haze
- Soft wind streaks

### Nature

- Tree
- Flower
- Stars
- Ocean waves
- Lake
- Grass in wind — shipped as `genart.grass`
- Reeds
- Moss / lichen grow
- Desert dunes
- Waterfall mist

### Live nature

Calm agents only:

- School of fish — shipped as `genart.fishschool`
- Bird flock (boids) — shipped as `genart.birdflock`
- Ant trails
- Distant dinosaur silhouettes
- Sleeping pet outline (cat / dog / pig)
- Fireflies (also Light & sky) — shipped as `genart.fireflies`

### Planet

Prefer distant / abstract maps and slow orbits — not busy traffic:

- Cities (soft night lights)
- Roads
- Continents
- Asteroids
- Planets
- Solar system
- Rivers
- Mountains — shipped as `genart.mountains`
- Fields

### Light & sky

- Aurora — shipped as `genart.aurora`
- Sunbeams through haze — shipped as `genart.sunbeams`
- Moonlight ripples
- Candle ember
- Sparse meteor streaks — shipped as `genart.meteors`

### Water & fluids

- Pond ripples — shipped as `genart.pondripples`
- Ink in water
- Rising bubbles — shipped as `genart.bubbles`
- Lava-lamp blobs
- Soft caustics

### Seasons & time

- Falling leaves — shipped as `genart.fallingleaves`
- Cherry blossom petals — shipped as `genart.cherryblossoms`
- Soft day → night wash
- First frost crystals

### Abstract calm

Extends the current genart family:

- Breath circles — shipped as `genart.breathcircles`
- Soft ribbons — shipped as `genart.ribbons`
- Morphing blobs
- Silk folds
- Low-frequency noise field

### Cozy micro

- Aquarium
- Terrarium drip
- Fireplace embers — shipped as `genart.fireembers`
- Steam curl
- Rain on glass (shared with Weather)

### Sand & earth

- Wind-blown dunes — shipped as `genart.dunes`
- Drifting pollen
- Soft landslide dust
- Pebble shore wash

### Celestial soft

Calmer slice overlapping Planet:

- Nebula drift — shipped as `genart.nebula`
- Quiet orbit trails
- Constellation twinkle — shipped as `genart.constellation`
- Eclipse corona

## Out of scope / hard (parked)

- Detailed pets walking or interacting
- City traffic sims
- Storm lightning strobes
- Anything that fights “relax” on Auto / TV Ambient
