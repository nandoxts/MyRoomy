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
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import cn.pedant.SweetAlert.SweetAlertDialog

class PagoFragment : Fragment() {

    private val PUBLIC_KEY = "pk_test_K9z4TGDS15roIg2H"

    private lateinit var edtCardNumber: EditText
    private lateinit var edtMonth: EditText
    private lateinit var edtYear: EditText
    private lateinit var edtCvv: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnPagar: Button
    private lateinit var progressBar: ProgressBar
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
        txtMontoTotal = view.findViewById(R.id.txtMontoTotal)

        // Mostrar monto total
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

        // ✅ Validaciones
        when {
            cardNumber.isEmpty() || cvv.isEmpty() || expMonth.isEmpty() ||
                    expYear.isEmpty() || email.isEmpty() -> {
                mostrarError("Por favor completa todos los campos obligatorios.")
                return
            }

            cardNumber.length < 13 || cardNumber.length > 19 -> {
                mostrarError("Número de tarjeta inválido.\nDebe tener entre 13 y 19 dígitos.")
                return
            }

            cvv.length < 3 || cvv.length > 4 -> {
                mostrarError("CVV inválido.\nDebe tener 3 o 4 dígitos.")
                return
            }

            expMonth.toIntOrNull() == null || expMonth.toInt() !in 1..12 -> {
                mostrarError("Mes inválido.\nDebe estar entre 01 y 12.")
                return
            }

            expYear.toIntOrNull() == null || expYear.length != 4 || expYear.toInt() < Calendar.getInstance().get(Calendar.YEAR) -> {
                mostrarError("Año inválido.\nDebe ser de 4 dígitos y no menor al año actual.")
                return
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mostrarError("Correo electrónico inválido.\nPor favor ingresa uno válido.")
                return
            }
        }

        // ✅ Mostrar alerta de carga
        val loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
        loadingDialog.titleText = "Procesando pago..."
        loadingDialog.setCancelable(false)
        loadingDialog.show()

        btnPagar.isEnabled = false
        progressBar.visibility = View.VISIBLE

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
                    loadingDialog.dismissWithAnimation()

                    if (response.isSuccessful && response.body() != null) {
                        val tokenData = response.body()!!
                        val tokenId = tokenData.id

                        if (tokenId != null) {
                            mostrarExitoTemporal("Token generado correctamente.\nConfirmando reserva...")
                            confirmarReservaConPago(tokenId)
                        } else {
                            mostrarError(tokenData.merchantMessage ?: "No se pudo crear el token.")
                            btnPagar.isEnabled = true
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = parsearErrorCulqi(errorBody)
                        mostrarError(errorMessage)
                        btnPagar.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismissWithAnimation()
                    mostrarError("Error de conexión.\n${e.message}")
                    btnPagar.isEnabled = true
                }
            }
        }
    }

    private fun parsearErrorCulqi(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) return "Error desconocido al procesar el pago."

        return try {
            val json = JSONObject(errorBody)
            val userMessage = json.optString("user_message", "")
            val merchantMessage = json.optString("merchant_message", "")
            val code = json.optString("code", "")

            when (code) {
                "invalid_cvv" -> "CVV inválido.\nDebe tener 3 o 4 dígitos numéricos."
                "invalid_expiration_year" -> "Año de expiración inválido.\nDebe ser de 4 dígitos y no menor al año actual."
                "invalid_expiration_month" -> "Mes de expiración inválido.\nDebe estar entre 01 y 12."
                "invalid_card_number" -> "Número de tarjeta inválido.\nVerifica los datos ingresados."
                "card_lost" -> "Tarjeta reportada como perdida.\nContacta a tu banco."
                "card_stolen" -> "Tarjeta reportada como robada.\nContacta a tu banco."
                "insufficient_funds" -> "Fondos insuficientes.\nIntenta con otra tarjeta."
                "issuer_not_available" -> "Banco no disponible.\nIntenta más tarde."
                "issuer_decline_operation" -> "Operación rechazada por el banco."
                else -> userMessage.ifEmpty { merchantMessage.ifEmpty { "Error al procesar el pago. Código: $code" } }
            }
        } catch (e: Exception) {
            "Error al procesar el pago.\n$errorBody"
        }
    }

    // ✅ Alerta de error
    private fun mostrarError(mensaje: String) {
        val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
        dialog.titleText = "Error"
        dialog.contentText = mensaje
        dialog.confirmText = "Entendido"
        dialog.setConfirmClickListener { it.dismissWithAnimation() }

        // Personalizar para modo oscuro
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }


    private fun mostrarExito(mensaje: String) {
        val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE)
        dialog.titleText = "Pago exitoso"
        dialog.contentText = mensaje
        dialog.confirmText = "Aceptar"
        dialog.setConfirmClickListener {
            it.dismissWithAnimation()

            // Navegar al Fragment de Catálogo
            val catalogoFragment = CatalogoFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor_main, catalogoFragment)
                .commit()
        }

        // Personalizar para modo oscuro
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    // ✅ Alerta de éxito temporal (para etapas intermedias)
    private fun mostrarExitoTemporal(mensaje: String) {
        val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE)
        dialog.titleText = "Éxito"
        dialog.contentText = mensaje
        dialog.confirmText = "OK"
        dialog.setConfirmClickListener { it.dismissWithAnimation() }

        // Personalizar para modo oscuro
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun confirmarReservaConPago(tokenId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fechaSolicitud = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val reserva = Reserva(
                    idUsuario = idUsuario,
                    idHabitacion = idHabitacion,
                    fechaSolicitud = fechaSolicitud,
                    estado = "Confirmada",
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
                    btnPagar.isEnabled = true

                    if (idReserva != -1L) {
                        val habitacionDao = HabitacionDAO(requireContext())
                        habitacionDao.actualizarEstado(idHabitacion, "Reservada")

                        mostrarExito(
                            "¡Pago exitoso!\nReserva confirmada con ID #$idReserva\nToken: $tokenId"
                        )
                    } else {
                        mostrarError(
                            "Pago procesado, pero ocurrió un error al guardar la reserva.\nToken: $tokenId\nPor favor contacta a soporte."
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnPagar.isEnabled = true
                    mostrarError("Error al confirmar reserva.\n${e.message}")
                }
            }
        }
    }
}
