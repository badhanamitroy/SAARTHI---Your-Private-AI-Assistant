package com.saarthi.assistant.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext

enum class NetworkStatus {
    ONLINE,
    OFFLINE
}

class ConnectivityObserver(context: Context) {
    private val connectivityManager = try {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (_: Throwable) {
        null
    }

    fun isCurrentlyOnline(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (_: Throwable) {
            // Guard against SecurityException or OEM permission restriction
            false
        }
    }

    fun observe(onStatusChanged: (NetworkStatus) -> Unit): () -> Unit {
        val cm = connectivityManager ?: run {
            onStatusChanged(NetworkStatus.OFFLINE)
            return {}
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onStatusChanged(NetworkStatus.ONLINE)
            }

            override fun onLost(network: Network) {
                if (isCurrentlyOnline()) {
                    onStatusChanged(NetworkStatus.ONLINE)
                } else {
                    onStatusChanged(NetworkStatus.OFFLINE)
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                onStatusChanged(if (hasInternet) NetworkStatus.ONLINE else NetworkStatus.OFFLINE)
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, callback)
            onStatusChanged(if (isCurrentlyOnline()) NetworkStatus.ONLINE else NetworkStatus.OFFLINE)

            return {
                try {
                    cm.unregisterNetworkCallback(callback)
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            onStatusChanged(if (isCurrentlyOnline()) NetworkStatus.ONLINE else NetworkStatus.OFFLINE)
            return {}
        }
    }
}

@Composable
fun rememberNetworkStatus(): State<NetworkStatus> {
    val context = LocalContext.current
    return produceState(initialValue = NetworkStatus.OFFLINE) {
        try {
            val observer = ConnectivityObserver(context)
            value = if (observer.isCurrentlyOnline()) NetworkStatus.ONLINE else NetworkStatus.OFFLINE
            val unregister = observer.observe { status ->
                value = status
            }
            awaitDispose {
                try {
                    unregister()
                } catch (_: Throwable) {}
            }
        } catch (_: Throwable) {
            value = NetworkStatus.OFFLINE
        }
    }
}
