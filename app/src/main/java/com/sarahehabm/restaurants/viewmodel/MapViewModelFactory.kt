package com.sarahehabm.restaurants.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sarahehabm.restaurants.model.RestaurantsRepository

class MapViewModelFactory(private val repository: RestaurantsRepository, private val ll: String="") : //"30.0444,31.2357"
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(repository, ll) as T
        }

        throw IllegalArgumentException("Unknown ViewModel type")
    }
}