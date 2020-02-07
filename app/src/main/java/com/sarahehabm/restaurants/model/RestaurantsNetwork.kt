package com.sarahehabm.restaurants.model

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val SEARCH_INTENT = "browse"
const val CLIENT_ID = "XLOLYTGNGMOITN2BNJSOH4XWGF4ETG3HTSIXBXQDBBBOEJXN"
const val CLIENT_SECRET = "0OFBFJTGNR35AK1SC0G4JRPM5BPNJSGEW25T0E4Q20OWHXYQ"
const val VERSION = "20200201"
const val LIMIT = "50"
const val CATEGORY_ID = "4d4b7105d754a06374d81259"

private val service: RestaurantsNetwork by lazy {

    val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
        this.level = HttpLoggingInterceptor.Level.BODY
    }
    val okHttpClient = OkHttpClient.Builder().apply {
        this.addInterceptor(interceptor)
    }.build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.foursquare.com/v2/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    retrofit.create(RestaurantsNetwork::class.java)
}

fun getNetworkService() = service

interface RestaurantsNetwork {
    @GET("venues/search")
    suspend fun getRestaurants(
        @Query("ll") ll: String,
        @Query("sw") sw: String,
        @Query("ne") ne: String,
        @Query("intent") intent: String = SEARCH_INTENT,
        @Query("client_id") clientId: String = CLIENT_ID,
        @Query("client_secret") clientSecret: String = CLIENT_SECRET,
        @Query("v") v: String = VERSION,
        @Query("limit") limit: String = LIMIT,
        @Query("categoryId") categoryId: String = CATEGORY_ID
    ): Response<RestaurantsResponse>
}