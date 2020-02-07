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

class MapViewModel(private val repository: RestaurantsRepository, ll: String, sw: String, ne: String) : ViewModel() {
    private var restaurantsList = MutableLiveData<ArrayList<Restaurant>>()

    private var _selectedRestaurant = MutableLiveData<Restaurant>()
    private var _location = MutableLiveData<Location>()
    private var _sw = MutableLiveData<Location>()
    private var _ne = MutableLiveData<Location>()
    private var _isLocationPermissionGranted = MutableLiveData<Boolean>()
    private var _isLocationSettingsEnabled = MutableLiveData<Boolean>()

    fun loadRestaurants(ll: String, sw: String, ne: String) {
        viewModelScope.launch {
            try {
                val res: Response<RestaurantsResponse> = repository.getRestaurants(ll, sw, ne)
                if (res.isSuccessful) {
                    val tmpList =  res.body()!!.response.venues
                    if(restaurantsList.value == null){
                        restaurantsList.value = tmpList
                    } else {
                        for (restaurant in tmpList) {
                            when(restaurantsList.value?.contains(restaurant)){
                                false -> restaurantsList.value!!.add(restaurant)
                            }
                        }
                    }
                    restaurantsList.postValue(restaurantsList.value)
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

    fun getLastLocation(): LiveData<Location> = _location
    fun setLastLocation(location: Location) {
        _location.value = location
    }

    fun getSW(): LiveData<Location> = _sw
    fun getSWString(): String = "${_sw.value?.latitude},${_sw.value?.longitude}"
    fun setSW(location: Location) {
        _sw.value = location
    }

    fun getNE(): LiveData<Location> = _ne
    fun getNEString(): String = "${_ne.value?.latitude},${_ne.value?.longitude}"
    fun setNE(location: Location) {
        _ne.value = location
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
