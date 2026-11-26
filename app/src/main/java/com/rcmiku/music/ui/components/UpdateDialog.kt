package com.rcmiku.music.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.pavi2410.appupdater.UpdateState
import com.rcmiku.music.R
import com.rcmiku.music.update.createAppUpdater
import com.rcmiku.music.update.isDebuggableBuild
import kotlinx.coroutines.launch

/** Checks GitHub releases once per launch and offers to download and install a newer build. */
@Composable
fun UpdateDialog() {
    val context = LocalContext.current.applicationContext
    val updater = remember { createAppUpdater(context) }
    val state by updater.state.collectAsState()
    var dismissed by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (!context.isDebuggableBuild()) updater.checkForUpdate()
    }
    DisposableEffect(Unit) { onDispose { updater.close() } }

    if (dismissed) return
    when (val current = state) {
        is UpdateState.UpdateAvailable -> AlertDialog(
            onDismissRequest = { dismissed = true },
            title = { Text(stringResource(R.string.update_available)) },
            text = {
                Column {
                    Text(current.release.tagName)
                    current.release.changelog.takeIf { it.isNotBlank() }?.let { Text(it) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        updater.downloadUpdate()
                        if (updater.state.value is UpdateState.ReadyToInstall) updater.installUpdate()
                    }
                }) { Text(stringResource(R.string.update_now)) }
            },
            dismissButton = {
                TextButton(onClick = { dismissed = true }) {
                    Text(stringResource(R.string.update_later))
                }
            }
        )

        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.update_downloading)) },
            text = { LinearProgressIndicator(progress = { current.progress }) },
            confirmButton = {}
        )

        else -> Unit
    }
}
