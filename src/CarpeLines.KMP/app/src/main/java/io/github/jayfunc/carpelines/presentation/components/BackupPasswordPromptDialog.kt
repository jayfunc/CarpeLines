package io.github.jayfunc.carpelines.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jayfunc.carpelines.R
import io.github.jayfunc.carpelines.ui.m3e.M3ETextField

/**
 * Password prompt for opening an encrypted backup. Shared by the settings page and the first-run
 * setup, so a user who encrypted a backup can still open it right after reinstalling.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupPasswordPromptDialog(
    wrongPassword: Boolean,
    isInspecting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isInspecting) onDismiss() },
        title = { Text(stringResource(R.string.backup_encrypted_prompt_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.backup_encrypted_prompt_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                M3ETextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.backup_password_hint)) },
                    singleLine = true,
                    enabled = !isInspecting,
                    isError = wrongPassword,
                    supportingText = if (wrongPassword) {
                        { Text(stringResource(R.string.backup_wrong_password)) }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isInspecting) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        LoadingIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = password.isNotEmpty() && !isInspecting,
                onClick = { onConfirm(password) }
            ) { Text(stringResource(R.string.backup_unlock), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        },
        dismissButton = {
            TextButton(enabled = !isInspecting, onClick = onDismiss) {
                Text(stringResource(R.string.cancel), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    )
}
