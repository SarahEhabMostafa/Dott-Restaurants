package com.sarahehabm.restaurants.viewmodel

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.model.RestaurantsResponse
import com.sarahehabm.restaurants.repository.RestaurantsRepository
import kotlinx.coroutines.launch
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MapViewModel(
    private val repository: RestaurantsRepository
) : ViewModel() {
    //Declare variables
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

    /*
    * A function that fetches the restaurants from the [RestaurantsRepository]
    *
    * @param ll Value defining the latitude and longitude of the point
    * @param sw Value defining the lat and long of the south west corner
    * @param ne Value defining the lat and long of the north east corner
    *
    * All three parameters should be provided in the following format: lat,lng
    * */
    fun loadRestaurants(ll: String, sw: String, ne: String) {
        //Setting the value of the [_showLoader] parameter to true to signal to the views
        // to start indicating a loader to the user
        _showLoader.value = true
        //Calling the repository suspend function from a coroutine
        viewModelScope.launch {
            try {
                //Initiating a response variable with the API response
                val res: Response<RestaurantsResponse> = repository.getRestaurants(ll, sw, ne)
                //Setting the value of the [_showLoader] parameter to false to signal to the views
                // to stop indicating a loader to the user
                _showLoader.value = false
                if (res.isSuccessful) { //Response is successful
                    val tmpList = res.body()!!.response.venues
                    for (restaurant in tmpList) {
                        if (!_map.containsKey(restaurant.id)) {
                            _map[restaurant.id] = restaurant
                        }
                    }

                    restaurantsList.postValue(ArrayList(_map.values))
                } else { //Response failed for some reason
                    //Get the error response JSON value and parse it to get the error
                    // message provided from the API
                    val errorResponse =
                        Gson().fromJson(res.errorBody()?.string(), RestaurantsResponse::class.java)
                    //Broadcast the error message received
                    _errorMessage.postValue(errorResponse.meta.errorDetail)
                }
            } catch (e: Exception) { //The request failed for a connection or other reasons
                when (e) {
                    //Connection failure cases; post a different value to be parsed
                    // for a specific error message by the view
                    is UnknownHostException -> _errorMessage.postValue("-1")
                    is SocketTimeoutException -> _errorMessage.postValue("-1")

                    //Error was que to something else; post a null value to be parsed
                    // for a generic error message by the view
                    else -> {
                        _errorMessage.postValue(null)
                    }
                }

                //Setting the value of the [_showLoader] parameter to false to signal to the views
                // to stop indicating a loader to the user
                _showLoader.value = false
            }
        }
    }

    /*
    * Get function that returns the live data [Restaurant] object
    * */
    fun getRestaurants(): LiveData<ArrayList<Restaurant>> {
        return restaurantsList
    }

    /*
    * Set function that returns sets live data [Restaurant] object
    * */
    fun setRestaurants(list: ArrayList<Restaurant>) {
        restaurantsList = MutableLiveData(list)
    }

    /*
    * Get function that returns the live data "Error Message" value
    * */
    fun getError(): LiveData<String> {
        return _errorMessage
    }

    /*
    * Get function that returns the value of the live data [_location] object
    * */
    fun getLastLocation(): LiveData<Location> = _location

    /*
    * Set function that sets the value of the live data [_location] object
    * */
    fun setLastLocation(location: Location) {
        _location.value = location
    }

    /*
    * Get function that returns the value of the live data [_sw] object
    * as a String in the format of latitude,longitude
    * */
    fun getSWString(): String = "${_sw.value?.latitude},${_sw.value?.longitude}"

    /*
    * Set function that sets the value of the live data [_sw] object
    * */
    fun setSW(location: Location) {
        _sw.value = location
    }

    /*
    * Get function that returns the value of the live data [_ne] object
    * as a String in the format of latitude,longitude
    * */
    fun getNEString(): String = "${_ne.value?.latitude},${_ne.value?.longitude}"

    /*
    * Set function that sets the value of the live data [_ne] object
    * */
    fun setNE(location: Location) {
        _ne.value = location
    }

    /*
    * Get function that returns the value of the live data [_selectedRestaurant] object
    * */
    fun getSelectedRestaurant(): LiveData<Restaurant> = _selectedRestaurant

    /*
    * Set function that sets the value of the live data [_selectedRestaurant] object
    * */
    fun setSelectedRestaurant(restaurant: Restaurant) {
        _selectedRestaurant.value = restaurant
    }

    /*
    * Get function that returns the value of the live data [_isLocationPermissionGranted] object
    * */
    fun isLocationPermissionGranted(): LiveData<Boolean> = _isLocationPermissionGranted

    /*
    * Set function that sets the value of the live data [_isLocationPermissionGranted] object
    * */
    fun setLocationPermissionGranted(isGranted: Boolean) {
        _isLocationPermissionGranted.value = isGranted
    }

    /*
    * Get function that returns the value of the live data [_isLocationSettingsEnabled] object
    * */
    fun isLocationSettingsEnabled(): LiveData<Boolean> = _isLocationSettingsEnabled

    /*
    * Set function that sets the value of the live data [_isLocationSettingsEnabled] object
    * */
    fun setLocationSettingsEnabled(isEnabled: Boolean) {
        _isLocationSettingsEnabled.value = isEnabled
    }

    /*
    * Get function that returns the value of the live data [_showLoader] object
    * */
    fun isShowLoader(): LiveData<Boolean> = _showLoader

    /*
    * Set function that sets the value of the live data [_showLoader] object
    * */
    fun setShowLoader(isShow: Boolean) {
        _showLoader.value = isShow
    }
}
