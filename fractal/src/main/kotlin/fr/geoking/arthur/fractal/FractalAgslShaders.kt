package fr.geoking.arthur.fractal

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * AGSL programs for GPU fractal fields (API 33+).
 *
 * Inspired by the ProAndroidDev AGSL fractal approach: every pixel is escape-time
 * math on the GPU — resolution-independent, zero image payload.
 *
 * [iType] selects the iterator (matches [FractalType] ordinal). Julia reads [iJuliaC]
 * from touch / animation uniforms.
 */
object FractalAgslShaders {

    /** Unified escape-time fractal shader. Uses float (not half) for edge precision. */
    const val ESCAPE_TIME_AGSL = """
        uniform float2 iResolution;
        uniform float2 iCenter;
        uniform float iZoom;
        uniform float iMaxIter;
        uniform float iType;
        uniform float2 iJuliaC;
        uniform float iPhase;
        uniform float iBrightness;
        layout(color) uniform half4 color1;
        layout(color) uniform half4 color2;
        layout(color) uniform half4 color3;
        layout(color) uniform half4 color4;

        float smoothEscape(float n, float zr2, float zi2, float power) {
            float logZn = log(max(zr2 + zi2, 1e-12)) * 0.5;
            float nu = log(max(logZn / log(2.0), 1e-12)) / log(power);
            return n + 1.0 - nu;
        }

        // Coherent 4-stop mix (Mandelbrot Glow style) — no rainbow HSV.
        half3 palette(float t) {
            float u = fract(t);
            if (u < 0.33) {
                return mix(color1.rgb, color2.rgb, u / 0.33);
            } else if (u < 0.66) {
                return mix(color2.rgb, color3.rgb, (u - 0.33) / 0.33);
            } else {
                return mix(color3.rgb, color4.rgb, (u - 0.66) / 0.34);
            }
        }

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * iResolution) / min(iResolution.x, iResolution.y);
            float halfSpan = 2.2 / max(iZoom, 0.001);
            float2 pos = iCenter + float2(uv.x, -uv.y) * halfSpan;

            float cr = pos.x;
            float ci = pos.y;
            float zr = 0.0;
            float zi = 0.0;
            float pr = 0.0;
            float pi = 0.0;
            float power = 2.0;
            int kind = int(iType + 0.5);

            // Julia / Phoenix / Newton seed z0 from pixel; others seed c from pixel
            if (kind == 1 || kind == 7 || kind == 9) {
                zr = cr;
                zi = ci;
                if (kind == 1) {
                    cr = iJuliaC.x;
                    ci = iJuliaC.y;
                } else if (kind == 7) {
                    cr = 0.5667;
                    ci = -0.5;
                }
            }

            float continuous = -1.0;
            float maxIter = min(iMaxIter, 512.0);

            for (int n = 0; n < 512; n++) {
                if (float(n) >= maxIter) break;
                float zr2 = zr * zr;
                float zi2 = zi * zi;

                if (kind == 9) {
                    // Newton for z^3 - 1 = 0:  z := (2/3)z + 1/(3 z^2)
                    float z2r = zr2 - zi2;
                    float z2i = 2.0 * zr * zi;
                    float invDen = 1.0 / max(z2r * z2r + z2i * z2i, 1e-12);
                    float invR = z2r * invDen;
                    float invI = -z2i * invDen;
                    float nextZr = (2.0 / 3.0) * zr + (1.0 / 3.0) * invR;
                    float nextZi = (2.0 / 3.0) * zi + (1.0 / 3.0) * invI;
                    float dr = nextZr - zr;
                    float di = nextZi - zi;
                    zr = nextZr;
                    zi = nextZi;
                    if (dr * dr + di * di < 1e-10) {
                        float angle = atan(zi, zr);
                        continuous = (angle / 6.2831853 + 0.5 + float(n) * 0.002) * maxIter * 0.15;
                        break;
                    }
                    continue;
                }

                if (zr2 + zi2 > 4.0) {
                    continuous = smoothEscape(float(n), zr2, zi2, power);
                    break;
                }

                float nextZr;
                float nextZi;
                if (kind == 0) {
                    // Mandelbrot
                    nextZi = 2.0 * zr * zi + ci;
                    nextZr = zr2 - zi2 + cr;
                } else if (kind == 1) {
                    // Julia
                    nextZi = 2.0 * zr * zi + ci;
                    nextZr = zr2 - zi2 + cr;
                } else if (kind == 2) {
                    // Burning Ship
                    nextZr = abs(zr2 - zi2 + cr);
                    nextZi = abs(2.0 * zr * zi) + ci;
                } else if (kind == 3) {
                    // Tricorn
                    nextZr = zr2 - zi2 + cr;
                    nextZi = -2.0 * zr * zi + ci;
                } else if (kind == 4) {
                    // Multibrot z^3 + c
                    power = 3.0;
                    nextZr = zr * (zr2 - 3.0 * zi2) + cr;
                    nextZi = zi * (3.0 * zr2 - zi2) + ci;
                } else if (kind == 5) {
                    // Celtic
                    nextZr = abs(zr2 - zi2) + cr;
                    nextZi = 2.0 * zr * zi + ci;
                } else if (kind == 6) {
                    // Buffalo
                    nextZr = abs(zr2 - zi2) + cr;
                    nextZi = -abs(2.0 * zr * zi) + ci;
                } else if (kind == 7) {
                    // Phoenix
                    nextZr = zr2 - zi2 + cr + ci * pr;
                    nextZi = 2.0 * zr * zi + ci * pi;
                    pr = zr;
                    pi = zi;
                } else if (kind == 8) {
                    // Nova — quartic multibrot z^4 + c
                    power = 4.0;
                    float z3r = zr * (zr2 - 3.0 * zi2);
                    float z3i = zi * (3.0 * zr2 - zi2);
                    nextZr = z3r * zr - z3i * zi + cr;
                    nextZi = z3r * zi + z3i * zr + ci;
                } else {
                    nextZi = 2.0 * zr * zi + ci;
                    nextZr = zr2 - zi2 + cr;
                }
                zr = nextZr;
                zi = nextZi;
            }

            if (continuous < 0.0) {
                return half4(0.008, 0.024, 0.086, 1.0);
            }

            // Julia Touch style: soft power curve + 4-stop mix (no stripe cycling).
            float normalized = log(1.0 + continuous) / log(1.0 + maxIter);
            float t = pow(clamp(normalized + iPhase * 0.08, 0.0, 1.0), 0.8);
            half3 col = palette(t);
            float edge = clamp(continuous / maxIter, 0.0, 1.0);
            float glow = 0.88 + 0.22 * pow(edge, 0.55);
            float gain = clamp(glow * iBrightness, 0.75, 1.35);
            return half4(col * half(gain), 1.0);
        }
    """

