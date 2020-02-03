package com.sarahehabm.restaurants.model

import retrofit2.Response

class RestaurantsRepository(private val network: RestaurantsNetwork) {

    suspend fun getRestaurants(ll:String) : Response<RestaurantsResponse>
            = network.getRestaurants(ll=ll)
}
