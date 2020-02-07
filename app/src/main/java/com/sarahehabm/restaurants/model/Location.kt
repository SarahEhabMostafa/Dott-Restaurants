package com.sarahehabm.restaurants.model

class Location {
    var address: String? = ""
    var crossStreet: String? = ""
    var lat: Float = 0.toFloat()
    var lng: Float = 0.toFloat()
    internal var labeledLatLngs = ArrayList<Any>()
    var distance: Float = 0.toFloat()
    var cc: String? = ""
    var city: String? = ""
    var state: String? = ""
    var country: String? = ""
    internal var formattedAddress = ArrayList<Any>()
}