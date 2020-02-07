package com.sarahehabm.restaurants.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class Restaurant : ClusterItem {
    lateinit var id: String
    lateinit var name: String
    lateinit var location: Location
    internal var categories = ArrayList<Any>()
    lateinit var referralId: String
    var hasPerk: Boolean = false

    override fun getSnippet(): String {
        return name + id
    }

    override fun getTitle(): String {
        return name
    }

    override fun getPosition(): LatLng {
        return LatLng(location.lat.toDouble(), location.lng.toDouble())
    }

    fun getTag(): Restaurant {
        return this
    }
}

