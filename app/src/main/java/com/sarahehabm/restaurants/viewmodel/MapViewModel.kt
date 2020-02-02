package com.sarahehabm.restaurants.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.RestaurantsResponse
import kotlinx.coroutines.launch
import retrofit2.Response

class MapViewModel(private val repository: RestaurantsRepository) : ViewModel() {
    private val _restaurantsList = MutableLiveData<ArrayList<Restaurant>>()
    val restaurants: LiveData<ArrayList<Restaurant>>
        get() = _restaurantsList

    fun getRestaurants() {
        viewModelScope.launch {
            try {
                val res: Response<RestaurantsResponse> = repository.getRestaurants()
                if (res.isSuccessful) {
                    _restaurantsList.postValue(res.body()!!.response.venues)
                } else {
                    //TODO handle failure
                    _restaurantsList.postValue(null)
                }
            } catch (e: Exception) {
                //TODO handle failure
                _restaurantsList.postValue(null)
            }
        }
    }
}
