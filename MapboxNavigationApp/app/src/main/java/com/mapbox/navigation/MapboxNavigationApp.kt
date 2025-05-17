package com.mapbox.navigation

import android.app.Application
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.search.MapboxSearchSdk

class MapboxNavigationApp : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize the Mapbox Search SDK
        MapboxSearchSdk.initialize(
            application = this,
            accessToken = getString(R.string.mapbox_access_token),
            locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        )
    }
}
