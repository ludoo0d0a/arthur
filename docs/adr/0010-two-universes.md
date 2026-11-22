# Two product universes (Gallery vs Generative)

## Status

Accepted

## Context

Arthur ships museum stills and procedural genart in one APK. Mixing them on the same home grid dilutes ASO and purchase intent, but splitting into two apps doubles listing/CI cost while the Control Plane / canvases / Content Engine stay shared.

## Decision

One app, two **universes**:

| Universe | Pack families |
|----------|----------------|
| **Gallery** | Museum, Painting, Sculpture, Photo, Video, Personal |
| **Generative** | Genart (incl. fractal / custom topics), VintageGames if re-enabled |

- First launch shows `UniversePickerScreen` (preference only).
- Header chips always allow switching; no paywall to try the other side.
- Marketplace lists Diamond + packs for the **active** universe, with a CTA to switch.
- Audio / music remains gated by `FeatureFlag.musicEnabled` and is out of scope for universe affinity until re-enabled.
- No cross-universe ambient rotation (one `PackSelection` / family per session, as before).

## Consequences

- Clearer home and boutique storytelling without a second `applicationId`.
- Cold start restores last `activeUniverse` via `UniverseSettings`.
- Analytics: `universe_selected` (first) / `universe_switched`.
- Future spin-off to two apps can reuse the same mapping if one universe dominates revenue.
