package fr.geoking.arthur.shared.neural

/**
 * Phone Neural Music navigation routes.
 * Pack identity uses [fr.geoking.arthur.shared.audio.NeuralAudioPack.id].
 */
sealed class NeuralMusicRoute {
    abstract val packId: String

    data class Hub(override val packId: String) : NeuralMusicRoute()

    data class Create(override val packId: String) : NeuralMusicRoute()

    data class ModelSetup(
        override val packId: String,
        val promptId: String,
        val durationSec: Int = 120,
        val resumeToCreate: Boolean = true,
    ) : NeuralMusicRoute()

    data class Generating(
        override val packId: String,
        val promptId: String,
        val durationSec: Int = 120,
    ) : NeuralMusicRoute()

    data class Result(
        override val packId: String,
        val takeId: String,
    ) : NeuralMusicRoute()
}

/** Parent route when the user presses back; null means leave the Neural stack. */
fun NeuralMusicRoute.back(): NeuralMusicRoute? = when (this) {
    is NeuralMusicRoute.Hub -> null
    is NeuralMusicRoute.Create -> NeuralMusicRoute.Hub(packId)
    is NeuralMusicRoute.ModelSetup ->
        if (resumeToCreate) NeuralMusicRoute.Create(packId) else NeuralMusicRoute.Hub(packId)
    is NeuralMusicRoute.Generating -> NeuralMusicRoute.Create(packId)
    is NeuralMusicRoute.Result -> NeuralMusicRoute.Hub(packId)
}

/**
 * Next route after the user taps Generate on Create.
 * [modelReady] true → Generating; false → ModelSetup gate.
 */
fun routeAfterGenerateRequest(
    packId: String,
    promptId: String,
    durationSec: Int,
    modelReady: Boolean,
): NeuralMusicRoute =
    if (modelReady) {
        NeuralMusicRoute.Generating(packId, promptId, durationSec)
    } else {
        NeuralMusicRoute.ModelSetup(packId, promptId, durationSec)
    }

/** After ModelSetup reports Ready, continue into generation with the same brief. */
fun NeuralMusicRoute.ModelSetup.continueToGenerating(): NeuralMusicRoute.Generating =
    NeuralMusicRoute.Generating(packId, promptId, durationSec)
