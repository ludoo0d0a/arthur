package fr.geoking.arthur.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.geoking.arthur.R

@Composable
internal fun StartAmbientBar(
    selectedTitle: String?,
    onStartAmbient: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val container by animateColorAsState(
        if (focused) scheme.secondary else scheme.primary,
        label = "ambient_btn_container",
    )
    val content by animateColorAsState(
        if (focused) scheme.onSecondary else scheme.onPrimary,
        label = "ambient_btn_content",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surfaceContainer,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedVisibility(visible = selectedTitle != null) {
                Text(
                    text = stringResource(R.string.preview_label, selectedTitle.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.testTag("preview_title"),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onStartAmbient,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_ambient"),
                interactionSource = interactionSource,
                shape = MaterialTheme.shapes.large,
                border = if (focused) BorderStroke(3.dp, scheme.primary) else null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = container,
                    contentColor = content,
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = if (focused) 8.dp else 2.dp,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_play_ambient),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.start_ambient),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
