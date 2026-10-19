package fr.geoking.arthur.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.geoking.arthur.R
import fr.geoking.arthur.phone.theme.ArthurTheme

@Composable
fun UpdateAvailableDialog(
    onCancel: () -> Unit,
    onUpdate: () -> Unit,
    messageResId: Int = R.string.update_available_message,
    confirmLabelResId: Int = R.string.action_update,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = { Text(stringResource(messageResId)) },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(stringResource(confirmLabelResId))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview
@Composable
private fun UpdateAvailableDialogPreview() {
    ArthurTheme {
        UpdateAvailableDialog(onCancel = {}, onUpdate = {})
    }
}
