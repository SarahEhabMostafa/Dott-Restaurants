package com.sarahehabm.restaurants.view.map

import android.content.IntentSender
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
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
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.sarahehabm.restaurants.R
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.view.MainActivity
import com.sarahehabm.restaurants.viewmodel.MapViewModel

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    companion object {
        fun newInstance() = MapFragment()
    }

    private var viewModel: MapViewModel? = null
    private var googleMap: GoogleMap? = null
    private var buttonLocate: ImageButton? = null
    private var loader: ProgressBar? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 5000
        fastestInterval = 60000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.map_fragment, container, false)
        setHasOptionsMenu(true)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        viewModel = activity?.let { ViewModelProviders.of(it).get(MapViewModel::class.java) }

        var size: Int
        viewModel?.getRestaurants()?.observe(viewLifecycleOwner,
            Observer<ArrayList<Restaurant>> { value ->
                Log.v("RESPONSE", "Empty list? " + (value?.isEmpty() ?: "null"))
                size = value?.size ?: 0
                Toast.makeText(
                    context, "List received with size $size",
                    Toast.LENGTH_SHORT
                ).show()

                if(value != null) {
                    for (restaurant in value) {
                        val latLng = LatLng(
                            restaurant.location.lat.toDouble(),
                            restaurant.location.lng.toDouble()
                        )
                        val snippet = String.format(
                            "Lat: %1$.5f, Long: %2$.5f",
                            latLng.latitude,
                            latLng.longitude
                        )

                        val marker = googleMap?.addMarker(
                            MarkerOptions().position(latLng)
                        )

                        marker?.tag = restaurant
                    }
                }

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
                viewModel?.setLastLocation(locationResult.locations[0])

                zoomToLocation()
                showLoader()
            }
        }

        viewModel?.getLastLocation()?.observe(viewLifecycleOwner, Observer { t ->
            viewModel?.loadRestaurants("${t.latitude},${t.longitude}")
        })

        viewModel?.isLocationPermissionGranted()?.
            observe(viewLifecycleOwner, Observer { isGranted ->
                if(isGranted && googleMap!=null)
                    googleMap?.isMyLocationEnabled = true
            })

        viewModel?.isLocationSettingsEnabled()?.observe(viewLifecycleOwner, Observer { isEnabled ->
            if(!isEnabled) {
                hideLoader()
            }
        })

        return root
    }

    override fun onMapReady(map: GoogleMap) {
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true

        googleMap = map

        googleMap?.setOnMarkerClickListener(this)
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
                        MainActivity.requestCheckSettingsId
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun zoomToLocation() {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    viewModel?.getLastLocation()?.value?.latitude!!,
                    viewModel?.getLastLocation()?.value?.longitude!!
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
        buttonLocate?.setOnClickListener {
            if (viewModel?.isLocationPermissionGranted()?.value != true) {
                requestLocationPermission()
            } else {
                showLoader()
                initializeLocationServices()
            }
        }
        loader = menuLayout.findViewById(R.id.progressBar_loader)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity!!,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            MainActivity.requestPermissionId
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

    private fun showLoader() {
        loader?.visibility = View.VISIBLE
        buttonLocate?.visibility = View.GONE
    }

    private fun hideLoader() {
        loader?.visibility = View.GONE
        buttonLocate?.visibility = View.VISIBLE
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val restaurant : Restaurant = marker.tag as Restaurant
        viewModel?.setSelectedRestaurant(restaurant)

        return false
    }
}
