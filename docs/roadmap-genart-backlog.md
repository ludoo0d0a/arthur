# Genart backlog by category

Part of the [Genart roadmap](roadmap-genart.md) — see that doc for principles, the shipped-today
summary, and where to look for rendering techniques ([roadmap-genart-architecture.md](roadmap-genart-architecture.md)).

### Weather

- Snow — shipped as `genart.snow`
- Clouds — shipped as `genart.clouds`
- Rain — shipped as `genart.rain`
- Storm (soft; no strobing lightning) — shipped as `genart.storm`: blurred cumulonimbus
  silhouettes, rain intensity "breathes" in slow waves, distant glow pulses in place of lightning
  (fades in/out over ~7s, capped alpha add, no hard flash)
- Rain on glass / Water drops on window — droplets grow and merge (simple metaball distance
  test), each droplet redraws a shifted slice of the background gradient behind it to fake
  refraction, blurred edge
- Sunshine — shipped as `genart.sunshine`
- Rainbow — shipped as `genart.rainbow`
- Smog — shipped as `genart.smog` (a desaturated, lower-contrast Fog mood variant)
- Smoke — shipped as `genart.smoke`
- Fog / brouillard — shipped as `genart.fog`
- Light drizzle — shipped as `genart.lightdrizzle` (a sparser, gentler Rain mood variant)
- Heat haze — shipped as `genart.heathaze`: thin horizontal sine-warped bands, cheap analogue
  of refraction shimmer
- Soft wind streaks — translucent curved streaks following a slowly evolving noise-based gust
  field; the same gust field should drive Grass/Dunes/Reeds so a "windy" preset feels coherent

### Nature

- Tree
- Flower
- Stars — see "Star field parallax" (Light & sky, shipped as `genart.starfield`)
- Ocean waves — see "Gerstner ocean swell" (Water & fluids)
- Lake
- Grass in wind — shipped as `genart.grass`
- Reeds — same spring-physics blade as Grass, sparser/taller, near-water palette
- Moss / lichen grow
- Desert dunes
- Waterfall mist
- Tumbleweed drift — rolling silhouette, rotation speed tied to the shared wind-gust field
- Wind chime silhouette — hanging shapes on a spring, driven by the same gust field

### Live nature

Calm agents only:

- School of fish — shipped as `genart.fishschool`
- Bird flock (boids) — shipped as `genart.birdflock`
- Ant trails — shipped as `genart.anttrails`: tiny dot silhouettes offset along one shared
  Lissajous path, no legs/antennae detail
- Distant dinosaur silhouettes
- Sleeping pet outline (cat / dog / pig) — shipped as `genart.sleepingpet`: a curled silhouette
  built from overlapping ovals, breathing scale animation only, zero anatomical detail
- Fireflies (also Light & sky) — shipped as `genart.fireflies`

### Planet

Prefer distant / abstract maps and slow orbits — not busy traffic:

- Cities (soft night lights)
- Roads
- Continents
- Asteroids — sparse tumbling rock silhouettes, slow independent rotation, no fast crossings
- Planets, Solar system, Quiet orbit trails (Celestial soft) — shipped together as
  `genart.solarsystem`: a glowing sun + 3-5 procedural planets (lit-hemisphere/terminator
  shading, blurred atmosphere rim, blurred cloud bands clipped to the disc, one Saturn-like
  ring) sliding along thin flattened elliptical orbit lines over the `genart.starfield`
  backdrop. Each planet completes a whole number of revolutions per loop, decreasing with orbit
  radius — a cheap Kepler's-third-law stand-in that also keeps the loop seamless.
- Rivers
- Mountains — shipped as `genart.mountains`
- Fields

### Light & sky

- Aurora — shipped as `genart.aurora`
- Sunbeams through haze — shipped as `genart.sunbeams`
- Moonlight ripples — shipped as `genart.moonlightripples`: blurred lit-hemisphere/terminator
  moon disc (small-scale reuse of the Solar System planet-shading technique) over Pond Ripples'
  ripple math, one soft blurred reflective beam connecting the two
- Candle ember — shipped as `genart.candleember`
- Sparse meteor streaks — shipped as `genart.meteors`
- Star field parallax — shipped as `genart.starfield`: 3 depth layers of point-stars (size/speed
  increasing and a shallow depth-of-field blur added with "closeness"), independent per-star
  twinkle phase, one very slow straight-line "satellite" point crossing the frame each loop

### Water & fluids

- Pond ripples — shipped as `genart.pondripples`
- Gerstner ocean swell — superpose 3-4 sine wave trains at different frequencies/directions/
  phase speeds (Gerstner-wave approximation) instead of one radial ripple, for a convincing
  open-water surface; reuse for Lake / Ocean waves (Nature) and Moonlight ripples
