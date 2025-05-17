package com.mapbox.navigation.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.R
import com.mapbox.navigation.data.FavoritesManager
import com.mapbox.search.result.SearchResult

class SearchResultsAdapter(
    private val onItemClick: (SearchResult) -> Unit,
    private val onSavedLocationClick: (FavoritesManager.SavedLocation) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.ViewHolder>() {

    private val items = mutableListOf<SearchItem>()

    fun updateResults(results: List<SearchResult>, favorites: FavoritesManager) {
        items.clear()
        
        // Add home and work at the top if they exist
        val homeLocation = favorites.getHomeLocation()
        if (homeLocation != null) {
            items.add(SearchItem.Header("Saved Places"))
            items.add(SearchItem.SavedLocation("Home", R.drawable.ic_home, homeLocation))
        }
        
        val workLocation = favorites.getWorkLocation()
        if (workLocation != null) {
            if (items.isEmpty()) {
                items.add(SearchItem.Header("Saved Places"))
            }
            items.add(SearchItem.SavedLocation("Work", R.drawable.ic_work, workLocation))
        }
        
        // Add favorites
        val favoriteLocations = favorites.getFavoriteLocations()
        if (favoriteLocations.isNotEmpty()) {
            if (items.isEmpty()) {
                items.add(SearchItem.Header("Saved Places"))
            }
            favoriteLocations.forEach { location ->
                items.add(SearchItem.SavedLocation(location.name, R.drawable.ic_favorite, location))
            }
        }
        
        // Add recent locations
        val recentLocations = favorites.getRecentLocations()
        if (recentLocations.isNotEmpty()) {
            items.add(SearchItem.Header("Recent"))
            recentLocations.forEach { location ->
                items.add(SearchItem.SavedLocation(location.name, R.drawable.ic_recent, location))
            }
        }
        
        // Add search results
        if (results.isNotEmpty()) {
            items.add(SearchItem.Header("Results"))
            results.forEach { result ->
                items.add(SearchItem.Result(result))
            }
        }
        
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_SAVED_LOCATION -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_favorite, parent, false)
                SavedLocationViewHolder(view, onSavedLocationClick)
            }
            VIEW_TYPE_RESULT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_search_result, parent, false)
                ResultViewHolder(view, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SearchItem.Header -> (holder as HeaderViewHolder).bind(item)
            is SearchItem.SavedLocation -> (holder as SavedLocationViewHolder).bind(item)
            is SearchItem.Result -> (holder as ResultViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is SearchItem.Header -> VIEW_TYPE_HEADER
            is SearchItem.SavedLocation -> VIEW_TYPE_SAVED_LOCATION
            is SearchItem.Result -> VIEW_TYPE_RESULT
        }
    }

    abstract class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class HeaderViewHolder(itemView: View) : ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.headerTitle)

        fun bind(item: SearchItem.Header) {
            titleTextView.text = item.title
        }
    }

    class SavedLocationViewHolder(
        itemView: View,
        private val onItemClick: (FavoritesManager.SavedLocation) -> Unit
    ) : ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.favoriteIcon)
        private val nameTextView: TextView = itemView.findViewById(R.id.favoriteName)

        fun bind(item: SearchItem.SavedLocation) {
            iconImageView.setImageResource(item.iconResId)
            nameTextView.text = item.name
            
            itemView.setOnClickListener {
                onItemClick(item.location)
            }
        }
    }

    class ResultViewHolder(
        itemView: View,
        private val onItemClick: (SearchResult) -> Unit
    ) : ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.resultName)
        private val addressTextView: TextView = itemView.findViewById(R.id.resultAddress)

        fun bind(item: SearchItem.Result) {
            nameTextView.text = item.result.name
            addressTextView.text = item.result.address?.formattedAddress() ?: ""
            
            itemView.setOnClickListener {
                onItemClick(item.result)
            }
        }
    }

    sealed class SearchItem {
        data class Header(val title: String) : SearchItem()
        data class SavedLocation(val name: String, val iconResId: Int, val location: FavoritesManager.SavedLocation) : SearchItem()
        data class Result(val result: SearchResult) : SearchItem()
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_SAVED_LOCATION = 1
        private const val VIEW_TYPE_RESULT = 2
    }
}
