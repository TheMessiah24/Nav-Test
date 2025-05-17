package com.mapbox.navigation.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mapbox.geojson.Point

class FavoritesManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    // Home location
    fun saveHomeLocation(name: String, point: Point) {
        val location = SavedLocation(name, point.longitude(), point.latitude())
        val json = gson.toJson(location)
        sharedPreferences.edit().putString(KEY_HOME, json).apply()
    }
    
    fun getHomeLocation(): SavedLocation? {
        val json = sharedPreferences.getString(KEY_HOME, null) ?: return null
        return gson.fromJson(json, SavedLocation::class.java)
    }
    
    // Work location
    fun saveWorkLocation(name: String, point: Point) {
        val location = SavedLocation(name, point.longitude(), point.latitude())
        val json = gson.toJson(location)
        sharedPreferences.edit().putString(KEY_WORK, json).apply()
    }
    
    fun getWorkLocation(): SavedLocation? {
        val json = sharedPreferences.getString(KEY_WORK, null) ?: return null
        return gson.fromJson(json, SavedLocation::class.java)
    }
    
    // Favorite locations
    fun saveFavoriteLocation(name: String, point: Point) {
        val location = SavedLocation(name, point.longitude(), point.latitude())
        val favorites = getFavoriteLocations().toMutableList()
        
        // Check if location with same name exists and update it
        val existingIndex = favorites.indexOfFirst { it.name == name }
        if (existingIndex >= 0) {
            favorites[existingIndex] = location
        } else {
            favorites.add(location)
        }
        
        val json = gson.toJson(favorites)
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply()
    }
    
    fun getFavoriteLocations(): List<SavedLocation> {
        val json = sharedPreferences.getString(KEY_FAVORITES, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedLocation>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun removeFavoriteLocation(name: String) {
        val favorites = getFavoriteLocations().toMutableList()
        favorites.removeAll { it.name == name }
        val json = gson.toJson(favorites)
        sharedPreferences.edit().putString(KEY_FAVORITES, json).apply()
    }
    
    // Recent locations
    fun addRecentLocation(name: String, point: Point) {
        val location = SavedLocation(name, point.longitude(), point.latitude())
        val recents = getRecentLocations().toMutableList()
        
        // Remove if already exists to avoid duplicates
        recents.removeAll { it.name == name }
        
        // Add to beginning of list
        recents.add(0, location)
        
        // Limit to max recent locations
        if (recents.size > MAX_RECENT_LOCATIONS) {
            recents.removeAt(recents.size - 1)
        }
        
        val json = gson.toJson(recents)
        sharedPreferences.edit().putString(KEY_RECENTS, json).apply()
    }
    
    fun getRecentLocations(): List<SavedLocation> {
        val json = sharedPreferences.getString(KEY_RECENTS, null) ?: return emptyList()
        val type = object : TypeToken<List<SavedLocation>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    
    fun clearRecentLocations() {
        sharedPreferences.edit().remove(KEY_RECENTS).apply()
    }
    
    companion object {
        private const val PREFERENCES_NAME = "mapbox_navigation_favorites"
        private const val KEY_HOME = "home_location"
        private const val KEY_WORK = "work_location"
        private const val KEY_FAVORITES = "favorite_locations"
        private const val KEY_RECENTS = "recent_locations"
        private const val MAX_RECENT_LOCATIONS = 10
    }
    
    data class SavedLocation(
        val name: String,
        val longitude: Double,
        val latitude: Double
    ) {
        fun toPoint(): Point = Point.fromLngLat(longitude, latitude)
    }
}
