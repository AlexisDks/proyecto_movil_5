package com.cibertec.fanaticride.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.icu.text.IDNA.Info
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cibertec.fanaticride.R
import com.cibertec.fanaticride.colecciones.ConductorUbicacion
import com.cibertec.fanaticride.databinding.ActivityMapBinding
import com.cibertec.fanaticride.providers.AuthProvider
import com.cibertec.fanaticride.providers.GeoProvider
import com.cibertec.fanaticride.utils.CarMoveAnim
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.SphericalUtil
import org.imperiumlabs.geofirestore.GeoQuery
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener {

    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()

    // GOOGLE PLACES
    private var places: PlacesClient? = null
    private var autocompleteOrigin: AutocompleteSupportFragment? = null
    private var autocompleteDestination: AutocompleteSupportFragment? = null
    private var originName = ""
    private var destinationName = ""
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var isLocationEnabled = false

    //CONDUCTOR
    private val driverMarkers = ArrayList<Marker>()
    private val driverLocation = ArrayList<ConductorUbicacion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        locationPermissions.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        startGooglePlaces()

        binding.btnRequestTrip.setOnClickListener {
            gotoInfo()
        }
    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido")
                    easyWayLocation?.startLocation()

                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido con limitacion")
                    easyWayLocation?.startLocation()

                }
                else -> {
                    Log.d("LOCALIZACION", "Permiso no concedido")
                }
            }
        }
    }

    private fun getNearbyDrivers() {
        if (myLocationLatLng == null) return

        geoProvider.obtenerConductores(myLocationLatLng!!, 20.0).addGeoQueryEventListener(object: GeoQueryEventListener {
            override fun onGeoQueryError(exception: Exception) {

            }

            override fun onGeoQueryReady() {
            }

            override fun onKeyEntered(documentID: String, location: GeoPoint) {
                Log.d("FIRESTORE", "Document id: $documentID")
                Log.d("FIRESTORE", "location: $location")

                for (marker in driverMarkers) {
                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
                            return
                        }
                    }
                }

                // CREAMOS UN NUEVO MARCADOR PARA EL CONDUCTOR CONECTADO
                val driverLatLng = LatLng(location.latitude, location.longitude)
                val marker = googleMap?.addMarker(
                    MarkerOptions().position(driverLatLng).title("Conductor disponible").icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.uber_car)
                    )
                )

                marker?.tag = documentID
                driverMarkers.add(marker!!)

                val dl = ConductorUbicacion()
                dl.id = documentID
                driverLocation.add(dl)
            }

            override fun onKeyExited(documentID: String) {
                for (marker in driverMarkers) {
                    if (marker.tag != null) {
                        if (marker.tag == documentID) {
                            marker.remove()
                            driverMarkers.remove(marker)
                            driverLocation.removeAt(getPositionDriver(documentID))
                            return
                        }
                    }
                }
            }

            override fun onKeyMoved(documentID: String, location: GeoPoint) {
                for (marker in driverMarkers) {

                    val start = LatLng(location.latitude, location.longitude)
                    var end: LatLng? = null
                    val position = getPositionDriver(marker.tag.toString())

                    if(marker.tag != null) {
                        if(marker.tag == documentID) {
                            marker.position = LatLng(location.latitude, location.longitude)

                            if (driverLocation[position].latLng != null) {
                                end = driverLocation[position].latLng
                            }
                            driverLocation[position].latLng = LatLng(location.latitude, location.longitude)
                            if (end  != null) {
                                CarMoveAnim.carAnim(marker, end, start)
                            }
                        }
                    }
                }
            }

        })
    }

    private fun getPositionDriver(id: String): Int {
        var position = 0
        for (i in driverLocation.indices) {
            if(id == driverLocation[i].id) {
                position = i
                break
            }
        }
        return position
    }

    private fun gotoInfo() {

        if(originLatLng != null && destinationLatLng != null) {
            val i = Intent(this, InfoActivity::class.java)
            i.putExtra("recojo",  originName)
            i.putExtra("destino", destinationName)
            i.putExtra("origen_latitud", originLatLng?.latitude)
            i.putExtra("origen_longitud", originLatLng?.longitude)
            i.putExtra("destino_latitud", destinationLatLng?.latitude)
            i.putExtra("destino_longitud", destinationLatLng?.longitude)
            startActivity(i)
        } else {
            Toast.makeText(this, "Seleccionar Ubicaciones", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onCameraMove() {
        googleMap?.setOnCameraIdleListener {
            try {
                val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target
                val direccionList = geocoder.getFromLocation(originLatLng?.latitude!!, originLatLng?.longitude!!, 1)
                if (direccionList != null && direccionList.isNotEmpty()) {
                val ciudad = direccionList[0].locality
                val pais = direccionList[0].countryName
                val direccion = direccionList[0].getAddressLine(0)
                val origenNombre = "$direccion $ciudad"
                autocompleteOrigin?.setText("$direccion $ciudad")
                }
            } catch(e: Exception) {
                Log.d("ERROR", "Mensaje : ${e.message}")
            }
        }
    }

    private fun startGooglePlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, resources.getString(R.string.google_maps_key))
        }

        places = Places.createClient(this)
        instanceAutocompleteOrigin()
        instanceAutocompleteDestination()
    }

    private fun limitSearch() {
        val northSide = SphericalUtil.computeOffset(myLocationLatLng, 5000.0, 0.0)
        val southSide = SphericalUtil.computeOffset(myLocationLatLng, 5000.0, 180.0)

        autocompleteOrigin?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
        autocompleteDestination?.setLocationBias(RectangularBounds.newInstance(southSide, northSide))
    }

    private fun instanceAutocompleteOrigin() {
        autocompleteOrigin = supportFragmentManager.findFragmentById(R.id.placesAutocompleteOr) as AutocompleteSupportFragment
        autocompleteOrigin?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )
        autocompleteOrigin?.setHint("Lugar de recogida")
        autocompleteOrigin?.setCountry("PE")
        autocompleteOrigin?.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                originName = place.name!!
                originLatLng = place.latLng
                Log.d("PLACES", "Address: $originName")
                Log.d("PLACES", "LAT: ${originLatLng?.latitude}")
                Log.d("PLACES", "LNG: ${originLatLng?.longitude}")
            }

            override fun onError(status: Status) {
                Log.e("PLACES", "Error en la selección de lugar: ${status.statusMessage}")
            }
        })
    }

    private fun instanceAutocompleteDestination() {
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDes) as AutocompleteSupportFragment
        autocompleteDestination?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
            )
        )
        autocompleteDestination?.setHint("Destino")
        autocompleteDestination?.setCountry("PE")
        autocompleteDestination?.setOnPlaceSelectedListener(object: PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destinationName = place.name!!
                destinationLatLng = place.latLng
                Log.d("PLACES", "Address: $destinationName")
                Log.d("PLACES", "LAT: ${destinationLatLng?.latitude}")
                Log.d("PLACES", "LNG: ${destinationLatLng?.longitude}")
            }

            override fun onError(status: Status) {
                Log.e("PLACES", "Error en la selección de lugar: ${status.statusMessage}")
            }
        })
    }


    override fun onResume() {
        super.onResume() // ABRIMOS LA PANTALLA ACTUAL
    }

    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        onCameraMove()
//        easyWayLocation?.startLocation();

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        googleMap?.isMyLocationEnabled = false

        try {
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
            )
            if (!success!!) {
                Log.d("MAPAS", "No se pudo encontrar el estilo")
            }

        } catch (e: Resources.NotFoundException) {
            Log.d("MAPAS", "Error: ${e.toString()}")
        }

    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) { // ACTUALIZACION DE LA POSICION EN TIEMPO REAL
        myLocationLatLng = LatLng(location.latitude, location.longitude) // LAT Y LONG DE LA POSICION ACTUAL

        if(!isLocationEnabled) { // UNA SOLA VEZ
            isLocationEnabled = true
             googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(myLocationLatLng!!).zoom(15f).build()
        ))
            getNearbyDrivers()
            limitSearch()
        }
    }

    override fun locationCancelled() {

    }


}