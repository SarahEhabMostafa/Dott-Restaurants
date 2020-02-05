package com.sarahehabm.restaurants.view.map

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
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
    private lateinit var buttonLocate: ImageButton
    private lateinit var loader: ProgressBar
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 60000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private val requestLocationId = 12
    private val requestCheckSettingsId = 13

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

                hideLoader()
            }
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                for (location in locationResult.locations) {
                    Toast.makeText(
                        context!!,
                        "Received " + locationResult.locations.size + " locations",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                lastLocation = locationResult.locations[0]

                zoomToLocation()
                showLoader()
                viewModel.loadRestaurants("${lastLocation?.latitude},${lastLocation?.longitude}")
            }
        }

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
    }

    private fun initializeLocationServices() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(context!!)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // All location settings are satisfied
            Toast.makeText(context, getString(R.string.getting_location), Toast.LENGTH_LONG).show()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied; show dialog to the user to change settings
                try {
                    exception.startResolutionForResult(
                        activity,
                        requestCheckSettingsId
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun zoomToLocation() {
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    lastLocation!!.latitude,
                    lastLocation!!.longitude
                ), 15f
            )
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        val menuItem = menu.findItem(R.id.action_locate)
        val menuLayout = menuItem.actionView

        buttonLocate = menuLayout.findViewById(R.id.button_locate)
        buttonLocate.setOnClickListener {
            if (!isLocationPermissionGranted()) {
                requestLocationPermission()
            } else {
                showLoader()
                initializeLocationServices()
            }
        }
        loader = menuLayout.findViewById(R.id.progressBar_loader)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context!!,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity!!,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            requestLocationId
        )
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestCheckSettingsId) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(context, getString(R.string.locate_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoader() {
        loader.visibility = View.VISIBLE
        buttonLocate.visibility = View.GONE
    }

    private fun hideLoader() {
        loader.visibility = View.GONE
        buttonLocate.visibility = View.VISIBLE
    }
}
