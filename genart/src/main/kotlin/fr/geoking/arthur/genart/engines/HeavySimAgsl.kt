package fr.geoking.arthur.genart.engines

/** Soft filament field (API 33+) — lightweight stand-in for physarum transport. */
internal const val PHYSARUM_SOFT_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;
    float f = 0.0;
    for (int i = 0; i < 5; i++) {
        float fi = float(i);
        float2 q = p * (1.4 + 0.35 * fi);
        q += float2(sin(t * 0.35 + fi), cos(t * 0.28 + fi * 1.3)) * 0.25;
        f += 0.18 / (0.35 + length(float2(sin(q.x * 3.0 + t), cos(q.y * 3.0 - t))));
    }
    f *= iEnergy;
    float3 col = iColor0 * clamp(f, 0.0, 1.4);
    return half4(col, 1.0);
}
"""

/** Soft reaction-diffusion look without a full Gray-Scott grid (API 33+). */
internal const val REACTION_SOFT_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform float iEnergy;
uniform float3 iColor0;
uniform float3 iColor1;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float2 p = uv * 2.0 - 1.0;
    p.x *= iResolution.x / iResolution.y;
    float t = iTime;
    float spots = 0.0;
    for (int i = 0; i < 6; i++) {
        float fi = float(i);
        float2 c = float2(sin(t * 0.2 + fi * 1.7), cos(t * 0.17 + fi * 1.1)) * 0.55;
        float d = length(p - c);
        spots += smoothstep(0.35, 0.05, d + 0.08 * sin(d * 18.0 - t * 2.0));
    }
    spots = clamp(spots * iEnergy, 0.0, 1.5);
    float3 col = mix(iColor0 * 0.15, mix(iColor0, iColor1, clamp(spots, 0.0, 1.0)), clamp(spots, 0.0, 1.0));
    return half4(col, 1.0);
}
"""
