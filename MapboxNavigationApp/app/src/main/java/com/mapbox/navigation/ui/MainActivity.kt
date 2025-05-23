package com.mapbox.navigation.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.atmosphere.generated.atmosphere
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.skyLayer
import com.mapbox.maps.extension.style.lights.generated.ambientLight
import com.mapbox.maps.extension.style.lights.generated.directionalLight
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.navigation.R
import com.mapbox.navigation.core.NavigationManager
import com.mapbox.navigation.data.FavoritesManager
import com.mapbox.navigation.databinding.ActivityMainBinding
import com.mapbox.navigation.ui.search.SearchResultsAdapter
import com.mapbox.search.MapboxSearchSdk
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchOptions
import com.mapbox.search.result.SearchResult

class MainActivity : AppCompatActivity(), PermissionsListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var searchAdapter: SearchResultsAdapter
    private lateinit var mapboxMap: MapboxMap
    private lateinit var favoritesManager: FavoritesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        navigationManager = NavigationManager(this)
        navigationManager.initialize()
        
        favoritesManager = FavoritesManager(this)
        
        initializeMap()
        setupSearchUI()
        setupClickListeners()
    }
    
    private fun initializeMap() {
        mapboxMap = binding.mapView.getMapboxMap()
        
        mapboxMap.loadStyleUri(NavigationManager.getNavigationMapStyle()) { style ->
            // Enable 3D buildings and terrain
            enable3DBuildings(style)
            
            // Map is ready
            if (PermissionsManager.areLocationPermissionsGranted(this)) {
                enableLocationComponent()
            } else {
                requestLocationPermission()
            }
        }
    }
    
    private fun enable3DBuildings(style: Style) {
        // Enable 3D buildings
        style.styleSourceProperty("composite", "building", true)
        style.styleSourceProperty("composite", "3d-buildings", true)
        
        // Add atmospheric effects
        style.atmosphere(
            atmosphere {
                color(listOf(0.8, 0.85, 0.9, 1.0))
                highColor(listOf(0.8, 0.85, 0.9, 1.0))
                horizonBlend(0.1)
                starIntensity(0.0)
            }
        )
        
        // Add sky layer for realistic sky rendering
        style.addLayer(
            skyLayer("sky") {
                skyType(com.mapbox.maps.extension.style.layers.properties.generated.SkyType.ATMOSPHERE)
                skyAtmosphereSun(listOf(-75.0, 75.0))
                skyAtmosphereSunIntensity(15.0)
            }
        )
        
        // Add lighting for 3D objects
        style.addLight(
            ambientLight("ambientLight") {
                color(listOf(1.0, 1.0, 1.0, 0.6))
            }
        )
        
        style.addLight(
            directionalLight("directionalLight") {
                color(listOf(1.0, 0.9, 0.7, 0.8))
                direction(listOf(-40.0, 40.0))
                castShadows(true)
            }
        )
        
        // Set camera pitch for 3D view
        binding.mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .pitch(45.0)
                .build()
        )
    }
    
    private fun setupSearchUI() {
        searchAdapter = SearchResultsAdapter(
            onItemClick = { searchResult ->
                handleSearchResultClick(searchResult)
            },
            onSavedLocationClick = { savedLocation ->
                handleSavedLocationClick(savedLocation)
            }
        )
        
        binding.searchResultsList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = searchAdapter
        }
        
        binding.searchInput.setOnEditorActionListener { textView, _, _ ->
            val query = textView.text.toString()
            if (query.isNotEmpty()) {
                performSearch(query)
                true
            } else {
                false
            }
        }
    }
    
    private fun performSearch(query: String) {
        val searchEngine = MapboxSearchSdk.createSearchEngine()
        
        searchEngine.search(
            query,
            SearchOptions.Builder()
                .limit(5)
                .build(),
            object : SearchCallback {
                override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                    runOnUiThread {
                        searchAdapter.updateResults(results, favoritesManager)
                    }
                }
                
                override fun onError(e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Search error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
    
    private fun handleSearchResultClick(searchResult: SearchResult) {
        // Hide search UI
        binding.searchContainer.visibility = View.GONE
        
        // Get coordinates from search result
        val destination = searchResult.coordinate?.let {
            Point.fromLngLat(it.longitude(), it.latitude())
        } ?: return
        
        // Add to recent locations
        favoritesManager.addRecentLocation(
            searchResult.name ?: searchResult.address?.formattedAddress() ?: "Unknown location",
            destination
        )
        
        // Start navigation
        startNavigationToDestination(destination, searchResult.name ?: "")
    }
    
    private fun handleSavedLocationClick(savedLocation: FavoritesManager.SavedLocation) {
        // Hide search UI
        binding.searchContainer.visibility = View.GONE
        
        // Get destination point
        val destination = savedLocation.toPoint()
        
        // Start navigation
        startNavigationToDestination(destination, savedLocation.name)
    }
    
    private fun startNavigationToDestination(destination: Point, destinationName: String) {
        // Get current location as origin
        val originLocation = binding.mapView.location.getLastKnownLocation()
        val origin = originLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return
        
        // Request route with traffic data
        navigationManager.requestRoutes(origin, destination) { routes ->
            if (routes != null && routes.isNotEmpty()) {
                // Start navigation with the first route
                navigationManager.startNavigation(routes.first())
                
                // Show navigation UI
                binding.navigationTopBar.visibility = View.VISIBLE
                binding.speedLimitContainer.visibility = View.VISIBLE
                binding.bottomNavigationBar.visibility = View.VISIBLE
                
                // Update UI with route information
                binding.streetName.text = destinationName
                binding.arrivalTime.text = "12:18 pm" // This would be calculated based on the route
                binding.tripDetails.text = "15 min · 2.6 mi" // This would be calculated based on the route
                
                // Set camera to follow the navigation path with 3D perspective
                binding.mapView.camera.easeTo(
                    CameraOptions.Builder()
                        .center(origin)
                        .zoom(18.0)
                        .pitch(60.0)
                        .bearing(routes.first().legs()?.firstOrNull()?.steps()?.firstOrNull()?.heading() ?: 0.0)
                        .build()
                )
            } else {
                Toast.makeText(this, "Could not find a route to the destination", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun enableLocationComponent() {
        binding.mapView.location.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
        
        // Center camera on user location with 3D perspective
        binding.mapView.location.addOnIndicatorPositionChangedListener { point ->
            binding.mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(18.0)
                    .pitch(45.0)
                    .build()
            )
            
            // Remove the listener after initial position is determined
            binding.mapView.location.removeOnIndicatorPositionChangedListener(this)
        }
    }
    
    private fun requestLocationPermission() {
        permissionsManager = PermissionsManager(this)
        permissionsManager.requestLocationPermissions(this)
    }
    
    private fun setupClickListeners() {
        binding.searchButton.setOnClickListener {
            binding.searchContainer.visibility = View.VISIBLE
            // Refresh search results with saved locations
            searchAdapter.updateResults(emptyList(), favoritesManager)
        }
        
        binding.closeButton.setOnClickListener {
            // Close navigation or search UI
            if (binding.searchContainer.visibility == View.VISIBLE) {
                binding.searchContainer.visibility = View.GONE
            } else {
                // Stop navigation if active
                navigationManager.stopNavigation()
                binding.navigationTopBar.visibility = View.GONE
                binding.speedLimitContainer.visibility = View.GONE
            }
        }
        
        binding.settingsButton.setOnClickListener {
            // Open settings
            Toast.makeText(this, "Settings not implemented yet", Toast.LENGTH_SHORT).show()
        }
        
        binding.alternativeRoutesButton.setOnClickListener {
            // Show alternative routes
            Toast.makeText(this, "Alternative routes not implemented yet", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    
    override fun onExplanationNeeded(permissionsToExplain: List<String>) {
        Toast.makeText(
            this,
            R.string.user_location_permission_explanation,
            Toast.LENGTH_LONG
        ).show()
    }
    
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            enableLocationComponent()
        } else {
            Toast.makeText(
                this,
                R.string.user_location_permission_not_granted,
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }
    
    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }
    
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
        navigationManager.onDestroy()
    }
}
