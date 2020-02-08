package com.sarahehabm.restaurants.view.map

import android.content.IntentSender
import android.location.Location
import android.os.Bundle
import android.os.Looper
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

/*
* The MapFragment class. This class holds the mapFragment view and initialized it using
* a clusterManager. It also handles the location changes to update them on the map.
* */
class MapFragment : Fragment(), OnMapReadyCallback,
    GoogleMap.OnCameraIdleListener, ClusterManager.OnClusterItemClickListener<Restaurant>,
    ClusterManager.OnClusterClickListener<Restaurant> {

    companion object {
        fun newInstance() = MapFragment()
    }

    //Declaring the views
    private var googleMap: GoogleMap? = null
    private var buttonLocate: ImageButton? = null
    private var loader: ProgressBar? = null

    //Declaring all other variables
    private var viewModel: MapViewModel? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var clusterManager: ClusterManager<Restaurant>? = null
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 300000 //The interval is set to 5 minutes as there's no need
        // to have the updates faster than that
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY //Accuracy is set to the highest accuracy
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = inflater.inflate(R.layout.map_fragment, container, false)
        setHasOptionsMenu(true)

        //Initializing the mapFragment anf preparing the map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Initializing the viewModel object from the activity
        viewModel = activity?.let { ViewModelProviders.of(it).get(MapViewModel::class.java) }

        //Initializing the location specific objects
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return

                //When a new location is received, zoom to the new location. Based on this zoom
                // and the fact that, respectively, the center of the map will
                // change, hence a new API request will be fired
                zoomToLocation(
                    locationResult.locations[0].latitude,
                    locationResult.locations[0].longitude
                )
                //Updating the showLoader variable to be true
                viewModel?.setShowLoader(true)
            }
        }

        //Setting all the viewModel observers
        setObservers()

        return root
    }

    /*
    * A function that sets all the needed viewModel observers
    * */
    private fun setObservers() {
        //Setting the restaurants observer. When new values are received, they're added
        // to the cluster manager (if it was already initialize)
        viewModel?.getRestaurants()?.observe(viewLifecycleOwner,
            Observer<ArrayList<Restaurant>> { value ->
                if (clusterManager != null) {
                    if (value != null) {
                        clusterManager!!.addItems(value)
                    }

                    clusterManager!!.cluster()
                }
            }
        )

        //Setting the last location observer. This variable is updated when the user
        // location is changed. An API call is fired to get the restaurants
        // surrounding the user's new location
        viewModel?.getLastLocation()?.observe(viewLifecycleOwner, Observer { t ->
            viewModel?.loadRestaurants(
                "${t.latitude},${t.longitude}",
                sw = viewModel?.getSWString()!!,
                ne = viewModel?.getNEString()!!
            )
        })

        //Setting the is location permission granted observer. This variable is updated when
        // the user grants the location permission to the app. If the permission is
        // granted, we enable the location on the map view (given that the map is already initialized)
        viewModel?.isLocationPermissionGranted()
            ?.observe(viewLifecycleOwner, Observer { isGranted ->
                if (isGranted && googleMap != null)
                    googleMap?.isMyLocationEnabled = true
            })

        //Setting the is location settings enabled observer. This variable is updated when
        // the user turns the location on or off on his device. If the location is
        // off, we update the showLoader variable to hide the shown loader
        viewModel?.isLocationSettingsEnabled()?.observe(viewLifecycleOwner, Observer { isEnabled ->
            if (!isEnabled) {
                viewModel?.setShowLoader(false)
            }
        })

        //Setting the is show loader observer. This variable is updated when the
        // loader should be displayed or hidden. Based on the variable
        // value, we call the [showLoader] and [hideLoader] functions respectively
        viewModel?.isShowLoader()?.observe(viewLifecycleOwner, Observer { isShow ->
            if (isShow)
                showLoader()
            else
                hideLoader()
        })
    }

    /*
    * A function that overrides the onMapReady function which indicates that the map is ready
    * and that we can start using it.
    * */
    override fun onMapReady(map: GoogleMap) {
        //Setting the map type to normal
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        //Setting the zoom, scroll, compass and rotation ui settings
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isRotateGesturesEnabled = true

        googleMap = map
        setupClusterManager()

        //Setting the map listeners
        googleMap?.setOnCameraIdleListener(this)
        googleMap?.setOnMarkerClickListener(clusterManager)
    }

    /*
    * A function that sets up the cluster manager to be used with the map
    * */
    private fun setupClusterManager() {
        //Initializing the cluster manager
        clusterManager = ClusterManager(activity, googleMap)
        clusterManager!!.setAnimation(true)

        //Setting the cluster manager listeners
        clusterManager!!.setOnClusterItemClickListener(this)
        clusterManager!!.setOnClusterClickListener(this)

        //Setting the custom renderer to change the marker and cluster views
        clusterManager!!.renderer = RestaurantsClusterRenderer(context!!, googleMap, clusterManager)
    }

    /*
    * A function that checks the location settings
    * */
    private fun checkLocationSettings() {
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

    /*
    * A function that zooms the map to the provided location
    *
    * @param lat Latitude of the position to zoom to
    * @param lng Longitude of the position to zoom to
    * */
    private fun zoomToLocation(lat: Double, lng: Double) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    lat, lng
                ), 15f
            )
        )
    }

    /*
    * A function that overrides the onCreateOptionsMenu to inflate the menu file
    * */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    /*
    * A fucntion that overrides the onPrepareOptionsMenu to setup the menu. Since we're using
    * an actionView to the menu item, then we get each view using its ID and set the
    * needed click listeners -if any-.
    * */
    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)

        //Initializing the variables
        val menuItem = menu.findItem(R.id.action_locate)
        val menuLayout = menuItem.actionView

        //Initializing the locate Button and setting its listener
        buttonLocate = menuLayout.findViewById(R.id.button_locate)
        buttonLocate?.setOnClickListener {
            //If the permission is denied, request it
            if (viewModel?.isLocationPermissionGranted()?.value != true) {
                requestLocationPermission()
            } else { //If the permission is granted, check for the settings
                checkLocationSettings()
            }
        }

        //Initializing the loader ProgressBar
        loader = menuLayout.findViewById(R.id.progressBar_loader)
    }

    /*
    * A function that requests the location permission from the user
    * */
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            activity!!,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            MainActivity.requestPermissionId
        )
    }

    /*
    * A function that overrides the onResume function to start the location updates
    * */
    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    /*
    * A function that overrides the onPause function to stop the location updates
    * */
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    /*
    * A function that requests the location updates from the [FusedLocationProviderClient]
    * */
    private fun startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /*
    * A function that removes the location updates from the [FusedLocationProviderClient]
    * */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /*
    * A function that shows the loader and hides the locate button
    * */
    private fun showLoader() {
        loader?.visibility = View.VISIBLE
        buttonLocate?.visibility = View.GONE
    }

    /*
    * A function that hides the loader and shows the locate button
    * */
    private fun hideLoader() {
        loader?.visibility = View.GONE
        buttonLocate?.visibility = View.VISIBLE
    }

    /*
    * A function that overrides the onClusterItemClick function to update the selected
    * restaurant variable value in the viewModel
    * */
    override fun onClusterItemClick(restaurant: Restaurant): Boolean {
        viewModel?.setSelectedRestaurant(restaurant)

        return true
    }

    /*
    * A function that overrides the onClusterClick function and zooms the camera by 1
    * */
    override fun onClusterClick(cluster: Cluster<Restaurant>): Boolean {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                cluster.position,
                floor((googleMap?.cameraPosition?.zoom?.plus(1)!!))
            ), 300, null
        )

        return true
    }

    /*
    * A function that overrides the onCameraIdle to update the sw, ne, lastLocation and
    *  showLoader variable values in the viewModel
    * */
    override fun onCameraIdle() {
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

        clusterManager ?: clusterManager!!.onCameraIdle()
    }
}