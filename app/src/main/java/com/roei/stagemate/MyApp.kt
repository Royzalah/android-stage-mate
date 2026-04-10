package com.roei.stagemate

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.database.FirebaseDatabase
import com.bumptech.glide.Glide
import com.roei.stagemate.utilities.ImageLoader
import com.roei.stagemate.utilities.LocationManager
import com.roei.stagemate.utilities.SharedPrefsManager
import com.roei.stagemate.utilities.SignalManager
import java.util.Locale

// Application singleton per course standard (my-app.md).
// Initializes Firebase, App Check, managers, and locale.
// Must be registered in AndroidManifest.xml: android:name=".MyApp"
class MyApp : Application() {

    companion object {
        lateinit var signalManager: SignalManager
            private set
        lateinit var imageLoader: ImageLoader
            private set
        lateinit var sharedPrefsManager: SharedPrefsManager
            private set
        lateinit var locationManager: LocationManager
            private set
    }

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

        FirebaseApp.initializeApp(this)

        val appCheck = FirebaseAppCheck.getInstance()
        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
        } else {
            appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
        }

        try {
            val rtdbUrl = "https://stagemate-89f9c-default-rtdb.europe-west1.firebasedatabase.app"
            FirebaseDatabase.getInstance(rtdbUrl).setPersistenceEnabled(true)
        } catch (e: Exception) { android.util.Log.w("MyApp", "RTDB persistence init failed", e) }

        signalManager = SignalManager(this)
        imageLoader = ImageLoader(this)
        sharedPrefsManager = SharedPrefsManager(this)
        locationManager = LocationManager(this)

        setupLanguage()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.get(this).trimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.get(this).clearMemory()
    }

    private fun setupLanguage() {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.create(Locale.ENGLISH)
        )
    }

}
