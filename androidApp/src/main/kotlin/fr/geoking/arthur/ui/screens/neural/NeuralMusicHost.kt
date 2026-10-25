package fr.geoking.arthur.ui.screens.neural

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.geoking.arthur.shared.neural.NeuralMusicRoute
import fr.geoking.arthur.shared.neural.NeuralTakeLibrary
import fr.geoking.arthur.shared.neural.back
import fr.geoking.arthur.shared.neural.continueToGenerating
import fr.geoking.arthur.shared.neural.routeAfterGenerateRequest
import fr.geoking.arthur.source.AmbientAudioSettings
import org.koin.core.context.GlobalContext

/**
 * Phone-only host mapping [NeuralMusicRoute] to dedicated Neural Music screens.
 */
@Composable
fun NeuralMusicHost(
    route: NeuralMusicRoute,
    onRouteChange: (NeuralMusicRoute?) -> Unit,
    modifier: Modifier = Modifier,
    takeLibrary: NeuralTakeLibrary = remember {
        GlobalContext.get().get<NeuralTakeLibrary>()
    },
    audioSettings: AmbientAudioSettings? = remember {
        runCatching { GlobalContext.get().get<AmbientAudioSettings>() }.getOrNull()
    },
) {
    var hubEpoch by remember { mutableIntStateOf(0) }

    fun navigate(next: NeuralMusicRoute?) {
        onRouteChange(next)
    }

    fun goBack() {
        navigate(route.back())
    }

    when (val current = route) {
        is NeuralMusicRoute.Hub -> NeuralHubScreen(
            packId = current.packId,
            takeLibrary = takeLibrary,
            onOpenCreate = { navigate(NeuralMusicRoute.Create(current.packId)) },
            onOpenTake = { takeId ->
                navigate(NeuralMusicRoute.Result(current.packId, takeId))
            },
            onDismiss = { navigate(null) },
            modifier = modifier,
            refreshKey = hubEpoch,
        )
        is NeuralMusicRoute.Create -> NeuralCreateScreen(
            packId = current.packId,
            onGenerate = { promptId, durationSec, modelReady ->
                navigate(
                    routeAfterGenerateRequest(
                        packId = current.packId,
                        promptId = promptId,
                        durationSec = durationSec,
                        modelReady = modelReady,
                    ),
                )
            },
            onBack = ::goBack,
            modifier = modifier,
        )
        is NeuralMusicRoute.ModelSetup -> NeuralModelSetupScreen(
            packId = current.packId,
            onContinue = { navigate(current.continueToGenerating()) },
            onBack = ::goBack,
            modifier = modifier,
        )
        is NeuralMusicRoute.Generating -> NeuralGeneratingScreen(
            packId = current.packId,
            promptId = current.promptId,
            durationSec = current.durationSec,
            takeLibrary = takeLibrary,
            onCompleted = { takeId ->
                hubEpoch++
                navigate(NeuralMusicRoute.Result(current.packId, takeId))
            },
            onCancel = ::goBack,
            modifier = modifier,
        )
        is NeuralMusicRoute.Result -> NeuralResultScreen(
            packId = current.packId,
            takeId = current.takeId,
            takeLibrary = takeLibrary,
            onVariant = { promptId, durationSec ->
                navigate(
                    NeuralMusicRoute.Generating(
                        packId = current.packId,
                        promptId = promptId,
                        durationSec = durationSec,
                    ),
                )
            },
            onNewTrack = { navigate(NeuralMusicRoute.Create(current.packId)) },
            onSwapTake = { otherId ->
                navigate(NeuralMusicRoute.Result(current.packId, otherId))
            },
            onBack = ::goBack,
            modifier = modifier,
            audioSettings = audioSettings,
        )
    }
}