    fun supportsAgsl(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createEscapeShader(): RuntimeShader = RuntimeShader(ESCAPE_TIME_AGSL)

    /**
     * Mandelbrot AGSL from [tbahlai/agsl](https://github.com/tbahlai/agsl) MandelbrotScreen —
     * pinch/pan-friendly zoom+center uniforms and a 4-stop escape palette.
     */
    const val MANDELBROT_BAHLAI_AGSL = """
        uniform float2 size;
        uniform float zoom;
        uniform float2 center;

        layout(color) uniform half4 color1;
        layout(color) uniform half4 color2;
        layout(color) uniform half4 color3;
        layout(color) uniform half4 color4;

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * size) / min(size.y, size.x);
            float2 c = uv * zoom + center;
            float2 z = float2(0.0);
            float iter = 0.0;
            const float maxIter = 200.0;

            for (float i = 0.0; i < maxIter; i++) {
                float x_new = z.x * z.x - z.y * z.y + c.x;
                float y_new = 2.0 * z.x * z.y + c.y;
                z = float2(x_new, y_new);
                if (dot(z, z) > 4.0) break;
                iter++;
            }

            if (iter == maxIter) return half4(0.0, 0.0, 0.0, 1.0);

            float t = iter / maxIter;
            if (t < 0.33) {
                return mix(color1, color2, t / 0.33);
            } else if (t < 0.66) {
                return mix(color2, color3, (t - 0.33) / 0.33);
            } else {
                return mix(color3, color4, (t - 0.66) / 0.34);
            }
        }
    """

    /**
     * Interactive Julia AGSL from [tbahlai/agsl](https://github.com/tbahlai/agsl) JuliaScreen —
     * touch position drives the Julia constant `c`, with exponential color smoothing.
     */
    const val JULIA_BAHLAI_AGSL = """
        uniform float2 size;
        uniform float2 mouse;
        layout(color) uniform half4 color1;
        layout(color) uniform half4 color2;
        layout(color) uniform half4 color3;
        layout(color) uniform half4 color4;

        half4 main(float2 fragCoord) {
            float2 uv = (fragCoord - 0.5 * size) / min(size.y, size.x);
            uv *= 3.0;

            float2 c = (mouse - 0.5 * size) / min(size.y, size.x) * 2.0;

            float2 z = uv;
            float iter = 0.0;
            const float maxIter = 120.0;

            for (float i = 0.0; i < maxIter; i++) {
                float x = z.x * z.x - z.y * z.y + c.x;
                float y = 2.0 * z.x * z.y + c.y;
                z = float2(x, y);
                if (dot(z, z) > 4.0) break;
                iter++;
            }

            if (iter == maxIter) return half4(0.0, 0.0, 0.0, 1.0);

            float t = pow(iter / maxIter, 0.8);

            if (t < 0.33) {
                return mix(color1, color2, t / 0.33);
            } else if (t < 0.66) {
                return mix(color2, color3, (t - 0.33) / 0.33);
            } else {
                return mix(color3, color4, (t - 0.66) / 0.34);
            }
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createMandelbrotBahlaiShader(): RuntimeShader = RuntimeShader(MANDELBROT_BAHLAI_AGSL)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createJuliaBahlaiShader(): RuntimeShader = RuntimeShader(JULIA_BAHLAI_AGSL)
}
