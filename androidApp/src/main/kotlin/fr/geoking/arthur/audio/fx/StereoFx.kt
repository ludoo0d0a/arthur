package fr.geoking.arthur.audio.fx

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Constant-power pan gains for [pan] in -1 (left) .. +1 (right). */
class Pan(pan: Float) {
    private companion object {
        const val SQRT2 = 1.4142135f
    }

    val l: Float
    val r: Float

    init {
        val a = ((pan.coerceIn(-1f, 1f) + 1f) * 0.25f * PI.toFloat())
        l = cos(a) * SQRT2
        r = sin(a) * SQRT2
    }
}

/**
 * Freeverb-style stereo reverb: 8 damped combs + 4 allpasses per channel, right channel
 * detuned for width. Output is read from [outL] / [outR] after [process].
 */
class StereoReverb {
    private var sr = 0
    private var combsL = emptyArray<FloatArray>()
    private var combsR = emptyArray<FloatArray>()
    private var apsL = emptyArray<FloatArray>()
    private var apsR = emptyArray<FloatArray>()
    private var combIdx = IntArray(0)
    private var apIdx = IntArray(0)
    private var damp1L = FloatArray(8)
    private var damp1R = FloatArray(8)

    private var feedback = 0.84f
    private var damp = 0.35f

    var outL = 0f
        private set
    var outR = 0f
        private set

    fun configure(sampleRate: Int, roomSize: Float = 0.5f, damping: Float = 0.35f) {
        feedback = 0.70f + roomSize.coerceIn(0f, 1f) * 0.26f
        damp = damping.coerceIn(0f, 0.9f)
        if (sampleRate == sr && combsL.isNotEmpty()) return
        sr = sampleRate
        val k = sampleRate / 44100f
        fun len(n: Int) = (n * k).toInt().coerceAtLeast(2)
        combsL = Array(8) { FloatArray(len(COMB[it])) }
        combsR = Array(8) { FloatArray(len(COMB[it] + SPREAD)) }
        apsL = Array(4) { FloatArray(len(ALLPASS[it])) }
        apsR = Array(4) { FloatArray(len(ALLPASS[it] + SPREAD)) }
        combIdx = IntArray(16)
        apIdx = IntArray(8)
        damp1L = FloatArray(8)
        damp1R = FloatArray(8)
    }

    /** Feed one mono send sample; results land in [outL]/[outR] (wet only). */
    fun process(input: Float) {
        if (combsL.isEmpty()) {
            outL = 0f
            outR = 0f
            return
        }
        val x = input * 0.015f
        var sumL = 0f
        var sumR = 0f
        for (c in 0 until 8) {
            val bl = combsL[c]
            val il = combIdx[c]
            val yl = bl[il]
            damp1L[c] = yl * (1f - damp) + damp1L[c] * damp
            bl[il] = x + damp1L[c] * feedback
            combIdx[c] = if (il + 1 >= bl.size) 0 else il + 1
            sumL += yl

            val br = combsR[c]
            val ir = combIdx[8 + c]
            val yr = br[ir]
            damp1R[c] = yr * (1f - damp) + damp1R[c] * damp
            br[ir] = x + damp1R[c] * feedback
            combIdx[8 + c] = if (ir + 1 >= br.size) 0 else ir + 1
            sumR += yr
        }
        for (a in 0 until 4) {
            val bl = apsL[a]
            val il = apIdx[a]
            val dl = bl[il]
            val ol = -sumL + dl
            bl[il] = sumL + dl * 0.5f
            apIdx[a] = if (il + 1 >= bl.size) 0 else il + 1
            sumL = ol

            val br = apsR[a]
            val ir = apIdx[4 + a]
            val dr = br[ir]
            val or = -sumR + dr
            br[ir] = sumR + dr * 0.5f
            apIdx[4 + a] = if (ir + 1 >= br.size) 0 else ir + 1
            sumR = or
        }
        outL = sumL * 3f
        outR = sumR * 3f
    }

    fun reset() {
        combsL.forEach { it.fill(0f) }
        combsR.forEach { it.fill(0f) }
        apsL.forEach { it.fill(0f) }
        apsR.forEach { it.fill(0f) }
        damp1L.fill(0f)
        damp1R.fill(0f)
    }

    private companion object {
        val COMB = intArrayOf(1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617)
        val ALLPASS = intArrayOf(556, 441, 341, 225)
        const val SPREAD = 23
    }
}

/** Mono-in / stereo-out ensemble chorus (two opposite-phase modulated taps + dry). */
class StereoChorus {
    private var buf = FloatArray(0)
    private var w = 0
    private var phase = 0.0
    private var sr = 1

    var outL = 0f
        private set
    var outR = 0f
        private set

    fun configure(sampleRate: Int) {
        if (sampleRate == sr && buf.isNotEmpty()) return
        sr = sampleRate
        buf = FloatArray((0.06 * sampleRate).toInt().coerceAtLeast(8))
        w = 0
    }

    fun process(input: Float, rateHz: Float = 0.35f, depthMs: Float = 3.2f, baseMs: Float = 14f) {
        if (buf.isEmpty()) {
            outL = input
            outR = input
            return
        }
        buf[w] = input
        phase += rateHz / sr
        if (phase >= 1.0) phase -= 1.0
        val a = sin(2.0 * PI * phase).toFloat()
        val b = sin(2.0 * PI * (phase + 0.5)).toFloat()
        val dL = (baseMs + depthMs * a) * sr / 1000f
        val dR = (baseMs + depthMs * b) * sr / 1000f
        outL = input * 0.6f + tap(dL) * 0.5f
        outR = input * 0.6f + tap(dR) * 0.5f
        w = if (w + 1 >= buf.size) 0 else w + 1
    }

    private fun tap(delaySamples: Float): Float {
        val n = buf.size
        val pos = w - delaySamples
        val p0 = kotlin.math.floor(pos).toInt()
        val f = pos - p0
        val i0 = Math.floorMod(p0, n)
        val i1 = if (i0 + 1 >= n) 0 else i0 + 1
        return buf[i0] * (1f - f) + buf[i1] * f
    }

    fun reset() = buf.fill(0f)
}

/** Tempo-synced ping-pong delay with a darkening low-pass in the feedback path. */
class PingPongDelay {
    private var l = FloatArray(0)
    private var r = FloatArray(0)
    private var w = 0
    private var delay = 1
    private var lpL = 0f
    private var lpR = 0f
    private var feedback = 0.38f

    var outL = 0f
        private set
    var outR = 0f
        private set

    fun configure(sampleRate: Int, delaySeconds: Float, feedback: Float = 0.38f) {
        val len = (1.5f * sampleRate).toInt().coerceAtLeast(8)
        if (l.size != len) {
            l = FloatArray(len)
            r = FloatArray(len)
            w = 0
        }
        delay = (delaySeconds * sampleRate).toInt().coerceIn(1, len - 1)
        this.feedback = feedback.coerceIn(0f, 0.7f)
    }

    fun process(input: Float) {
        if (l.isEmpty()) {
            outL = 0f
            outR = 0f
            return
        }
        val n = l.size
        val rp = Math.floorMod(w - delay, n)
        val dl = l[rp]
        val dr = r[rp]
        lpL += 0.35f * (dl - lpL)
        lpR += 0.35f * (dr - lpR)
        // Input enters left; each side feeds the other.
        l[w] = input + lpR * feedback
        r[w] = lpL * feedback
        outL = dl
        outR = dr
        w = if (w + 1 >= n) 0 else w + 1
    }

    fun reset() {
        l.fill(0f)
        r.fill(0f)
        lpL = 0f
        lpR = 0f
    }
}
