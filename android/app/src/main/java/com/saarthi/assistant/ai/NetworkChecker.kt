package com.saarthi.assistant.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class NetworkType {
    NONE,
    WIFI,
    CELLULAR,
    OTHER
}

/**
 * Decoupled interface for inspecting network connectivity.
 */
interface NetworkChecker {
    fun isConnected(): Boolean
    fun getNetworkType(): NetworkType
}

/**
 * Android implementation verifying network capabilities for Wi-Fi and mobile data.
 */
class AndroidNetworkChecker(context: Context) : NetworkChecker {

    private val connectivityManager = try {
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    } catch (_: Throwable) {
        null
    }

    override fun isConnected(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Throwable) {
            false
        }
    }

    override fun getNetworkType(): NetworkType {
        return try {
            val cm = connectivityManager ?: return NetworkType.NONE
            val activeNetwork = cm.activeNetwork ?: return NetworkType.NONE
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> NetworkType.OTHER
                else -> NetworkType.NONE
            }
        } catch (_: Throwable) {
            NetworkType.NONE
        }
    }
}

/**
 * Fake network checker for pure deterministic unit testing.
 */
class FakeNetworkChecker(
    var connected: Boolean = true,
    var type: NetworkType = NetworkType.WIFI
) : NetworkChecker {
    override fun isConnected(): Boolean = connected
    override fun getNetworkType(): NetworkType = type
}
