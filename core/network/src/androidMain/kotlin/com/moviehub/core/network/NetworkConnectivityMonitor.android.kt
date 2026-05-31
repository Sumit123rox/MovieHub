package com.moviehub.core.network

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.moviehub.core.database.PlatformContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class NetworkConnectivityMonitor actual constructor(ctx: PlatformContext) {
    private val connectivityManager =
        ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkCurrentConnectivity())
    actual val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    actual val isOnlineNow: Boolean get() = _isOnline.value

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
        }

        override fun onLost(network: Network) {
            // Only mark offline if no other network is available
            _isOnline.value = checkCurrentConnectivity()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            _isOnline.value = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun checkCurrentConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    protected fun finalize() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