- Ink in water — shipped as `genart.inkinwater`: blobs bloom (radius grows) while fading
  (alpha shrinks), staggered independent lifecycles so blooms overlap in time
- Rising bubbles — shipped as `genart.bubbles`
- Lava-lamp blobs — shipped as `genart.blobs`
- Soft caustics — thin bright cellular streaks from a domain-warped noise field, blurred, slowly
  drifting (reuse the fbm helper proposed for Clouds/Nebula)

### Seasons & time

- Falling leaves — shipped as `genart.fallingleaves`
- Cherry blossom petals — shipped as `genart.cherryblossoms`
- Soft day → night wash
- First frost crystals — shipped as `genart.frostcrystals`: tiny asterisk-glyph sparkles,
  each on an independent slow fade-in/hold/fade-out cycle (smoothstep, never a snap on/off)

### Abstract calm

Extends the current genart family:

- Breath circles — shipped as `genart.breathcircles`
- Soft ribbons — shipped as `genart.ribbons`
- Morphing blobs — shipped as `genart.blobs`
- Soft noise field — shipped as `genart.noisefield`
- Voronoi wash — shipped as `genart.voronoi`
- Silk folds — shipped as `genart.silk`
- Gradient mesh — shipped as `genart.gradientmesh`
- Arc mosaic — shipped as `genart.arcmosaic`
- Low-frequency noise field (variant of soft noise field)

### Cozy micro

- Aquarium
- Terrarium drip
- Fireplace embers — shipped as `genart.fireembers`
- Steam curl — shipped as `genart.steamcurl`: thinner/lighter than Smoke, curls via layered
  sine displacement, dissipates faster with height
- Rain on glass (shared with Weather)

### Sand & earth

- Wind-blown dunes — shipped as `genart.dunes`
- Drifting pollen — shipped as `genart.pollen`: reuses Fireflies' glow-particle look but with
  in-place wander (no directional fall) and warm yellow-green tint
- Soft landslide dust — shipped as `genart.landslidedust`: Fog's blob mechanism, confined to
  the bottom third, warm ochre/tan palette, slow settle/re-stir alpha breathing
- Pebble shore wash — shipped as `genart.pebbleshore`: Pond Ripples' arc math reoriented as a
  bottom-up sweep, plus static pebble-silhouette scatter

### Celestial soft

Calmer slice overlapping Planet:

- Nebula drift — shipped as `genart.nebula`
- Spiral galaxy drift — place the star field along logarithmic-spiral-arm curves with density
  falloff from the core instead of a uniform scatter, plus a blurred core glow and very slow
  whole-field rotation; reuses Nebula's star-scatter primitives, just a different placement
  function — much more instantly-recognizable as "galaxy" than the current amorphous blob look
- Quiet orbit trails — shipped as part of `genart.solarsystem` (Planet)
- Constellation twinkle — shipped as `genart.constellation`
- Eclipse corona — shipped as `genart.eclipsecorona`: dark disc + blurred bright radial-gradient
  ring over a starfield, alpha capped 0.5-0.7 and modulated by a slow sine (shimmer, never a
  flash) — reuses Storm's capped-pulse pattern

### Sci-fi

Distant / abstract only, same "silhouette not detail" spirit as Live nature — no HUDs, no busy
detail, stays inside the calm/car-safe principles above:

- Quiet orbit trails — shipped as part of `genart.solarsystem` (see Celestial soft)
- Data horizon — an atmospheric upgrade of the existing wire-lattice/tunnel engines: same
  perspective grid-plane scroll, but with volumetric haze (blur) and a slow color pulse instead
  of hard wireframe lines
- Space station silhouette drift — shipped as `genart.spacestation`: one small angular
  silhouette (plain rectangles/ovals, no detail) drifting/rotating a few degrees over the
  starfield backdrop, edge-fades using the same timing as the starfield's satellite point
- Warp streak — shipped as `genart.warpstreak`: Sunbeams' radiating-streak look re-themed as
  starfield rays from a center point, kept extremely slow/soft (no strobe, no hyperspace-jump
  flash)
- Ion trail — shipped as `genart.iontrail`: a glowing probe on a lazy S-curve over the
  starfield backdrop, trail approximated analytically as fading segments behind the current
  position (no per-frame state needed)

## Out of scope / hard (parked)

- Detailed pets walking or interacting
- City traffic sims
- Storm lightning strobes (real flash) — the calm alternative already ships as `genart.storm`
  (distant glow pulses fading over ~7s, capped alpha add, no hard flash); an actual strobe stays
  parked by design, not just unbuilt
- Anything that fights “relax” on Auto / TV Ambient
