package com.sarahehabm.restaurants.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.sarahehabm.restaurants.R
import com.sarahehabm.restaurants.databinding.MainActivityBinding
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.getNetworkService
import com.sarahehabm.restaurants.view.map.MapFragment
import com.sarahehabm.restaurants.viewmodel.MapViewModel
import com.sarahehabm.restaurants.viewmodel.MapViewModelFactory
import kotlinx.android.synthetic.main.bottom_sheet_details.*
import kotlinx.android.synthetic.main.bottom_sheet_details.view.*
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val requestPermissionId = 12
        const val requestCheckSettingsId = 13
    }

    private lateinit var binding: MainActivityBinding
    private lateinit var viewModel: MapViewModel
    private lateinit var viewModelFactory: MapViewModelFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MapFragment.newInstance())
                .commitNow()
        }
        setSupportActionBar(toolbar)

        val repository = RestaurantsRepository(getNetworkService())
        viewModelFactory = MapViewModelFactory(repository)
        viewModel = ViewModelProviders.of(this, viewModelFactory)
            .get(MapViewModel::class.java)

        viewModel.isLocationPermissionGranted().observe(this, Observer { isGranted ->
            if (isGranted) {
                container.visibility = View.VISIBLE
                group.visibility = View.GONE
            } else {
                container.visibility = View.GONE
                group.visibility = View.VISIBLE
            }
        })

        binding.mapViewModel = viewModel
        binding.lifecycleOwner = this

        binding.buttonSettings.setOnClickListener { getLocationPermission() }

        if (isLocationPermissionGranted()) {
            container.visibility = View.VISIBLE
        } else {
            getLocationPermission()
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(bottom_sheet_parent)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN

        viewModel.getSelectedRestaurant().observe(this, Observer { restaurant ->
            textView_name.text = restaurant.name
            val location = restaurant.location

            if (location.address == null || location.address!!.isEmpty())
                textView_address.visibility = View.GONE
            else {
                textView_address.text = location.address
                textView_address.visibility = View.VISIBLE
            }

            if (location.crossStreet == null || location.crossStreet!!.isEmpty())
                textView_crossStreet.visibility = View.GONE
            else {
                textView_crossStreet.text = location.crossStreet
                textView_crossStreet.visibility = View.VISIBLE
            }

            if (location.city == null || location.city!!.isEmpty())
                textView_city.visibility = View.GONE
            else {
                textView_city.text = location.city
                textView_city.visibility = View.VISIBLE
            }

            if (location.state == null || location.state!!.isEmpty())
                textView_state.visibility = View.GONE
            else {
                textView_state.text = location.state
                textView_state.visibility = View.VISIBLE
            }

            if (location.country == null || location.country!!.isEmpty())
                textView_country.visibility = View.GONE
            else {
                textView_country.text = location.country
                textView_country.visibility = View.VISIBLE
            }

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        })

        bottom_sheet_parent.textView_name.setOnClickListener { v ->
            when (bottomSheetBehavior.state) {
                BottomSheetBehavior.STATE_EXPANDED ->
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

                BottomSheetBehavior.STATE_COLLAPSED -> bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_HALF_EXPANDED

                BottomSheetBehavior.STATE_HALF_EXPANDED -> bottomSheetBehavior.state =
                    BottomSheetBehavior.STATE_EXPANDED
            }
        }

        viewModel.getError().observe(this, Observer { error ->
            val message = when (error) {
                "-1" -> getString(R.string.connection_error)
                null -> getString(R.string.unknown_error)
                else -> {
                    error
                }
            }
            Snackbar.make(coordinator_layout, message, Snackbar.LENGTH_SHORT).show()
        })
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLocationPermission() {
        if (!isLocationPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                requestPermissionId
            )
        }
    }

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == requestCheckSettingsId) {
            if (resultCode != Activity.RESULT_OK) {
                Snackbar.make(coordinator_layout, R.string.locate_error, Snackbar.LENGTH_SHORT).show()
                viewModel.setLocationSettingsEnabled(false)
            } else {
                viewModel.setLocationSettingsEnabled(true)
            }
        }
    }
}
