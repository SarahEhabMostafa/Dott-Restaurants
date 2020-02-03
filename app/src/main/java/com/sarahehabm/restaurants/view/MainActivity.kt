package com.sarahehabm.restaurants.view

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sarahehabm.restaurants.view.map.MapFragment
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {
    private var isPermissionGranted: Boolean = false
    private val requestId = 12

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.sarahehabm.restaurants.R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(com.sarahehabm.restaurants.R.id.container, MapFragment.newInstance())
                .commitNow()
        }
        setSupportActionBar(toolbar)

        button_settings.setOnClickListener { getLocationPermission() }

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            container.visibility = View.VISIBLE
        } else {
            getLocationPermission()
        }
    }

    private fun getLocationPermission() {
        isPermissionGranted = false
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            isPermissionGranted = true
        else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                requestId
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        isPermissionGranted = false

        when (requestCode) {
            requestId -> {
                if (grantResults.isNotEmpty() && grantResults.get(0) == PackageManager.PERMISSION_GRANTED)
                    isPermissionGranted = true
            }
        }

        if (isPermissionGranted) {
            container.visibility = View.VISIBLE
            group.visibility = View.GONE
        } else {
            container.visibility = View.GONE
            group.visibility = View.VISIBLE
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
