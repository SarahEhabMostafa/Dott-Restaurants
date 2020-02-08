package com.sarahehabm.restaurants.repository

import com.sarahehabm.restaurants.model.RestaurantsResponse
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

/*
* [RestaurantsNetwork] object
* */
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

/*
* Get method for the [RestaurantsNetwork] service object
* */
fun getNetworkService() = service

/*
* Interface class containing the APIs
* */
interface RestaurantsNetwork {

    /*
    * API call to get the restaurants. You only need to provide the ll, sw & ne values
    *
    * @param ll specifies the latitude and longitude
    * @param sw specifies the south west point. This parameter combined with the ne parameter
    *           draw a boundary square to retrieve all the restaurants within this square
    * @param ne specifies the north east point
    * @param intent this is an optional parameter that specifies the intent from using the API
    * @param client_id this is an optional parameter that specifies the client_id needed for the API
    * @param client_secret this is an optional parameter that specifies the client_secret needed for the API
    * @param version this is an optional parameter that indicates that the API is targeting
    *           and handles all the changes up to and including this date
    * @param limit this is an optional parameter that specifies the number of results per API call
    * @param categoryId this is an optional parameter that defines the id of the category to be retrieved
    *
    * Refer to https://developer.foursquare.com/docs/api/venues/search for more documentation about the API,
    * */
    @GET("venues/search")
    suspend fun getRestaurants(
        @Query("ll") ll: String,
        @Query("sw") sw: String,
        @Query("ne") ne: String,
        @Query("intent") intent: String = SEARCH_INTENT,
        @Query("client_id") clientId: String = CLIENT_ID,
        @Query("client_secret") clientSecret: String = CLIENT_SECRET,
        @Query("v") version: String = VERSION,
        @Query("limit") limit: String = LIMIT,
        @Query("categoryId") categoryId: String = CATEGORY_ID
    ): Response<RestaurantsResponse>
}