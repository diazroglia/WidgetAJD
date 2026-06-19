package com.example.weatherwidget

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permisoBtn = findViewById<Button>(R.id.request_permission_button)
        val actualizarBtn = findViewById<Button>(R.id.update_location_button)
        val statusText = findViewById<TextView>(R.id.status_text)

        permisoBtn.setOnClickListener {
            pedirPermisoUbicacion()
        }

        actualizarBtn.setOnClickListener {
            actualizarUbicacion()
        }

        actualizarEstado(statusText)
        
        // Check and request permission automatically on startup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pedirPermisoUbicacion()
        } else {
            actualizarBtn.isEnabled = true
            // If permission is already granted, try to update location immediately
            actualizarUbicacion()
        }
    }

    private fun pedirPermisoUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            findViewById<TextView>(R.id.status_text).text = getString(R.string.permission_granted)
            findViewById<Button>(R.id.update_location_button).isEnabled = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val statusText = findViewById<TextView>(R.id.status_text)
        val actualizarBtn = findViewById<Button>(R.id.update_location_button)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.text = getString(R.string.permission_granted)
                actualizarBtn.isEnabled = true
            } else {
                statusText.text = getString(R.string.permission_denied)
                actualizarBtn.isEnabled = false
            }
        }
    }

    private fun actualizarEstado(statusText: TextView) {
        val estado = if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            getString(R.string.permission_granted)
        } else {
            getString(R.string.waiting_permissions)
        }
        statusText.text = getString(R.string.status_format, estado)
    }

    private fun actualizarUbicacion() {
        val statusText = findViewById<TextView>(R.id.status_text)
        statusText.text = "Obteniendo ubicación..."
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val location = withTimeoutOrNull(4000) {
                        LocationHelper.getCurrentLocation(this@MainActivity)
                    }
                    if (location != null) {
                        val (latitude, longitude) = location
                        // Get city name from coordinates
                        val cityName = LocationHelper.getCityName(
                            this@MainActivity,
                            latitude,
                            longitude
                        ) ?: getString(R.string.default_location_name)
                        
                        // Save location with city name
                        LocationHelper.saveLocation(
                            this@MainActivity,
                            latitude,
                            longitude,
                            cityName
                        )
                        statusText.text = getString(R.string.location_updated, cityName)
                    } else {
                        // Fallback: try lastLocation directly (instantaneous)
                        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                val cityName = LocationHelper.getCityName(
                                    this@MainActivity,
                                    lastLoc.latitude,
                                    lastLoc.longitude
                                ) ?: getString(R.string.default_location_name)
                                
                                LocationHelper.saveLocation(
                                    this@MainActivity,
                                    lastLoc.latitude,
                                    lastLoc.longitude,
                                    cityName
                                )
                                statusText.text = getString(R.string.location_updated, cityName)
                            } else {
                                statusText.text = getString(R.string.location_error)
                            }
                        }.addOnFailureListener {
                            statusText.text = getString(R.string.location_error)
                        }
                    }
                } catch (e: Exception) {
                    statusText.text = getString(R.string.location_error)
                }
            }
        } else {
            statusText.text = getString(R.string.location_permission_not_granted)
        }
    }
}
