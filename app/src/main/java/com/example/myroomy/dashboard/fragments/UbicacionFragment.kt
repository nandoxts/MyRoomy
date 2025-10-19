package com.example.myroomy.dashboard.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myroomy.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.card.MaterialCardView
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UbicacionFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "UbicacionFragment"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val DEBOUNCE_DELAY = 800L
        private const val REQUEST_TIMEOUT = 15000L
    }

    private lateinit var spinnerDepartamento: Spinner
    private lateinit var txtInfoRuta: TextView
    private lateinit var cardInfoRuta: MaterialCardView
    private lateinit var progressBar: ProgressBar

    private var googleMap: GoogleMap? = null
    private var currentPolyline: Polyline? = null
    private var rutaJob: Job? = null
    private var debounceJob: Job? = null

    private val apiKey = "AIzaSyD69eDGuw2DLihe5mS3i-FvvDeN4sJ_6-c"

    private val ubicaciones = mapOf(
        "Lima" to LatLng(-12.0464, -77.0428),
        "Cusco" to LatLng(-13.5319, -71.9675),
        "Arequipa" to LatLng(-16.3989, -71.5350)
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ubicacion, container, false)

        spinnerDepartamento = view.findViewById(R.id.spinnerDepartamento)
        txtInfoRuta = view.findViewById(R.id.txtInfoRuta)
        cardInfoRuta = view.findViewById(R.id.cardInfoRuta)
        progressBar = view.findViewById(R.id.progressBar)

        configurarSpinner()
        configurarMapa()

        return view
    }

    private fun configurarSpinner() {
        val departamentos = ubicaciones.keys.toList()
        spinnerDepartamento.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            departamentos
        )

        spinnerDepartamento.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val seleccionado = departamentos[position]
                ubicaciones[seleccionado]?.let { destino ->
                    actualizarMapaConDebounce(destino, seleccionado)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun configurarMapa() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap?.isMyLocationEnabled = true
        }

        ubicaciones["Lima"]?.let { actualizarMapa(it, "Lima") }
    }

    private fun actualizarMapaConDebounce(destino: LatLng, nombre: String) {
        debounceJob?.cancel()
        rutaJob?.cancel()

        googleMap?.apply {
            clear()
            addMarker(
                MarkerOptions()
                    .position(destino)
                    .title("Hotel MyRoomy - $nombre")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
            animateCamera(CameraUpdateFactory.newLatLngZoom(destino, 12f))
        }

        txtInfoRuta.text = "Preparando..."
        progressBar.visibility = View.VISIBLE
        cardInfoRuta.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light)
        )

        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(DEBOUNCE_DELAY)
            if (isActive) {
                actualizarMapa(destino, nombre)
            }
        }
    }

    private fun actualizarMapa(destino: LatLng, nombre: String) {
        val origen = LatLng(-12.0464, -77.0428)
        obtenerRuta(origen, destino)
    }

    private fun obtenerRuta(origen: LatLng, destino: LatLng) {
        progressBar.visibility = View.VISIBLE
        txtInfoRuta.text = "Calculando ruta..."
        cardInfoRuta.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light)
        )

        rutaJob?.cancel()

        rutaJob = CoroutineScope(Dispatchers.IO).launch {
            var connection: HttpURLConnection? = null
            try {
                val urlString = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${origen.latitude},${origen.longitude}" +
                        "&destination=${destino.latitude},${destino.longitude}" +
                        "&mode=driving" +
                        "&key=$apiKey"

                Log.d(TAG, "Iniciando petición a Directions API")

                val url = URL(urlString)
                connection = withTimeout(REQUEST_TIMEOUT) {
                    url.openConnection() as HttpURLConnection
                }

                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Accept", "application/json")
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "Response code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val data = response.toString()
                    Log.d(TAG, "Respuesta recibida, tamaño: ${data.length}")

                    val json = JSONObject(data)
                    val status = json.getString("status")

                    Log.d(TAG, "API Status: $status")

                    when (status) {
                        "OK" -> {
                            val routes = json.getJSONArray("routes")
                            if (routes.length() > 0) {
                                val route = routes.getJSONObject(0)
                                val legs = route.getJSONArray("legs").getJSONObject(0)
                                val distancia = legs.getJSONObject("distance").getString("text")
                                val duracion = legs.getJSONObject("duration").getString("text")
                                val puntos = route.getJSONObject("overview_polyline").getString("points")
                                val decodedPath = PolyUtil.decode(puntos)

                                Log.d(TAG, "Ruta OK: $distancia - $duracion")

                                withContext(Dispatchers.Main) {
                                    if (isActive) {
                                        mostrarRuta(decodedPath, distancia, duracion)
                                    }
                                }
                            }
                        }
                        "REQUEST_DENIED" -> {
                            val errorMsg = json.optString("error_message", "")
                            Log.e(TAG, "REQUEST_DENIED: $errorMsg")
                            withContext(Dispatchers.Main) {
                                if (isActive) {
                                    mostrarError("API Key no autorizada")
                                }
                            }
                        }
                        "ZERO_RESULTS" -> {
                            Log.w(TAG, "No se encontró ruta")
                            withContext(Dispatchers.Main) {
                                if (isActive) {
                                    mostrarError("Sin ruta disponible")
                                }
                            }
                        }
                        else -> {
                            Log.e(TAG, "Error API: $status")
                            withContext(Dispatchers.Main) {
                                if (isActive) {
                                    mostrarError("Error: $status")
                                }
                            }
                        }
                    }
                } else {
                    Log.e(TAG, "HTTP Error: $responseCode")
                    withContext(Dispatchers.Main) {
                        if (isActive) {
                            mostrarError("Error de conexión")
                        }
                    }
                }

            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Timeout en la petición")
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        mostrarError("Tiempo de espera agotado")
                    }
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "Petición cancelada por el usuario")
            } catch (e: Exception) {
                Log.e(TAG, "Excepción: ${e.javaClass.simpleName} - ${e.message}", e)
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        mostrarError("Error de red")
                    }
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun mostrarRuta(path: List<LatLng>, distancia: String, duracion: String) {
        try {
            currentPolyline?.remove()

            val polylineOptions = PolylineOptions()
                .addAll(path)
                .width(10f)
                .color(ContextCompat.getColor(requireContext(), R.color.green))
                .geodesic(true)

            currentPolyline = googleMap?.addPolyline(polylineOptions)

            progressBar.visibility = View.GONE
            cardInfoRuta.setCardBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.green)
            )
            txtInfoRuta.text = "Distancia: $distancia | Tiempo: $duracion"

            Log.d(TAG, "Ruta mostrada exitosamente")

        } catch (e: Exception) {
            Log.e(TAG, "Error al mostrar ruta: ${e.message}")
            mostrarError("Error al dibujar")
        }
    }

    private fun mostrarError(mensaje: String) {
        progressBar.visibility = View.GONE
        cardInfoRuta.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
        )
        txtInfoRuta.text = mensaje
    }

    override fun onDestroyView() {
        super.onDestroyView()
        debounceJob?.cancel()
        rutaJob?.cancel()
        currentPolyline = null
        googleMap = null
    }
}