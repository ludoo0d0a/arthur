# Genart backlog by category

Part of the [Genart roadmap](roadmap-genart.md) — see that doc for principles, the shipped-today
summary, and where to look for rendering techniques ([roadmap-genart-architecture.md](roadmap-genart-architecture.md)).

**Status: the original ideas backlog below is now fully shipped.** Every category is complete,
including the one parameter-variant item. Next round should propose fresh ideas rather than draw
from what's left here.

### Weather

- Snow — shipped as `genart.snow`
- Clouds — shipped as `genart.clouds`
- Rain — shipped as `genart.rain`
- Storm (soft; no strobing lightning) — shipped as `genart.storm`: blurred cumulonimbus
  silhouettes, rain intensity "breathes" in slow waves, distant glow pulses in place of lightning
  (fades in/out over ~7s, capped alpha add, no hard flash)
- Rain on glass / Water drops on window — shipped as `genart.rainonglass`: droplets grow and
  occasionally run a short distance with a fading trail, each drawn as a glassy bead with an
  offset highlight (fake refraction), over a blurred background layer
- Sunshine — shipped as `genart.sunshine`
- Rainbow — shipped as `genart.rainbow`
- Smog — shipped as `genart.smog` (a desaturated, lower-contrast Fog mood variant)
- Smoke — shipped as `genart.smoke`
- Fog / brouillard — shipped as `genart.fog`
- Light drizzle — shipped as `genart.lightdrizzle` (a sparser, gentler Rain mood variant)
- Heat haze — shipped as `genart.heathaze`: thin horizontal sine-warped bands, cheap analogue
  of refraction shimmer
- Soft wind streaks — shipped as `genart.windstreaks`: translucent curved streaks driven by a
  shared "gust" scalar (reused from Storm) that speeds up and curves all streaks together

### Nature

