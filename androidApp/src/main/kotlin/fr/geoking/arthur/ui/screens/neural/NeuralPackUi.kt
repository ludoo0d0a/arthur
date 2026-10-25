package fr.geoking.arthur.ui.screens.neural

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import fr.geoking.arthur.R
import fr.geoking.arthur.shared.audio.NeuralAudioPack
import fr.geoking.arthur.ui.components.AudioPackTopic
import fr.geoking.arthur.ui.components.PackCovers
import fr.geoking.arthur.ui.components.PackSelection
import fr.geoking.arthur.ui.components.audioPackTopicOrNull

fun neuralAudioPackFromId(id: String): NeuralAudioPack? =
    NeuralAudioPack.entries.firstOrNull { it.id == id }

fun PackSelection.neuralPackIdOrNull(): String? =
    audioPackTopicOrNull()?.neuralPack?.id

@StringRes
fun NeuralAudioPack.titleRes(): Int = when (this) {
    NeuralAudioPack.ATMOSPHERES_NATURE -> R.string.audio_pack_atmospheres_nature
    NeuralAudioPack.MUSEUMS_CLASSICAL -> R.string.audio_pack_museums_classical
    NeuralAudioPack.GENART_DIGITAL -> R.string.audio_pack_genart_digital
    NeuralAudioPack.EXECUTORCH_MIDI -> R.string.audio_pack_executorch
}

@DrawableRes
fun NeuralAudioPack.coverRes(): Int {
    val topic = when (this) {
        NeuralAudioPack.ATMOSPHERES_NATURE -> AudioPackTopic.AtmospheresNature
        NeuralAudioPack.MUSEUMS_CLASSICAL -> AudioPackTopic.MuseumsClassical
        NeuralAudioPack.GENART_DIGITAL -> AudioPackTopic.GenartDigital
        NeuralAudioPack.EXECUTORCH_MIDI -> AudioPackTopic.ExecuTorch
    }
    return PackCovers.audio(topic)
}
