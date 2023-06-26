package com.cibertec.fanaticride.activities

import android.content.Intent
import android.content.res.Resources
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cibertec.fanaticride.R
import com.cibertec.fanaticride.colecciones.Precios
import com.cibertec.fanaticride.databinding.ActivityMapInfoBinding
import com.cibertec.fanaticride.providers.ConfiguracionProvider
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlin.math.log

class InfoActivity: AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {
    private lateinit var binding: ActivityMapInfoBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    private var extraOriginName = ""
    private var extraDestinationName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0

    private var originLatLn: LatLng? = null
    private var destinationLatLn: LatLng? = null

    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil

    private var markerOrigin: Marker? = null
    private var markerDestination: Marker? = null

    private var configuracionProvider = ConfiguracionProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // EXTRAS
        extraOriginName = intent.getStringExtra("recojo")!!
        extraDestinationName = intent.getStringExtra("destino")!!
        extraOriginLat = intent.getDoubleExtra("origen_latitud", 0.0)
        extraOriginLng = intent.getDoubleExtra("origen_longitud", 0.0)
        extraDestinationLat = intent.getDoubleExtra("destino_latitud", 0.0)
        extraDestinationLng = intent.getDoubleExtra("destino_longitud", 0.0)

        originLatLn = LatLng(extraOriginLat, extraOriginLng)
        destinationLatLn = LatLng(extraDestinationLat, extraDestinationLng)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 0
            fastestInterval = 0
            priority = Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }

        easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

        binding.textViewOrigin.text = extraOriginName
        binding.textViewDestination.text = extraDestinationName

        Log.d("LOCALIZACION", "Origen Lat: ${originLatLn?.latitude}")
        Log.d("LOCALIZACION", "Origen Lng: ${originLatLn?.longitude}")
        Log.d("LOCALIZACION", "Destino Lat: ${destinationLatLn?.latitude}")
        Log.d("LOCALIZACION", "Destino Lng: ${destinationLatLn?.longitude}")

        binding.imageViewBack.setOnClickListener {
            finish()
        }

        binding.btnConfirmRequest.setOnClickListener {
            gotoSearch()
        }
    }

    private fun gotoSearch() {

        if(originLatLn != null && destinationLatLn != null) {
            val i = Intent(this, SearchActivity::class.java)
            i.putExtra("recojo",  extraOriginName)
            i.putExtra("destino", extraDestinationName)
            i.putExtra("origen_latitud", originLatLn?.latitude)
            i.putExtra("origen_longitud", originLatLn?.longitude)
            i.putExtra("destino_latitud", destinationLatLn?.latitude)
            i.putExtra("destino_longitud", destinationLatLn?.longitude)
            startActivity(i)
        } else {
            Toast.makeText(this, "Seleccionar Ubicaciones", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPrices(distance: Double, time: Double) {
        configuracionProvider.obtenerPrecios().addOnSuccessListener {
            document ->
                if(document.exists()) {
                    val price = document.toObject(Precios::class.java)  // DOCUMENTO CON LA INFORMACION

                    val totalDistance = distance * price?.km!! // VALOR POR KM

                    Log.d("PRECIOS", "DISTANCIA TOTAL $totalDistance")

                    val totalTime = time * price?.min!! // VALOR POR MIN

                    Log.d("PRECIOS", "TIEMPO TOTAL $totalTime")

                    var total = totalDistance + totalTime // TOTAL

                    Log.d("PRECIOS", "TOTAL $total")

                    total = if(total < 5.0) price?.valorMin!! else total

                    var minTotal = total - price?.diferencia!! // RESTAS 2 SOLES AL TOTAL
                    var maxTotal = total + price?.diferencia!! // SUMAR 2 SOLES AL TOTAL

                    val minTotalString = String.format("%.1f", minTotal)
                    val maxTotalString = String.format("%.1f", maxTotal)

                    binding.textViewPrice.text = "S/ $minTotalString - $maxTotalString"
                }
        }
    }

    private fun addOriginMarker() {
        markerOrigin = googleMap?.addMarker(MarkerOptions().position(originLatLn!!).title("Recoger aqui")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))
    }

    private fun addDestinationMarker() {
        if (destinationLatLn != null) {
            markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLn!!).title("Destino")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))
        }
    }

    private fun easyDrawRoute() {
        wayPoints.clear()
        wayPoints.add(originLatLn!!)
        wayPoints.add(destinationLatLn!!)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey(resources.getString(R.string.google_maps_key))
            .setOrigin(originLatLn!!)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLinePrimaryColor(R.color.black)
            .setPolyLineWidth(12)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destinationLatLn!!)
            .build()

        directionUtil.initPath()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(originLatLn!!).zoom(13f).build()
        ))

        easyDrawRoute()
        addOriginMarker()
        addDestinationMarker()

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
        TODO("Not yet implemented")
    }

    override fun currentLocation(location: Location?) {
        TODO("Not yet implemented")
    }

    override fun locationCancelled() {
        TODO("Not yet implemented")
    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        var distance = polyLineDetailsArray[1].distance.toDouble() // Metros
        var time = polyLineDetailsArray[1].time.toDouble() // Segundos

        distance = if (distance < 1000.0) 1000.0 else distance // Menor de 1000 metros en 1km
        time = if(time < 60.0) 60.0 else time

        distance /= 1000 // KILOMETROS
        time /= 60 //MINUTOS

        val timeString = String.format("%.2f", time).toDouble()
        val distanceString = String.format("%.2f", distance).toDouble()

        getPrices(distance, time)

        binding.textViewTimeAndDistance.text = "$timeString mins - $distanceString km"

        directionUtil.drawPath(WAY_POINT_TAG)
    }
}