- Tree — shipped as `genart.tree`: soft-blob canopy (Clouds' puff technique) swaying as one
  rigid body around the trunk-top pivot (Grass's sway math)
- Flower — shipped as `genart.flower`: stem + ring of soft petal blobs, gentle sway plus a
  subtle breathing bloom-open pulse (BreathCircles' technique)
- Stars — see "Star field parallax" (Light & sky, shipped as `genart.starfield`)
- Ocean waves — see "Gerstner ocean swell" (Water & fluids, shipped as `genart.oceanswell`)
- Lake — shipped as `genart.lake`: mirrored sky-gradient reflection plus a few sparse, gentle
  Pond Ripples at a much lower frequency than Pond Ripples' own defaults
- Grass in wind — shipped as `genart.grass`
- Reeds — shipped as `genart.reeds`: Grass's spring-sway blade mechanism, sparser/taller,
  near-water blue-green palette, reflective water base
- Moss / lichen grow — shipped as `genart.moss`: Morphing Blobs' soft-patch technique at fixed
  positions, each staggered on a very slow grow/recede breathing cycle
- Desert dunes — shipped as `genart.canyondunes` ("Canyon Dunes"): Dunes' mechanism recolored to
  a red-rock/terracotta palette, with a very subtle Heat Haze shimmer near the horizon
- Waterfall mist — shipped as `genart.waterfallmist`: a narrow dense Rain column plus a blurred
  rising Fog/Smoke-style mist cloud at the base
- Tumbleweed drift — shipped as `genart.tumbleweed`: an irregular tangled-circle-cluster
  silhouette rolling across Dunes' desert background, rotation speed tied to horizontal drift
- Wind chime silhouette — shipped as `genart.windchime`: hanging silhouettes swaying together
  on the same shared gust scalar as Soft Wind Streaks

### Live nature

Calm agents only:

- School of fish — shipped as `genart.fishschool`
- Bird flock (boids) — shipped as `genart.birdflock`
- Ant trails — shipped as `genart.anttrails`: tiny dot silhouettes offset along one shared
  Lissajous path, no legs/antennae detail
- Distant dinosaur silhouettes — shipped as `genart.dinosaurs`: 1-2 body-blob-plus-curved-neck
  silhouettes on a Mountains-style dusk horizon, only a slow head/neck sway animates
- Sleeping pet outline (cat / dog / pig) — shipped as `genart.sleepingpet`: a curled silhouette
  built from overlapping ovals, breathing scale animation only, zero anatomical detail
- Fireflies (also Light & sky) — shipped as `genart.fireflies`

### Planet

Prefer distant / abstract maps and slow orbits — not busy traffic:

- Cities (soft night lights) — shipped as `genart.citylights`: a jagged dark skyline silhouette
  with independently-twinkling static window lights (Fireflies' technique, no drift)
- Roads — shipped as `genart.roads`: an empty perspective road (Tunnel's convergence math
  applied to a flat trapezoid) with dashed lane markers sliding toward the vanishing point —
  no vehicles, per the "no traffic sims" scope rule
- Continents — shipped as `genart.continents`: soft-edged Morphing-Blobs-style landmasses on a
  dark ocean background, the whole cluster slowly rotating together like a distant globe
- Asteroids — shipped as `genart.asteroids`: several irregular rock silhouettes over the
  `genart.starfield` backdrop, each independently drifting/tumbling, no fast crossings
- Planets, Solar system, Quiet orbit trails (Celestial soft) — shipped together as
  `genart.solarsystem`: a glowing sun + 3-5 procedural planets (lit-hemisphere/terminator
  shading, blurred atmosphere rim, blurred cloud bands clipped to the disc, one Saturn-like
  ring) sliding along thin flattened elliptical orbit lines over the `genart.starfield`
  backdrop. Each planet completes a whole number of revolutions per loop, decreasing with orbit
  radius — a cheap Kepler's-third-law stand-in that also keeps the loop seamless.
- Rivers — shipped as `genart.rivers`: a winding sine-displaced river band with slow
  downstream ripple highlights (Pond Ripples' technique) between soft green/brown banks
- Mountains — shipped as `genart.mountains`
- Fields — shipped as `genart.fields`: wide horizontal color bands with a Grass-style wavy
  top edge, swaying gently like a breeze passing over a hillside

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
- Gerstner ocean swell — shipped as `genart.oceanswell` ("Ocean Swell"): 3-4 stacked wave-crest
  bands, each summing 3-4 sine trains at different frequencies/directions/phase-speeds, darker
  and denser toward the bottom of the frame for depth
- Ink in water — shipped as `genart.inkinwater`: blobs bloom (radius grows) while fading
  (alpha shrinks), staggered independent lifecycles so blooms overlap in time
- Rising bubbles — shipped as `genart.bubbles`
- Lava-lamp blobs — shipped as `genart.blobs`
- Soft caustics — shipped as `genart.caustics`: thin bright curved streaks in a loose net
  pattern, blurred, drift faked by summing 2-3 sine waves per streak (no true noise helper
  needed)

### Seasons & time

- Falling leaves — shipped as `genart.fallingleaves`
- Cherry blossom petals — shipped as `genart.cherryblossoms`
- Soft day → night wash — shipped as `genart.daynightwash`: full-background gradient crossfades
  day/night palettes over a multi-minute cycle, stars fade in/out with how "night" it is
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
- Low-frequency noise field (variant of soft noise field) — shipped as `genart.lowfreqnoise`:
  fewer, larger, slower-breathing regions than Soft Noise Field, driven by the `loopedFbm` helper
  (see [roadmap-genart-architecture.md](roadmap-genart-architecture.md)) instead of per-cell sine
  wobble — reads as a genuinely large-scale field rather than a busier variant

### Cozy micro

- Aquarium — shipped as `genart.aquarium`: FishSchool's fish + Bubbles' rising-bubble
  technique combined in a smaller, calmer aqua-toned scene
- Terrarium drip — shipped as `genart.terrariumdrip`: 1-3 fixed-position droplets that grow,
  then briefly slide/stretch/fade before restarting, staggered independently
- Fireplace embers — shipped as `genart.fireembers`
- Steam curl — shipped as `genart.steamcurl`: thinner/lighter than Smoke, curls via layered
  sine displacement, dissipates faster with height
- Rain on glass (shared with Weather) — shipped as `genart.rainonglass`

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
- Spiral galaxy drift — shipped as `genart.galaxy`: stars placed along 2-3 logarithmic-spiral
  arms with density biased toward the core, a blurred central glow, and slow whole-field
  rotation (rotating the star positions together, not per-star drift) — much more
  instantly-recognizable as "galaxy" than Nebula's amorphous blob look
- Quiet orbit trails — shipped as part of `genart.solarsystem` (Planet)
- Constellation twinkle — shipped as `genart.constellation`
- Eclipse corona — shipped as `genart.eclipsecorona`: dark disc + blurred bright radial-gradient
  ring over a starfield, alpha capped 0.5-0.7 and modulated by a slow sine (shimmer, never a
  flash) — reuses Storm's capped-pulse pattern

### Sci-fi

Distant / abstract only, same "silhouette not detail" spirit as Live nature — no HUDs, no busy
detail, stays inside the calm/car-safe principles above:

- Quiet orbit trails — shipped as part of `genart.solarsystem` (see Celestial soft)
- Data horizon — shipped as `genart.datahorizon`: a blurred perspective floor-grid (lines
  bunching toward a vanishing point) plus fan lines, tint slowly cross-fading cyan/violet/blue
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
