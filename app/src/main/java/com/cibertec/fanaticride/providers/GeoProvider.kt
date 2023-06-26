package com.cibertec.fanaticride.providers

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import org.imperiumlabs.geofirestore.GeoFirestore
import org.imperiumlabs.geofirestore.GeoQuery

class GeoProvider {
    val collection = FirebaseFirestore.getInstance().collection("Locaciones")
    val geoFirestore = GeoFirestore(collection)

    fun guardarLocacion(idConductor: String, posicion: LatLng) {
        geoFirestore.setLocation(idConductor, GeoPoint(posicion.latitude, posicion.longitude))
    }

    fun obtenerConductores(posicion: LatLng, radius: Double): GeoQuery {
        val query = geoFirestore.queryAtLocation(GeoPoint(posicion.latitude, posicion.longitude), radius)
        query.removeAllListeners()
        return query
    }

    fun eliminarLocacion(idConductor: String) {
        collection.document(idConductor).delete()
    }

    fun obtenerLocacion(idConductor: String): Task<DocumentSnapshot> {
        return collection.document(idConductor).get().addOnFailureListener { exception ->
            Log.d("FIREBASE", "ERROR: ${exception.toString()}")
        }
    }
}