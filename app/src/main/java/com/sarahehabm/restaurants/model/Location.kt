package com.sarahehabm.restaurants.model

class Location {
    lateinit var address: String
    lateinit var crossStreet: String
    var lat: Float = 0.toFloat()
    var lng: Float = 0.toFloat()
    internal var labeledLatLngs = ArrayList<Any>()
    var distance: Float = 0.toFloat()
    lateinit var cc: String
    lateinit var city: String
    lateinit var state: String
    lateinit var country: String
    internal var formattedAddress = ArrayList<Any>()
}