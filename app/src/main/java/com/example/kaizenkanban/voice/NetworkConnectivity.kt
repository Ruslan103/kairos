package com.example.kaizenkanban.voice

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * True only when there is a validated network with internet.
 * Speech recognition (Google STT) needs this — a Wi‑Fi link without WAN is offline.
 */
fun Context.hasUsableInternet(): Boolean {
    return runCatching {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return false
        cm.allNetworks.any { network ->
            val caps = cm.getNetworkCapabilities(network) ?: return@any false
            val hasTransport =
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                hasTransport
        }
    }.getOrDefault(false)
}

/** Live network availability for Compose (speech recognition needs internet). */
@Composable
fun rememberIsOnline(): Boolean {
    val context = LocalContext.current.applicationContext
    var online by remember { mutableStateOf(context.hasUsableInternet()) }
    DisposableEffect(context) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (cm == null) {
            online = false
            return@DisposableEffect onDispose { }
        }
        val main = Handler(Looper.getMainLooper())
        val refresh = {
            main.post { online = context.hasUsableInternet() }
            Unit
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onUnavailable() = refresh()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = refresh()
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        refresh()
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
    return online
}
