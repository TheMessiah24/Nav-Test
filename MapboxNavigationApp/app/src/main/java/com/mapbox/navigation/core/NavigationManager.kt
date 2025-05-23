package com.mapbox.navigation.core

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.models.VoiceInstructions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigationProvider
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesRequestCallback
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.trip.session.TripSessionState
import com.mapbox.navigation.core.trip.session.TripSessionStateObserver
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.ui.maps.NavigationStyles
import java.util.Date
import java.util.Locale

class NavigationManager(private val context: Context) {

    private val mapboxNavigation by lazy {
        MapboxNavigationProvider.create(
            NavigationOptions.Builder(context)
                .accessToken(context.getString(com.mapbox.navigation.R.string.mapbox_access_token))
                .build()
        )
    }

    private var routesObserver: RoutesObserver? = null
    private var routeProgressObserver: RouteProgressObserver? = null
    private var tripSessionStateObserver: TripSessionStateObserver? = null
    private var voiceInstructionsObserver: VoiceInstructionsObserver? = null
    
    // Text-to-speech engine for voice instructions
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    fun initialize() {
        mapboxNavigation.initialize()
        initializeTextToSpeech()
    }
    
    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            isTtsReady = status == TextToSpeech.SUCCESS
            if (isTtsReady) {
                textToSpeech?.language = Locale.US
            }
        }
        
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {}
            override fun onError(utteranceId: String?) {}
        })
    }

    fun requestRoutes(
        origin: Point,
        destination: Point,
        callback: (List<DirectionsRoute>?) -> Unit
    ) {
        val routeOptions = RouteOptions.builder()
            .coordinatesList(listOf(origin, destination))
            .alternatives(true)
            // Enable traffic data for real-time route optimization
            .annotations(listOf(
                DirectionsCriteria.ANNOTATION_CONGESTION,
                DirectionsCriteria.ANNOTATION_MAXSPEED,
                DirectionsCriteria.ANNOTATION_SPEED
            ))
            // Consider traffic conditions when calculating routes
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            // Enable voice instructions
            .voiceInstructions(true)
            // Enable banner instructions (for lane guidance)
            .bannerInstructions(true)
            .departAt(Date())
            .language(context.resources.configuration.locales[0].language)
            .accessToken(context.getString(com.mapbox.navigation.R.string.mapbox_access_token))
            .build()

        mapboxNavigation.requestRoutes(
            routeOptions,
            object : RoutesRequestCallback {
                override fun onRoutesReady(routes: List<DirectionsRoute>) {
                    callback(routes)
                }

                override fun onRoutesRequestFailure(throwable: Throwable, routeOptions: RouteOptions) {
                    callback(null)
                }

                override fun onRoutesRequestCanceled(routeOptions: RouteOptions) {
                    callback(null)
                }
            }
        )
    }

    fun startNavigation(route: DirectionsRoute) {
        mapboxNavigation.setRoutes(listOf(route))
        
        // Register voice instructions observer
        registerVoiceInstructionsObserver()
        
        mapboxNavigation.startTripSession()
    }

    fun stopNavigation() {
        unregisterVoiceInstructionsObserver()
        mapboxNavigation.stopTripSession()
    }
    
    private fun registerVoiceInstructionsObserver() {
        voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
            playVoiceInstruction(voiceInstructions)
        }
        mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver!!)
    }
    
    private fun unregisterVoiceInstructionsObserver() {
        voiceInstructionsObserver?.let {
            mapboxNavigation.unregisterVoiceInstructionsObserver(it)
            voiceInstructionsObserver = null
        }
    }
    
    private fun playVoiceInstruction(voiceInstructions: VoiceInstructions) {
        if (isTtsReady) {
            val announcement = voiceInstructions.announcement()
            textToSpeech?.speak(
                announcement,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "voice_instruction"
            )
        }
    }

    fun registerRoutesObserver(observer: RoutesObserver) {
        routesObserver = observer
        mapboxNavigation.registerRoutesObserver(observer)
    }

    fun unregisterRoutesObserver() {
        routesObserver?.let {
            mapboxNavigation.unregisterRoutesObserver(it)
            routesObserver = null
        }
    }

    fun registerRouteProgressObserver(observer: RouteProgressObserver) {
        routeProgressObserver = observer
        mapboxNavigation.registerRouteProgressObserver(observer)
    }

    fun unregisterRouteProgressObserver() {
        routeProgressObserver?.let {
            mapboxNavigation.unregisterRouteProgressObserver(it)
            routeProgressObserver = null
        }
    }

    fun registerTripSessionStateObserver(observer: TripSessionStateObserver) {
        tripSessionStateObserver = observer
        mapboxNavigation.registerTripSessionStateObserver(observer)
    }

    fun unregisterTripSessionStateObserver() {
        tripSessionStateObserver?.let {
            mapboxNavigation.unregisterTripSessionStateObserver(it)
            tripSessionStateObserver = null
        }
    }

    fun onDestroy() {
        unregisterRoutesObserver()
        unregisterRouteProgressObserver()
        unregisterTripSessionStateObserver()
        unregisterVoiceInstructionsObserver()
        
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        
        mapboxNavigation.onDestroy()
    }

    companion object {
        fun getNavigationMapStyle(): String {
            return NavigationStyles.NAVIGATION_NIGHT_STYLE
        }
    }
}
