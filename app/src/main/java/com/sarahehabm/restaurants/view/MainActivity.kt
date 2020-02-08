package com.sarahehabm.restaurants.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.sarahehabm.restaurants.R
import com.sarahehabm.restaurants.databinding.MainActivityBinding
import com.sarahehabm.restaurants.repository.RestaurantsRepository
import com.sarahehabm.restaurants.repository.getNetworkService
import com.sarahehabm.restaurants.view.map.MapFragment
import com.sarahehabm.restaurants.viewmodel.MapViewModel
import com.sarahehabm.restaurants.viewmodel.MapViewModelFactory
import kotlinx.android.synthetic.main.layout_bottom_sheet_details.*
import kotlinx.android.synthetic.main.layout_bottom_sheet_details.view.*
import kotlinx.android.synthetic.main.main_activity.*

/*
* The Main activity of the application. It holds the MapFragment, viewModel and a bottom sheet
* */
class MainActivity : AppCompatActivity() {
    companion object {
        //Declare the constant request IDs
        const val requestPermissionId = 12
        const val requestCheckSettingsId = 13
    }

    //Declare the variables
    private lateinit var binding: MainActivityBinding
    private lateinit var viewModel: MapViewModel
    private lateinit var viewModelFactory: MapViewModelFactory
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Initializing the binding variable
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)

        //Creating and setting a MapFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MapFragment.newInstance())
                .commitNow()
        }
        //Setting the toolbar
        setSupportActionBar(toolbar)

        //Initializing the viewModel
        val repository = RestaurantsRepository(getNetworkService())
        viewModelFactory = MapViewModelFactory(repository)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(MapViewModel::class.java)

        binding.mapViewModel = viewModel
        binding.lifecycleOwner = this

        //If the permission is granted, show the map
        if (isLocationPermissionGranted()) {
            container.visibility = View.VISIBLE
        } else { //If the permission is denied, request the location permissions from the user
            getLocationPermission()
        }

        //Initializing the bottom sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_parent)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        //Setting the observers
        setObservers()
        //Setting the listeners
        setListeners()
    }

    /*
    * A function that sets all the needed viewModel observers
    * */
    private fun setObservers() {
        //Setting the is location permission granted observer. This variable is updated when
        // the user grants the location permission to the app. If the permission is
        // granted, we display the map and hide the message and button.
        // If the permission is denied, we hide the map and show the message and the button
        viewModel.isLocationPermissionGranted().observe(this, Observer { isGranted ->
            if (isGranted) {
                container.visibility = View.VISIBLE
                group.visibility = View.GONE
            } else {
                container.visibility = View.GONE
                group.visibility = View.VISIBLE
            }
        })

        //Setting the selected restaurant observer. This variable is updated when the
        // user clicks on a marker from the map
        viewModel.getSelectedRestaurant().observe(this, Observer { restaurant ->
            //Update the details (name and address) of the restaurant in the view. If a part of
            // the address doesn't exist, we hide its view
            textView_name.text = restaurant.name
            val location = restaurant.location

            setTextOrHideView(location.address, textView_address)
            setTextOrHideView(location.crossStreet, textView_crossStreet)
            setTextOrHideView(location.city, textView_city)
            setTextOrHideView(location.state, textView_state)
            setTextOrHideView(location.country, textView_country)

            //Expand the bottom sheet to the half
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        })

        //Setting the error observer. This variable is updated when an error is received from
        // the API call or an exception is thrown
        viewModel.getError().observe(this, Observer { error ->
            val message = when (error) {
                "-1" -> getString(R.string.connection_error)
                null -> getString(R.string.unknown_error)
                else -> {
                    error
                }
            }

            //Display the received/generated message in a Snackbar
            Snackbar.make(coordinator_layout, message, Snackbar.LENGTH_SHORT).show()
        })
    }

    /*
    * A function that sets the provided text to the provided [TextView] and hides the view if
    *  the text is null or empty
    *
    * @param string The text to be set to the view
    * @param textView The view that will have its text updates
    * */
    private fun setTextOrHideView(string: String?, textView: TextView) {
        if (string == null || string.isEmpty())
            textView.visibility = View.GONE
        else {
            textView.text = string
            textView.visibility = View.VISIBLE
        }
    }

    /*
    * A function that sets the needed listeners
    * */
    private fun setListeners() {
        //Setting the settings button listener to fire the get location permission
        binding.buttonSettings.setOnClickListener { getLocationPermission() }

        //Setting the Restaurant name click listener to expand the bottom sheet if it's collapsed
        // and collapse it if it's expanded
        bottom_sheet_parent.textView_name.setOnClickListener {
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_EXPANDED ->
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_HALF_EXPANDED

                BottomSheetBehavior.STATE_HALF_EXPANDED -> bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    /*
    * A function that checks whether the location permission is granted to the application or not
    * */
    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /*
    * A function that requests the location permission from the user if it is denied
    * */
    private fun getLocationPermission() {
        if (!isLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                requestPermissionId
            )
        }
    }

    /*
    * A function that overrides the onRequestPermissionsResult. The updated values is used
    * to notify the viewModel
    * */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isPermissionGranted = false

        when (requestCode) {
            requestPermissionId -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    isPermissionGranted = true
            }
        }

        viewModel.setLocationPermissionGranted(isPermissionGranted)
    }

    /*
    * A function that overrides the onActivityResult to handle the settings enables or not.
    * The updated value is used to notifu the viewModel
    * The updated value is used to notifu the viewModel
    * */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestCheckSettingsId) {
            if (resultCode != Activity.RESULT_OK) {
                Snackbar.make(coordinator_layout, R.string.locate_error, Snackbar.LENGTH_SHORT)
                    .show()
                viewModel.setLocationSettingsEnabled(false)
            } else {
                viewModel.setLocationSettingsEnabled(true)
            }
        }
    }
}
