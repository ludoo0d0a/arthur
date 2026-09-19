# Premium is a RevenueCat lifetime entitlement

**Superseded for content gates by [ADR 0009](0009-marketplace-pack-skus.md).** Premium remains a store-agnostic **lifetime** RevenueCat product for **global UX** (no ads, favorites). Pack content unlocks are Marketplace SKUs. Raw Play Billing in UI would lock Android-only patterns; the app reads entitlements, not purchase tokens at call sites.
