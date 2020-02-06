package com.sarahehabm.restaurants.view

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.sarahehabm.restaurants.R
import com.sarahehabm.restaurants.databinding.MainActivityBinding
import com.sarahehabm.restaurants.model.RestaurantsRepository
import com.sarahehabm.restaurants.model.getNetworkService
import com.sarahehabm.restaurants.view.map.MapFragment
import com.sarahehabm.restaurants.viewmodel.MapViewModel
import com.sarahehabm.restaurants.viewmodel.MapViewModelFactory
import kotlinx.android.synthetic.main.bottom_sheet_details.*
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

        val bottomsheet_behavior = BottomSheetBehavior.from(bottomsheet_parent)
        bottomsheet_behavior.state = BottomSheetBehavior.STATE_HIDDEN

        viewModel.getSelectedRestaurant().observe(this, Observer { restaurant ->
            textView_name.text = restaurant.name
            textView_address.text = restaurant.location.formattedAddress.toString()
            textView_location.text = getString(
                R.string.latlng_formatted,
                restaurant.location.lat,
                restaurant.location.lng
            )
            bottomsheet_behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
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
                Toast.makeText(this, getString(R.string.locate_error), Toast.LENGTH_SHORT).show()
                viewModel.setLocationSettingsEnabled(false)
            } else {
                viewModel.setLocationSettingsEnabled(true)
            }
        }
    }
}
