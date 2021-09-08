package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PointOfInterest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
        private const val REQUEST_LOCATION_PERMISSION = 1
        private const val REQUEST_TURN_DEVICE_LOCATION_ON = 2
    }

    private lateinit var googleMap: GoogleMap
    private lateinit var locationProviderClient: FusedLocationProviderClient
    private lateinit var binding: FragmentSelectLocationBinding

    override val _viewModel: SaveReminderViewModel by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        locationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        mapFragment.getMapAsync(this)

        return binding.root
    }

    override fun onMapReady(map: GoogleMap) {
        Log.i(TAG, "OnMapReady()")
        googleMap = map
        googleMap.apply {
            setStyle()
            zoomToDeviceLocation()
            enableMyLocation()
            onPickPOI()
            onPickCustomLocation()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        runCatching {
            if (requestCode == REQUEST_LOCATION_PERMISSION) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    // Should we show an explanation?
                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        Snackbar.make(
                            binding.mainLayout,
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        //Never ask again selected, or device policy prohibits the app from having that permission.
                        //So, disable that feature, or fall back to another situation...
                        Snackbar.make(
                            binding.mainLayout,
                            R.string.permission_denied_explanation,
                            Snackbar.LENGTH_LONG
                        ).setAction(R.string.settings) {
                            startActivity(Intent().apply {
                                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            })
                        }.show()
                    }
                }
            }
        }.onFailure { t -> Log.e(TAG, "Error in onRequestPermissionsResult(): ${t.message}") }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON)
            checkDeviceLocationSettings(false)
    }

    private fun GoogleMap.setStyle() {
        runCatching {
            setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
        }.onFailure { Log.e(TAG, "Failed to parse map style!") }
    }

    @SuppressLint("MissingPermission")
    fun zoomToDeviceLocation() {
        locationProviderClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val zoomLevel = 15f
                googleMap.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        userLatLng,
                        zoomLevel
                    )
                )
            }
        }
    }

    private fun onLocationSelected(poi: PointOfInterest) {
        runCatching {
            val latLng = poi.latLng
            _viewModel.reminderSelectedLocationStr.value = poi.name
            _viewModel.latitude.value = latLng.latitude
            _viewModel.longitude.value = latLng.longitude
            findNavController().popBackStack()
        }.onFailure { t -> Log.e(TAG, "Error in onLocationSelected(): ${t.message}") }
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettings(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.mainLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettings()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                runCatching {
                    googleMap.isMyLocationEnabled = true
                }.onFailure { t ->
                    Log.e(TAG, "Failed to enable my location feature: ${t.message}")
                }
            }
        }
    }

    private fun GoogleMap.onPickPOI() {
        runCatching {
            setOnPoiClickListener { poi ->
                binding.saveLocationButton.visibility = View.VISIBLE
                binding.saveLocationButton.setOnClickListener {
                    onLocationSelected(poi)
                }
                val poiMarker = addMarker(
                    MarkerOptions().position(poi.latLng).title(poi.name)
                )
                poiMarker?.showInfoWindow()
            }
        }.onFailure { t -> Log.e(TAG, "Error in onPickCustomLocation(): ${t.message}") }
    }

    private fun GoogleMap.onPickCustomLocation() {
        runCatching {
            setOnMapClickListener { latLng ->
                binding.saveLocationButton.visibility = View.VISIBLE
                binding.saveLocationButton.setOnClickListener {
                    _viewModel.latitude.value = latLng.latitude
                    _viewModel.longitude.value = latLng.longitude
                    _viewModel.reminderSelectedLocationStr.value = "Using a Custom location"
                    findNavController().popBackStack()
                }

                val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 15f)
                moveCamera(cameraUpdate)
                val poiMarker = addMarker(MarkerOptions().position(latLng))
                poiMarker?.showInfoWindow()
            }
        }.onFailure { t -> Log.e(TAG, "Error in onPickCustomLocation(): ${t.message}") }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isPermissionGranted()) {
                    googleMap.isMyLocationEnabled = true
                } else {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
            } else googleMap.isMyLocationEnabled = true
            checkDeviceLocationSettings()
        }.onFailure { t ->
            Log.e(TAG, "Failed to enable my location feature: ${t.message}")
        }
    }
}