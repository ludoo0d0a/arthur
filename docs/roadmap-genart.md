# Genart animation ideas

Ideas backlog for on-device procedural **Genart** (Compose Canvas in `:genart`). Not a ship commitment, not AI image generation, not stock photo packs.

**Goal:** simple, satisfying, relaxing Ambient loops for phone preview, Auto album art, and TV Dream.

This roadmap is split into three parts:

- **This doc** — principles and the shipped-today reference.
- [roadmap-genart-backlog.md](roadmap-genart-backlog.md) — the ideas backlog, organized by
  category (Weather, Nature, Live nature, Planet, Light & sky, Water & fluids, Seasons & time,
  Abstract calm, Cozy micro, Sand & earth, Celestial soft, Sci-fi), plus what's explicitly out
  of scope.
- [roadmap-genart-architecture.md](roadmap-genart-architecture.md) — the `GenartRegistry`
  plugin mechanism, and the realism/rendering-technique upgrades (blur, noise, physics-inspired
  motion, shaders) to reach for on new and existing engines.

## Principles

- Slow motion, soft loops, low cognitive load.
- No sudden flashes or high-contrast flicker (car-safe).
- **Live creatures** = silhouettes, flocks, schools, tiny agents — not detailed anatomy sims.
- Status: `idea` until an engine ships (then add a stable `genart.*` id in `GenartCatalog`).

## Shipped today (88 engines)

drifting particles, wire lattice, soft shadows, vanishing tunnel, tonal geometry, orbiting sphere, layered waves, volumetric rays, falling snow (`genart.snow`), grass in wind (`genart.grass`), bird flock (`genart.birdflock`), layered mountains (`genart.mountains`), aurora ribbons (`genart.aurora`), pond ripples (`genart.pondripples`), falling leaves (`genart.fallingleaves`), breath circles (`genart.breathcircles`), fireplace embers (`genart.fireembers`), wind-blown dunes (`genart.dunes`), constellation twinkle (`genart.constellation`), drifting clouds (`genart.clouds`), soft rain (`genart.rain`), soft fog (`genart.fog`), school of fish (`genart.fishschool`), fireflies (`genart.fireflies`), sunbeams through haze (`genart.sunbeams`), sparse meteors (`genart.meteors`), rising bubbles (`genart.bubbles`), cherry blossom petals (`genart.cherryblossoms`), soft ribbons (`genart.ribbons`), nebula drift (`genart.nebula`), morphing blobs (`genart.blobs`), soft noise field (`genart.noisefield`), voronoi wash (`genart.voronoi`), silk folds (`genart.silk`), gradient mesh (`genart.gradientmesh`), arc mosaic (`genart.arcmosaic`), soft storm (`genart.storm`), star field parallax (`genart.starfield`), solar system (`genart.solarsystem`), candle ember (`genart.candleember`), soft rainbow (`genart.rainbow`), soft smog (`genart.smog`), rising smoke (`genart.smoke`), heat haze (`genart.heathaze`), soft sunshine (`genart.sunshine`), light drizzle (`genart.lightdrizzle`), steam curl (`genart.steamcurl`), drifting pollen (`genart.pollen`), soft landslide dust (`genart.landslidedust`), pebble shore wash (`genart.pebbleshore`), first frost crystals (`genart.frostcrystals`), ion trail (`genart.iontrail`), ant trails (`genart.anttrails`), sleeping pet outline (`genart.sleepingpet`), warp streak (`genart.warpstreak`), space station drift (`genart.spacestation`), moonlight ripples (`genart.moonlightripples`), eclipse corona (`genart.eclipsecorona`), ink in water (`genart.inkinwater`), wind chime silhouette (`genart.windchime`), soft day-night wash (`genart.daynightwash`), tumbleweed drift (`genart.tumbleweed`), distant dinosaur silhouettes (`genart.dinosaurs`), city night lights (`genart.citylights`), moss growth (`genart.moss`), terrarium drip (`genart.terrariumdrip`), aquarium (`genart.aquarium`), soft fields (`genart.fields`), rain on glass (`genart.rainonglass`), reeds (`genart.reeds`), canyon dunes (`genart.canyondunes`), continents (`genart.continents`), roads (`genart.roads`), rivers (`genart.rivers`), tree in wind (`genart.tree`), flower bloom (`genart.flower`), lake surface (`genart.lake`), asteroids (`genart.asteroids`), waterfall mist (`genart.waterfallmist`), soft wind streaks (`genart.windstreaks`), ocean swell (`genart.oceanswell`), spiral galaxy drift (`genart.galaxy`), data horizon (`genart.datahorizon`), soft caustics (`genart.caustics`), low-frequency noise field (`genart.lowfreqnoise`), wildfire (`genart.fire`), paper-cut pack (`genart.papercut`), diamond weave (`genart.diamondweave`).

The original ideas backlog is now **fully shipped** (see
[roadmap-genart-backlog.md](roadmap-genart-backlog.md)) — future rounds should propose fresh
categories/ideas rather than draw down what's left there.

See [roadmap-genart-backlog.md](roadmap-genart-backlog.md) for what's still open per category,
and [roadmap-genart-architecture.md](roadmap-genart-architecture.md) for how engines are wired
up and rendered.
