# Photo / art display UX

Product backlog for still **Artwork** on Control Plane preview and Canvases (Auto / TV): chrome, detail, favorites, selection, Personal Photos sync, and ambient filters. Domain language: [`CONTEXT.md`](../CONTEXT.md). Pairing: [ADR 0004](adr/0004-canvas-pairing-lan-qr.md).

**Goal:** make Photo Artwork and museum stills feel curated and personal — readable identity on the image, deeper context on demand, user-shaped pools, and soft ambient treatments — without turning Canvases into a gallery browser.

Status: `idea` until shipped (then move to **Shipped** or strike through with a note).

## Principles

- Ambient first: overlays and filters must stay calm (no busy chrome on Auto; TV Dream stays lean).
- Attribution is product, not chrome clutter — title / source / date when useful, hideable for pure ambience.
- Personal Photos stay on-device + LAN pairing blobs; never a cloud photo library in v1.
- Prefer Control Plane for browse/detail; Canvases consume Prepared Rotation.

## Shipped (foundation)

| Piece | Notes |
|-------|--------|
| `Artwork` metadata | `title`, `attribution`, `sourceId` (no date field yet) |
| Category / Source filters | Control Plane chip rows (`FilterChipRows`) |
| Personal Photos gate | Premium + `ArtworkKind.PersonalPhoto` in Content Engine |
| Phone → TV blobs | Pairing codec treats Personal Photos as blob payload |

## Backlog

### Title on photo

Show identity **on** the still (preview, Ambient, optionally Auto subtitle / TV fade-in):

- **Name** (Artwork title)
- **Source** (museum / stock / Personal Photos)
- **Date** (creation / acquisition / photograph date when the Source provides it — extend `Artwork` metadata)

Toggle: always / on tap / never (ambient-only mode).

### Artwork detail

Richer context beyond the card line:

- Detail sheet or subpage: description, license, attribution, optional external **link** (museum object page, Unsplash/Pexels photo page)
- Open from Control Plane catalog; optional short affordance from Ambient preview
- Keep TV/Auto free of browse UI (ADR 0001 / TV Canvas screensaver)

### Favorites

- Star / heart Artwork on Control Plane; persist locally
- Favorites as a first-class pool for Ambient Rotation and custom random (below)
- Sync favorite **ids** in pairing manifest when relevant (not full blobs unless Personal Photos)

### Color change by swipe

- Horizontal (or circular) swipe on preview / Ambient to shift color grade / tint / palette of the current still
- Persist last grade per Artwork or as a session Ambient preference
- Must remain car-safe (no strobing); Auto may expose grade only if host UX allows

### Custom random selection

User-defined random pools for Ambient Rotation, not only “all prepared”:

- Per **category** (photo / painting / sculpture / genart / …)
- Per **Source** (Rijksmuseum, Met, Pexels, …)
- **Favorites** only
- Combinations (e.g. museum paintings + favorites)

Control Plane configures the pool; Canvases draw randomly from it on the existing rotation interval.

### Personal Photos (phone → TV)

Complete the Premium Personal Photos Source:

- Pick from device (Photo Picker) on Control Plane
- Include in Prepared Rotation
- Sync selected locals to TV via existing pairing **blobs** (not live streaming)
- Clear UX for “on phone only” vs “paired / synced to TV”

### Ambient art photo filter

Optional post-process over stills to create a **stylish ambient art** look (soft blur, vignette, tonal grade, light grain, painterly wash — not AI restyle):

- Presets + intensity
- Apply in preview and when baking/caching stills for Auto/TV where needed
- Respect license/ToS (do not strip required attribution overlays when the filter is on)

## Suggested order

1. Title on photo (+ date field when Sources can supply it)
2. Artwork detail (sheet + link)
3. Favorites + custom random pools
4. Personal Photos picker UI + TV blob sync polish
5. Ambient art filters
6. Color change by swipe (gesture polish; depends on ambient chrome decisions)

## Out of scope (for this backlog)

- Leanback TV gallery browse
- Cloud photo backup / account sync
- Prompt-based AI image generation
- Replacing museum/stock Sources with user scrapes
