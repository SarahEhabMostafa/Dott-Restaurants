package com.sarahehabm.restaurants.view.map

import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.sarahehabm.restaurants.R
import com.sarahehabm.restaurants.databinding.MapFragmentBinding
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.getNetworkService
import com.sarahehabm.restaurants.viewmodel.MapViewModel
import com.sarahehabm.restaurants.viewmodel.MapViewModelFactory

class MapFragment : Fragment(), OnMapReadyCallback {

    companion object {
        fun newInstance() = MapFragment()
    }

    private lateinit var binding: MapFragmentBinding
    private lateinit var viewModel: MapViewModel
    private lateinit var viewModelFactory: MapViewModelFactory
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private var isPermissionGranted: Boolean = false

    private val requestId = 12

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil
            .inflate(inflater, R.layout.map_fragment, container, false)
        setHasOptionsMenu(true)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val repository = RestaurantsRepository(getNetworkService())
        viewModelFactory = MapViewModelFactory(repository)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(MapViewModel::class.java)

        binding.mapViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        var size: Int
        binding.mapViewModel?.getRestaurants()?.observe(viewLifecycleOwner,
            Observer<ArrayList<Restaurant>> { value ->
                Log.v("RESPONSE", "Empty list? " + (value?.isEmpty() ?: "null"))
                size = value?.size ?: 0
                Toast.makeText(
                    context, "List received with size $size",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
    }

    private fun zoomToLocation() {
        val locationResult = fusedLocationClient.lastLocation
        locationResult.addOnCompleteListener(activity as Activity) { task ->
            if (task.isSuccessful && task.result != null) {
                lastLocation = task.result
                viewModel.loadRestaurants("${lastLocation?.latitude},${lastLocation?.longitude}")

                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            lastLocation!!.latitude,
                            lastLocation!!.longitude
                        ), 15f
                    )
                )
            } else {
                Toast.makeText(context, "Failed to get the location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.action_locate) {
            if (!isPermissionGranted) {
                getLocationPermission()
            } else {
                Toast.makeText(context, "Locate me", Toast.LENGTH_SHORT).show()

                zoomToLocation()
                //TODO should display restaurants now
            }
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun getLocationPermission() {
        isPermissionGranted = false
        if (ContextCompat.checkSelfPermission(
                context!!,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            isPermissionGranted = true
        else {
            ActivityCompat.requestPermissions(
                activity!!,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                requestId
            )
        }
    }
}
