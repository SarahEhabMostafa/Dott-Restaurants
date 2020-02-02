package com.sarahehabm.restaurants.model

class Restaurant {

    lateinit var id: String
    lateinit var name: String
    lateinit var location: Location
    internal var categories = ArrayList<Any>()
    lateinit var referralId: String
    var hasPerk: Boolean = false
}

