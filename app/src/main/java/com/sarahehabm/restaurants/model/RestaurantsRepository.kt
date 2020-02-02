package com.sarahehabm.restaurants.model

import retrofit2.Response

class RestaurantsRepository(private val network: RestaurantsNetwork) {

    suspend fun getRestaurants() : Response<RestaurantsResponse>
            = network.getRestaurants(ll="30.014702,31.322651")
}
