package com.sumit.moviehub

import android.os.Bundle
import android.os.StrictMode
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.moviehub.core.utils.initKoin
import com.moviehub.di.appModules
import org.koin.android.ext.koin.androidContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Permit disk reads during startup for library initialization (Koin/Ktor/Reflection)
        val oldPolicy = StrictMode.allowThreadDiskReads()
        try {
            initKoin(appDeclaration = {
                androidContext(this@MainActivity)
            }, modules = appModules())
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
    }
}
