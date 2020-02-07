package com.sarahehabm.restaurants.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.RestaurantsResponse
import kotlinx.coroutines.launch
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MapViewModel(
    private val repository: RestaurantsRepository,
    ll: String,
    sw: String,
    ne: String
) : ViewModel() {
    private var restaurantsList = MutableLiveData<ArrayList<Restaurant>>()
    private var _map = HashMap<String, Restaurant>()
    private var _errorMessage = MutableLiveData<String>()

    private var _selectedRestaurant = MutableLiveData<Restaurant>()
    private var _location = MutableLiveData<Location>()
    private var _sw = MutableLiveData<Location>()
    private var _ne = MutableLiveData<Location>()
    private var _isLocationPermissionGranted = MutableLiveData<Boolean>()
    private var _isLocationSettingsEnabled = MutableLiveData<Boolean>()
    private var _showLoader = MutableLiveData<Boolean>()

    fun loadRestaurants(ll: String, sw: String, ne: String) {
        _showLoader.value = true
        viewModelScope.launch {
            try {
                val res: Response<RestaurantsResponse> = repository.getRestaurants(ll, sw, ne)
                _showLoader.value = false
                if (res.isSuccessful) {
                    val tmpList = res.body()!!.response.venues
                    for (restaurant in tmpList) {
                        if (!_map.containsKey(restaurant.id)) {
                            _map[restaurant.id] = restaurant
                        }
                    }

                    restaurantsList.postValue(ArrayList(_map.values))
                } else {
                    val errorResponse =
                        Gson().fromJson(res.errorBody()?.string(), RestaurantsResponse::class.java)
                    _errorMessage.postValue(errorResponse.meta.errorDetail)
                }
            } catch (e: Exception) {
                when (e) {
                    is UnknownHostException -> _errorMessage.postValue("-1")
                    is SocketTimeoutException -> _errorMessage.postValue("-1")
                    else -> {
                        _errorMessage.postValue(null)
                    }
                }

                _showLoader.value = false
            }
        }
    }

    fun getRestaurants(): LiveData<ArrayList<Restaurant>> {
        return restaurantsList
    }

    fun getError(): LiveData<String> {
        return _errorMessage
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

    fun isShowLoader(): LiveData<Boolean> = _showLoader
    fun setShowLoader(isShow: Boolean) {
        _showLoader.value = isShow
    }
}
