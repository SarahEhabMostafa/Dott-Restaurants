package com.sarahehabm.restaurants.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.RestaurantsResponse
import kotlinx.coroutines.launch
import retrofit2.Response

class MapViewModel(private val repository: RestaurantsRepository, ll: String) : ViewModel() {
    private var restaurantsList = MutableLiveData<ArrayList<Restaurant>>()

    private var _selectedRestaurant = MutableLiveData<Restaurant>()
    private var _lastLocation = MutableLiveData<Location>()
    private var _isLocationPermissionGranted = MutableLiveData<Boolean>()
    private var _isLocationSettingsEnabled = MutableLiveData<Boolean>()

    init {
        loadRestaurants(ll)
    }

    fun loadRestaurants(ll: String) {
        viewModelScope.launch {
            try {
                val res: Response<RestaurantsResponse> = repository.getRestaurants(ll)
                if (res.isSuccessful) {
                    restaurantsList.postValue(res.body()!!.response.venues)
                } else {
                    //TODO handle failure
                    restaurantsList.postValue(restaurantsList.value)
                }
            } catch (e: Exception) {
                //TODO handle failure
                restaurantsList.postValue(restaurantsList.value)
            }
        }
    }

    fun getRestaurants(): LiveData<ArrayList<Restaurant>> {
        return restaurantsList
    }

    fun getLastLocation(): LiveData<Location> = _lastLocation
    fun setLastLocation(location: Location) {
        _lastLocation.value = location
    }

    fun getSelectedRestaurant(): LiveData<Restaurant> = _selectedRestaurant
    fun setSelectedRestaurant(restaurant: Restaurant) {
        _selectedRestaurant.value = restaurant
    }

    fun isLocationPermissionGranted(): LiveData<Boolean> = _isLocationPermissionGranted
    fun setLocationPermissionGranted(isGranted: Boolean) {
        _isLocationPermissionGranted.value = isGranted
    }

    fun isLocationSettingsEnabled(): LiveData<Boolean> = _isLocationSettingsEnabled
    fun setLocationSettingsEnabled(isEnabled: Boolean) {
        _isLocationSettingsEnabled.value = isEnabled
    }
}
