package com.sarahehabm.restaurants.view.map

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.sarahehabm.restaurants.model.Restaurant

class RestaurantsClusterRenderer(
    private val context: Context,
    map: GoogleMap?,
    clusterManager: ClusterManager<Restaurant>?
) : DefaultClusterRenderer<Restaurant>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: Restaurant?, markerOptions: MarkerOptions?) {
        super.onBeforeClusterItemRendered(item, markerOptions)

        val markerIcon = BitmapFactory.decodeResource(
            context.resources,
            com.sarahehabm.restaurants.R.drawable.ic_marker
        )
        markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(markerIcon))
    }

    override fun getColor(clusterSize: Int): Int {
        return Color.BLACK
    }
}