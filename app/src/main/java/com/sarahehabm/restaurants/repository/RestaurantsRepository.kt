package com.sarahehabm.restaurants.repository

import com.sarahehabm.restaurants.model.RestaurantsResponse
import retrofit2.Response

/*
* RestaurantsRepository class that is used by the ViewModel. It interacts with the
*  [RestaurantsNetwork] class to call the API
* */
class RestaurantsRepository(private val network: RestaurantsNetwork) {

    suspend fun getRestaurants(ll: String, sw: String, ne: String): Response<RestaurantsResponse> =
        network.getRestaurants(ll = ll, sw = sw, ne = ne)
}
