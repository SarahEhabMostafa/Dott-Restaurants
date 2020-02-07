package com.sarahehabm.restaurants.view.map

import android.content.IntentSender
import android.location.Location
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
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.sarahehabm.restaurants.R
import com.sarahehabm.restaurants.model.Restaurant
import com.sarahehabm.restaurants.view.MainActivity
import com.sarahehabm.restaurants.viewmodel.MapViewModel
import kotlinx.android.synthetic.main.map_fragment.*
import kotlin.math.floor

class MapFragment : Fragment(), OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener, ClusterManager.OnClusterItemClickListener<Restaurant>,
    ClusterManager.OnClusterClickListener<Restaurant> {

    companion object {
        fun newInstance() = MapFragment()
    }

    private var viewModel: MapViewModel? = null
    private var googleMap: GoogleMap? = null
    private var buttonLocate: ImageButton? = null
    private var loader: ProgressBar? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var clusterManager: ClusterManager<Restaurant>
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
                Log.v("RESPONSE", "List received with size $size")

                if(value != null) {
                    clusterManager.addItems(value)
                }

                clusterManager.cluster()
            }
        )

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                zoomToLocation(locationResult.locations[0].latitude, locationResult.locations[0].longitude)
                viewModel?.setShowLoader(true)
            }
        }

        viewModel?.getLastLocation()?.observe(viewLifecycleOwner, Observer { t ->
            viewModel?.loadRestaurants(
                "${t.latitude},${t.longitude}",
                sw = viewModel?.getSWString()!!,
                ne = viewModel?.getNEString()!!
            )
        })

        viewModel?.isLocationPermissionGranted()?.
            observe(viewLifecycleOwner, Observer { isGranted ->
                if(isGranted && googleMap!=null)
                    googleMap?.isMyLocationEnabled = true
            })

        viewModel?.isLocationSettingsEnabled()?.observe(viewLifecycleOwner, Observer { isEnabled ->
            if(!isEnabled) {
                viewModel?.setShowLoader(false)
            }
        })

        viewModel?.isShowLoader()?.observe(viewLifecycleOwner, Observer { isShow ->
            if(isShow)
                showLoader()
            else
                hideLoader()
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
        setupClusterManager()

        googleMap?.setOnCameraIdleListener(this)
        googleMap?.setOnMarkerClickListener(clusterManager)
    }

    private fun setupClusterManager() {
        clusterManager = ClusterManager(activity, googleMap)
        clusterManager.setAnimation(true)

        clusterManager.setOnClusterItemClickListener(this)
        clusterManager.setOnClusterClickListener(this)
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
                    Snackbar.make(map, R.string.unknown_error, Snackbar.LENGTH_SHORT).show()
                }
            } else {
                Snackbar.make(map, R.string.unknown_error, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun zoomToLocation(lat: Double, lng: Double) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(lat, lng
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

    override fun onClusterItemClick(restaurant: Restaurant): Boolean {
        viewModel?.setSelectedRestaurant(restaurant)

        return true
    }

    override fun onClusterClick(cluster: Cluster<Restaurant>): Boolean {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                cluster.position,
                floor((googleMap?.cameraPosition?.zoom?.plus(1)!!))
            ), 300, null
        )

        return true
    }

    override fun onCameraIdle() {
        Log.i("MAP", "Map is now settled with a center of " + googleMap?.cameraPosition?.target)

        val loc = Location("")
        loc.latitude = googleMap?.cameraPosition?.target?.latitude!!
        loc.longitude = googleMap?.cameraPosition?.target?.longitude!!

        val sw = Location("")
        sw.latitude = googleMap?.projection?.visibleRegion?.latLngBounds?.southwest?.latitude!!
        sw.longitude = googleMap?.projection?.visibleRegion?.latLngBounds?.southwest?.longitude!!


        val ne = Location("")
        ne.latitude = googleMap?.projection?.visibleRegion?.latLngBounds?.northeast?.latitude!!
        ne.longitude = googleMap?.projection?.visibleRegion?.latLngBounds?.northeast?.longitude!!

        viewModel?.setSW(sw)
        viewModel?.setNE(ne)
        viewModel?.setLastLocation(loc)
        viewModel?.setShowLoader(true)

        clusterManager.onCameraIdle()
    }
}
