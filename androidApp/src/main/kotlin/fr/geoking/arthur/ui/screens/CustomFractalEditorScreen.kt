package fr.geoking.arthur.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R
import fr.geoking.arthur.fractal.CustomFractalEffectCanvas
import fr.geoking.arthur.fractal.CustomFractalMorphMode
import fr.geoking.arthur.fractal.CustomFractalParams
import fr.geoking.arthur.fractal.FractalQuality
import fr.geoking.arthur.fractal.NormPoint
import fr.geoking.arthur.shared.domain.Artwork

/**
 * Premium Control Plane editor: tap points on AMOLED-dark field → unique Bezier fractal.
 */
@Composable
fun CustomFractalEditorScreen(
    isPremium: Boolean,
    onSave: (CustomFractalParams) -> Artwork,
    onClose: () -> Unit,
    onRequestPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isPremium) {
        CustomFractalUpsell(
            onClose = onClose,
            onRequestPremium = onRequestPremium,
            modifier = modifier,
        )
        return
    }

    val points = remember { mutableStateListOf<NormPoint>() }
    var morphMode by remember { mutableStateOf(CustomFractalMorphMode.Breathe) }
    var colorSeed by remember { mutableIntStateOf(7) }
    var fieldSize by remember { mutableStateOf(IntSize.Zero) }
    val canSave = points.size >= CustomFractalParams.MIN_POINTS

    val previewParams = remember(points.toList(), morphMode, colorSeed) {
        if (points.size >= CustomFractalParams.MIN_POINTS) {
            CustomFractalParams(
                points = points.toList(),
                colorSeed = colorSeed,
                morphMode = morphMode,
            )
        } else {
            null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .testTag("custom_fractal_editor"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.custom_fractal_close))
            }
            Text(
                text = stringResource(R.string.custom_fractal_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            TextButton(
                onClick = {
                    if (canSave) {
                        onSave(
                            CustomFractalParams(
                                points = points.toList(),
                                colorSeed = colorSeed,
                                morphMode = morphMode,
                            ),
                        )
                        onClose()
                    }
                },
                enabled = canSave,
            ) {
                Text(stringResource(R.string.custom_fractal_save))
            }
        }

        Text(
            text = stringResource(
                R.string.custom_fractal_hint,
                points.size,
                CustomFractalParams.MIN_POINTS,
                CustomFractalParams.MAX_POINTS,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .background(Color(0xFF020617))
                .onSizeChanged { fieldSize = it }
                .pointerInput(points.size) {
                    detectTapGestures { offset: Offset ->
                        if (fieldSize.width <= 0 || fieldSize.height <= 0) return@detectTapGestures
                        if (points.size >= CustomFractalParams.MAX_POINTS) return@detectTapGestures
                        points += NormPoint(
                            x = (offset.x / fieldSize.width).coerceIn(0f, 1f),
                            y = (offset.y / fieldSize.height).coerceIn(0f, 1f),
                        )
                    }
                }
                .testTag("custom_fractal_plot"),
        ) {
            if (previewParams != null) {
                CustomFractalEffectCanvas(
                    params = previewParams,
                    isActive = true,
                    quality = FractalQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = stringResource(R.string.custom_fractal_tap_prompt),
                    color = Color(0xFF64748B),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CustomFractalMorphMode.entries.forEach { mode ->
                val selected = morphMode == mode
                OutlinedButton(onClick = { morphMode = mode }) {
                    Text(
                        text = mode.name,
                        color = if (selected) Color(0xFF22D3EE) else Color(0xFF94A3B8),
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { if (points.isNotEmpty()) points.removeAt(points.lastIndex) },
                enabled = points.isNotEmpty(),
            ) {
                Text(stringResource(R.string.custom_fractal_undo))
            }
            OutlinedButton(
                onClick = { points.clear() },
                enabled = points.isNotEmpty(),
            ) {
                Text(stringResource(R.string.custom_fractal_clear))
            }
            OutlinedButton(
                onClick = { colorSeed = (colorSeed + 17) % 360 },
            ) {
                Text(stringResource(R.string.custom_fractal_recolor))
            }
        }
        Box(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun CustomFractalUpsell(
    onClose: () -> Unit,
    onRequestPremium: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .padding(24.dp)
            .testTag("custom_fractal_upsell"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.custom_fractal_premium_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Box(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.custom_fractal_premium_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
        )
        Box(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPremium) {
            Text(stringResource(R.string.custom_fractal_unlock))
        }
        TextButton(onClick = onClose) {
            Text(stringResource(R.string.custom_fractal_close))
        }
    }
}
