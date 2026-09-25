package com.example.kaizenkanban.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.kaizenkanban.data.local.KairosPreferences
import com.example.kaizenkanban.pro.Entitlements
import com.example.kaizenkanban.pro.ProFeature
import com.example.kaizenkanban.ui.i18n.LocalAppLanguage
import com.example.kaizenkanban.ui.i18n.LocalAppStrings
import com.example.kaizenkanban.ui.viewmodel.SharedViewModel
import com.example.kaizenkanban.voice.SpeechRecognizerClient
import com.example.kaizenkanban.voice.VoiceActionResult
import com.example.kaizenkanban.voice.VoiceCommandExecutor
import com.example.kaizenkanban.voice.hasUsableInternet
import com.example.kaizenkanban.voice.rememberIsOnline
import kotlinx.coroutines.launch

data class VoiceAssistantUiState(
    val listening: Boolean = false,
    val busy: Boolean = false,
    val online: Boolean = true
)

/**
 * Mic trigger for board / projects / pending widget intent.
 * @param openColumnIdProvider current hub on board, or null for Quick Add target.
 */
@Composable
fun rememberVoiceAssistantController(
    viewModel: SharedViewModel,
    openColumnIdProvider: () -> String?
): VoiceAssistantController {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val s = LocalAppStrings.current
    val locale = LocalAppLanguage.current.locale
    val prefs = remember { KairosPreferences(context) }
    val scope = rememberCoroutineScope()
    val openColumnIdProviderState = rememberUpdatedState(openColumnIdProvider)
    val online = rememberIsOnline()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var ui by remember { mutableStateOf(VoiceAssistantUiState(online = online)) }
    var client by remember { mutableStateOf<SpeechRecognizerClient?>(null) }

    LaunchedEffect(online) {
        ui = ui.copy(online = online)
    }

    val executor = remember(viewModel, prefs) {
        VoiceCommandExecutor(viewModel, prefs)
    }

    fun toast(message: String) {
        mainHandler.post {
            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
        }
    }

    fun showResult(result: VoiceActionResult) {
        val message = when (result) {
            is VoiceActionResult.Added -> s.voiceAdded(result.title)
            is VoiceActionResult.NotUnderstood -> s.voiceNotUnderstood
            VoiceActionResult.NoDestination -> s.voiceNoDestination
            VoiceActionResult.ProRequired -> s.proFeatureLocked
            is VoiceActionResult.Error -> s.voiceError
        }
        toast(message)
    }

    fun handleSpeechError(code: Int) {
        ui = ui.copy(listening = false, busy = false)
        // Offline / STT network failures often arrive as CLIENT or SERVER, not only NETWORK_*.
        val offline = !appContext.hasUsableInternet()
        val message = when {
            offline ||
                code == SpeechRecognizer.ERROR_NETWORK ||
                code == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
                code == SpeechRecognizer.ERROR_SERVER -> s.voiceNetworkError
            code == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> s.voiceMicPermissionNeeded
            code == SpeechRecognizer.ERROR_NO_MATCH ||
                code == SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> s.voiceNoMatch
            code == SpeechRecognizer.ERROR_CLIENT -> s.voiceUnavailable
            else -> s.voiceError
        }
        toast(message)
    }

    fun startListeningInternal() {
        if (ui.listening || ui.busy) {
            client?.destroy()
            client = null
            ui = ui.copy(listening = false, busy = false)
            return
        }
        if (!appContext.hasUsableInternet()) {
            toast(s.voiceNetworkError)
            return
        }
        if (!Entitlements.has(prefs, ProFeature.VoiceAssistant)) {
            showResult(VoiceActionResult.ProRequired)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            handleSpeechError(SpeechRecognizer.ERROR_CLIENT)
            return
        }

        val speech = SpeechRecognizerClient(
            context = context,
            onResult = { text ->
                ui = ui.copy(listening = false, busy = true)
                scope.launch {
                    val result = executor.execute(text, openColumnIdProviderState.value.invoke())
                    ui = ui.copy(busy = false)
                    showResult(result)
                }
            },
            onError = { handleSpeechError(it) },
            onListeningChanged = { listening ->
                ui = ui.copy(listening = listening)
            }
        )
        client?.destroy()
        client = speech
        // No "Listening…" toast until recognizer is ready — avoids false start when offline.
        speech.start(locale)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListeningInternal()
        else toast(s.voiceMicPermissionNeeded)
    }

    DisposableEffect(Unit) {
        onDispose {
            client?.destroy()
            client = null
        }
    }

    // Stable click handler — do not recreate with every ui tick (stale checks / missed toasts).
    val onMicClick = rememberUpdatedState {
        if (!appContext.hasUsableInternet()) {
            toast(s.voiceNetworkError)
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) startListeningInternal()
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    return remember(ui) {
        VoiceAssistantController(
            uiState = ui,
            onMicClick = { onMicClick.value.invoke() }
        )
    }
}

class VoiceAssistantController(
    val uiState: VoiceAssistantUiState,
    val onMicClick: () -> Unit
)
