package com.sarahehabm.restaurants.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sarahehabm.restaurants.repository.RestaurantsRepository

/*
* ViewModelProvider factory to generate objects from the [MapViewModel] class
* */
class MapViewModelFactory(private val repository: RestaurantsRepository) :
    ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel type")
    }
}