package com.sumit.moviehub

import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moviehub.core.utils.PerformanceMonitor
import com.moviehub.core.utils.initKoin
import com.moviehub.di.appModules
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        PerformanceMonitor.beginSection("MainActivity.onCreate")
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Enable high refresh rate (120Hz) on supported displays
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window?.attributes?.preferredRefreshRate = 120f
        }

        // Permit disk reads during startup for library initialization (Koin/Ktor/Reflection)
        val oldPolicy = StrictMode.allowThreadDiskReads()
        try {
            PerformanceMonitor.beginSection("Koin.init")
            initKoin(appDeclaration = {
                androidContext(this@MainActivity)
            }, modules = appModules())
            PerformanceMonitor.endSection()
        } finally {
            StrictMode.setThreadPolicy(oldPolicy)
        }

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll() // Includes DiskReads, DiskWrites, Network, etc.
                    .penaltyLog()
                    .build()
            )
        }

        setContent {
            App()
        }
        // Report fully drawn for Play Store vitals (TTFD metric)
        reportFullyDrawn()
        PerformanceMonitor.endSection()
    }
}
