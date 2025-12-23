package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.util.Log
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

    companion object {
        private const val TAG = "PagoFragment"
    }    private val PUBLIC_KEY = "pk_test_K9z4TGDS15roIg2H"

    private lateinit var edtCardNumber: EditText
    private lateinit var edtMonth: EditText
    private lateinit var edtYear: EditText
    private lateinit var edtCvv: EditText
    private lateinit var edtEmail: EditText
    private lateinit var btnPagar: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var txtMontoTotal: TextView

    private var loadingDialog: SweetAlertDialog? = null
    
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
        // LIMPIAR Y SANITIZAR COMPLETAMENTE
        var cardNumber = edtCardNumber.text.toString().trim().replace(" ", "").replace("-", "")
        val cvv = edtCvv.text.toString().trim()
        val expMonth = edtMonth.text.toString().trim().padStart(2, '0')  // Asegurar 2 dígitos
        val expYear = edtYear.text.toString().trim()
        val email = edtEmail.text.toString().trim().lowercase()  // Normalizar email
        
        Log.d(TAG, "INPUT RAW:")
        Log.d(TAG, "Card Raw: '${edtCardNumber.text.toString()}'")
        Log.d(TAG, "Card After Trim: '${edtCardNumber.text.toString().trim()}'")
        Log.d(TAG, "Card After Clean: '$cardNumber'")
        Log.d(TAG, "Card Bytes: ${cardNumber.toByteArray().joinToString(",") { it.toString() }}")

        // Validaciones
        when {
            cardNumber.isEmpty() || cvv.isEmpty() || expMonth.isEmpty() ||
                    expYear.isEmpty() || email.isEmpty() -> {
                mostrarError("Por favor completa todos los campos obligatorios.")
                return
            }

            !cardNumber.all { it.isDigit() } -> {
                mostrarError("Número de tarjeta contiene caracteres no válidos.\nSolo se aceptan números.")
                return
            }

            cardNumber.length < 13 || cardNumber.length > 19 -> {
                mostrarError("Número de tarjeta inválido.\nDebe tener entre 13 y 19 dígitos.")
                return
            }

            !cvv.all { it.isDigit() } || cvv.length < 3 || cvv.length > 4 -> {
                mostrarError("CVV inválido.\nDebe tener 3 o 4 dígitos numéricos.")
                return
            }

            expMonth.toIntOrNull() == null || expMonth.toInt() !in 1..12 -> {
                mostrarError("Mes inválido.\nDebe estar entre 01 y 12.")
                return
            }

            !expYear.all { it.isDigit() } || expYear.length != 4 || expYear.toInt() < Calendar.getInstance().get(Calendar.YEAR) -> {
                mostrarError("Año inválido.\nDebe ser de 4 dígitos y no menor al año actual.")
                return
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                mostrarError("Correo electrónico inválido.\nPor favor ingresa uno válido.")
                return
            }
        }

        btnPagar.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            // Mostrar alerta de carga (evitar duplicados)
            withContext(Dispatchers.Main) {
                if (loadingDialog?.isShowing != true) {
                    loadingDialog = SweetAlertDialog(requireContext(), SweetAlertDialog.PROGRESS_TYPE)
                    loadingDialog?.titleText = "Procesando pago..."
                    loadingDialog?.contentText = "Conectando con Culqi..."
                    loadingDialog?.setCancelable(false)
                    loadingDialog?.show()
                }
            }
            
            try {
                // Asegurar que el número de tarjeta esté limpio
                val cleanCardNumber = cardNumber.trim()
                
                Log.d(TAG, "DATOS ENVIADOS A CULQI:")
                Log.d(TAG, "Card: ${cleanCardNumber.take(6)}...${cleanCardNumber.takeLast(4)}")
                Log.d(TAG, "Exp: $expMonth/$expYear")
                Log.d(TAG, "Email: $email")
                Log.d(TAG, "Card Length: ${cleanCardNumber.length}")
                Log.d(TAG, "Card is numeric: ${cleanCardNumber.all { it.isDigit() }}")
                Log.d(TAG, "API Key: ${PUBLIC_KEY.take(7)}...${PUBLIC_KEY.takeLast(4)}")
                
                // Crear header de autorización Bearer
                val authHeader = "Bearer $PUBLIC_KEY"  // Formato correcto de Culqi
                Log.d(TAG, "Auth Header: Bearer [clave]")
                
                val request = CulqiTokenRequest(
                    cardNumber = cleanCardNumber,
                    cvv = cvv,
                    expirationMonth = expMonth,
                    expirationYear = expYear,
                    email = email
                )
                
                Log.d(TAG, "REQUEST A CULQI:")
                Log.d(TAG, "Card: '$cleanCardNumber' (length: ${cleanCardNumber.length})")
                Log.d(TAG, "Card HEX: ${cleanCardNumber.toByteArray().joinToString(",") { it.toString() }}")
                Log.d(TAG, "CVV: '$cvv'")
                Log.d(TAG, "Month: '$expMonth'")
                Log.d(TAG, "Year: '$expYear'")
                Log.d(TAG, "Email: '$email'")

                val response = RetrofitClient.culqiService.createToken(
                    authorization = authHeader,  // HTTP Basic Auth: "Basic base64(clave:)"
                    request = request
                )

                withContext(Dispatchers.Main) {
                    loadingDialog?.dismissWithAnimation()
                    loadingDialog = null

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
                    loadingDialog?.dismissWithAnimation()
                    loadingDialog = null
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

    // Alerta de error
    private fun mostrarExito(mensaje: String) {
        val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE)
        dialog.titleText = "Pago Exitoso!"
        dialog.contentText = mensaje
        dialog.confirmText = "Ir al inicio"
        dialog.setConfirmClickListener {
            it.dismissWithAnimation()
            loadingDialog = null  // Limpiar

            // Navegar al Fragment de Catálogo
            val catalogoFragment = CatalogoFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.contenedor_main, catalogoFragment)
                .commitNow()
        }

        // Personalizar para modo oscuro
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun mostrarError(mensaje: String) {
        val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.ERROR_TYPE)
        dialog.titleText = "Error"
        dialog.contentText = mensaje
        dialog.confirmText = "Reintentar"
        dialog.setConfirmClickListener {
            it.dismissWithAnimation()
            loadingDialog = null
            btnPagar.isEnabled = true
        }

        // Personalizar para modo oscuro
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun mostrarExitoTemporal(mensaje: String) {
        val dialog = SweetAlertDialog(requireContext(), SweetAlertDialog.SUCCESS_TYPE)
        dialog.titleText = "Confirmando..."
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
                    btnPagar.isEnabled = true

                    if (idReserva != -1L) {
                        val habitacionDao = HabitacionDAO(requireContext())
                        habitacionDao.actualizarEstado(idHabitacion, "Reservada")

                        mostrarExito(
                            "Reserva confirmada!\nID: #$idReserva\nEdificio actualizado.\nProximas confirmaciones via email."
                        )
                    } else {
                        mostrarError(
                            "Pago procesado, pero ocurrió un error al guardar la reserva.\nToken: $tokenId\nPor favor contacta a soporte."
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnPagar.isEnabled = true
                    mostrarError("Error al confirmar reserva.\n${e.message}")
                }
            }
        }
    }
}
