package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myroomy.R
import com.example.myroomy.data.api.RetrofitClient
import com.example.myroomy.data.models.CulqiTokenRequest
import com.example.myroomy.dashboard.database.HabitacionDAO
import com.example.myroomy.dashboard.database.ReservaDAO
import com.example.myroomy.dashboard.models.Reserva
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class PagoFragment : Fragment() {

    // 🔑 Reemplaza con tu clave pública de Culqi Sandbox
    private val PUBLIC_KEY = "pk_test_K9z4TGDS15roIg2H"

    private lateinit var edtCardNumber: EditText
    private lateinit var edtMonth: EditText
    private lateinit var edtYear: EditText
    private lateinit var edtCvv: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnPagar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtResultado: TextView
    private lateinit var txtMontoTotal: TextView

    // Datos de la reserva
    private var idUsuario: Int = -1
    private var idHabitacion: Int = -1
    private var fechaInicio: String = ""
    private var fechaFin: String = ""
    private var cantidadPersonas: Int = 1
    private var comentario: String = ""
    private var total: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pago, container, false)

        // Recibir datos de la reserva
        arguments?.let {
            idUsuario = it.getInt("usuario_id", -1)
            idHabitacion = it.getInt("habitacion_id", -1)
            fechaInicio = it.getString("fecha_inicio", "")
            fechaFin = it.getString("fecha_fin", "")
            cantidadPersonas = it.getInt("cantidad_personas", 1)
            comentario = it.getString("comentario", "")
            total = it.getDouble("total", 0.0)
        }

        // Referencias UI
        edtCardNumber = view.findViewById(R.id.edtCardNumber)
        edtMonth = view.findViewById(R.id.edtMonth)
        edtYear = view.findViewById(R.id.edtYear)
        edtCvv = view.findViewById(R.id.edtCvv)
        edtEmail = view.findViewById(R.id.edtEmail)
        btnPagar = view.findViewById(R.id.btnPagar)
        progressBar = view.findViewById(R.id.progressBar)
        txtResultado = view.findViewById(R.id.txtResultado)
        txtMontoTotal = view.findViewById(R.id.txtMontoTotal)

        // Mostrar monto a pagar
        txtMontoTotal.text = "Total a pagar: S/ %.2f".format(total)

        btnPagar.setOnClickListener {
            crearToken()
        }

        return view
    }

    private fun crearToken() {
        val cardNumber = edtCardNumber.text.toString().trim().replace(" ", "")
        val cvv = edtCvv.text.toString().trim()
        val expMonth = edtMonth.text.toString().trim()
        val expYear = edtYear.text.toString().trim()
        val email = edtEmail.text.toString().trim()

        // Validaciones
        if (cardNumber.isEmpty() || cvv.isEmpty() || expMonth.isEmpty() || expYear.isEmpty()) {
            Toast.makeText(requireContext(), "⚠️ Completa todos los datos de la tarjeta", Toast.LENGTH_SHORT).show()
            return
        }

        if (cardNumber.length < 13 || cardNumber.length > 19) {
            Toast.makeText(requireContext(), "⚠️ Número de tarjeta inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (cvv.length < 3 || cvv.length > 4) {
            Toast.makeText(requireContext(), "⚠️ CVV inválido", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "⚠️ Ingresa un email válido", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        txtResultado.text = "⏳ Procesando pago..."
        txtResultado.visibility = View.VISIBLE
        btnPagar.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = CulqiTokenRequest(
                    cardNumber = cardNumber,
                    cvv = cvv,
                    expirationMonth = expMonth,
                    expirationYear = expYear,
                    email = email
                )

                val response = RetrofitClient.culqiService.createToken(
                    authorization = "Bearer $PUBLIC_KEY",
                    request = request
                )

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val tokenData = response.body()!!
                        val tokenId = tokenData.id

                        if (tokenId != null) {
                            txtResultado.text = "✅ Token generado\nProcesando pago..."

                            // ✅ CONFIRMAR RESERVA DESPUÉS DEL TOKEN
                            confirmarReservaConPago(tokenId)
                        } else {
                            progressBar.visibility = View.GONE
                            btnPagar.isEnabled = true
                            txtResultado.text = "❌ Error: ${tokenData.merchantMessage ?: "No se pudo crear el token"}"
                        }
                    } else {
                        progressBar.visibility = View.GONE
                        btnPagar.isEnabled = true

                        val errorBody = response.errorBody()?.string()
                        txtResultado.text = "❌ Error al procesar pago\n\nCódigo: ${response.code()}\n$errorBody"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnPagar.isEnabled = true
                    txtResultado.text = "❌ Error de conexión:\n${e.message}"
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmarReservaConPago(tokenId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fechaSolicitud = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val reserva = Reserva(
                    idUsuario = idUsuario,
                    idHabitacion = idHabitacion,
                    fechaSolicitud = fechaSolicitud,
                    estado = "Confirmada", // ✅ Ya está pagado
                    comentario = "$comentario | Token: $tokenId",
                    fechaIngreso = fechaInicio,
                    fechaSalida = fechaFin,
                    total = total,
                    metodoPago = "Tarjeta (Culqi)",
                    cantidadPersonas = cantidadPersonas
                )

                val reservaDAO = ReservaDAO(requireContext())
                val idReserva = reservaDAO.insertar(reserva)

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE

                    if (idReserva != -1L) {
                        // Actualizar estado de habitación
                        val habitacionDao = HabitacionDAO(requireContext())
                        habitacionDao.actualizarEstado(idHabitacion, "Reservada")

                        txtResultado.text = "✅ ¡Pago exitoso!\n\nReserva confirmada\nID: $idReserva\nToken: $tokenId"

                        Toast.makeText(requireContext(), "✅ Reserva confirmada con éxito", Toast.LENGTH_LONG).show()

                        // Regresar después de 2 segundos
                        view?.postDelayed({
                            requireActivity().supportFragmentManager.popBackStack()
                            requireActivity().supportFragmentManager.popBackStack()
                        }, 2000)
                    } else {
                        txtResultado.text = "⚠️ Pago procesado pero error al guardar reserva.\nToken: $tokenId"
                        btnPagar.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnPagar.isEnabled = true
                    txtResultado.text = "❌ Error al confirmar reserva: ${e.message}"
                }
            }
        }
    }
}