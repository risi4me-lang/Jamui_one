package com.example.jamuione

import android.app.Application
import com.example.jamuione.util.LocationDataProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JamuiOneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        
        // Initialize Location Data Provider
        LocationDataProvider.initialize(this)
    }
}
