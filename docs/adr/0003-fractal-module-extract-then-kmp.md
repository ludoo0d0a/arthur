# Fractal Module: extract Julius, then deepen into KMP

Julius already has working fractal renderers (`FractalEffectCanvas` / `FractalEffectSurface`) but they live in the Julius app, not a shared engine. Arthur **copies/extracts** them into a local **Fractal Module**, then moves computation into `commonMain` for Apple-Ready Shared later. A live Julius Git dependency couples two products; a pure rewrite on day one delays v1.
