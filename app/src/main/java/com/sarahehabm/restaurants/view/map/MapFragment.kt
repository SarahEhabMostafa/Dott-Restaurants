package com.sarahehabm.restaurants.view.map

import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.getNetworkService
import com.sarahehabm.restaurants.viewmodel.MapViewModel
import kotlinx.android.synthetic.main.map_fragment.view.*

class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        fun newInstance() = MapFragment()
    }

    private lateinit var viewModel: MapViewModel
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view =
            inflater.inflate(com.sarahehabm.restaurants.R.layout.map_fragment, container, false)
        val mapFragment = childFragmentManager
            .findFragmentById(com.sarahehabm.restaurants.R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        view.button_show_restaurants.setOnClickListener { Log.v("Location", if(lastLocation == null) "NULL" else lastLocation.toString()) }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val repo = RestaurantsRepository(getNetworkService())
        viewModel = MapViewModel(repo)
        viewModel.restaurants.observe(activity as LifecycleOwner,
            Observer<ArrayList<Restaurant>> { value ->
                Log.v("RESPONSE", "Size= " + value.isEmpty())
                Toast.makeText(
                    context, "List received with size ${value.size}",
                    Toast.LENGTH_LONG
                ).show()
            }
        )

        viewModel.getRestaurants()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.isMyLocationEnabled = true

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if(location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }
    }
}
