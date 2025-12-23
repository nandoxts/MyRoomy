package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myroomy.BuildConfig
import com.example.myroomy.R
import com.example.myroomy.data.api.RetrofitClient
import com.example.myroomy.data.api.SendGridClient
import com.example.myroomy.data.models.CulqiTokenRequest
import com.example.myroomy.data.models.Content
import com.example.myroomy.data.models.EmailAddress
import com.example.myroomy.data.models.Personalization
import com.example.myroomy.data.models.SendGridEmailRequest
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
    }

    private lateinit var apiKey: String

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

        // Cargar API Key desde BuildConfig (inyectada desde local.properties)
        apiKey = BuildConfig.CULQI_PUBLIC_KEY

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

            // Validar que la tarjeta no esté vencida (mes/año)
            {
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
                val cardYear = expYear.toInt()
                val cardMonth = expMonth.toInt()
                
                if (cardYear == currentYear && cardMonth < currentMonth) {
                    mostrarError("Tarjeta vencida.\nVerifica el mes y año de expiración.")
                    return
                }
                
                if (cardYear > currentYear + 20) {
                    mostrarError("Año de expiración fuera de rango.\nVerifica que sea correcto.")
                    return
                }
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

                        // Enviar email de confirmación de forma asíncrona (no bloquear UI)
                        enviarEmailConfirmacion(
                            email = edtEmail.text.toString().trim(),
                            idReserva = idReserva.toString()
                        )

                        mostrarExito(
                            "Tu reserva está confirmada\nID: #$idReserva\nRevisa tu email para detalles"
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

    private fun enviarEmailConfirmacion(email: String, idReserva: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.SENDGRID_API_KEY
                val fromEmail = BuildConfig.SENDGRID_FROM_EMAIL
                val fromName = BuildConfig.SENDGRID_FROM_NAME

                // Obtener nombre de habitación desde base de datos
                val habitacionDao = HabitacionDAO(requireContext())
                val habitacion = habitacionDao.obtenerPorId(idHabitacion)
                val nombreHabitacion = habitacion?.nombre ?: "Habitación $idHabitacion"

                val htmlContent = construirHtmlConfirmacion(
                    nombreHabitacion = nombreHabitacion,
                    idReserva = idReserva,
                    fechaCheckIn = fechaInicio,
                    fechaCheckOut = fechaFin,
                    montoTotal = String.format("%.2f", total)
                )

                val request = SendGridEmailRequest(
                    personalizations = listOf(
                        Personalization(
                            to = listOf(EmailAddress(email = email)),
                            subject = "Confirmación de Reserva - My Roomy"
                        )
                    ),
                    from = EmailAddress(email = fromEmail, name = fromName),
                    subject = "Confirmación de Reserva - My Roomy",
                    content = listOf(
                        Content(type = "text/html", value = htmlContent)
                    )
                )

                val authHeader = "Bearer $apiKey"
                val response = SendGridClient.sendGridService.sendEmail(authHeader, request)

                if (response.isSuccessful) {
                    Log.d("SendGrid", "Email enviado exitosamente a $email")
                } else {
                    Log.e("SendGrid", "Error enviando email: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SendGrid", "Excepción al enviar email: ${e.message}")
            }
        }
    }

    private fun construirHtmlConfirmacion(
        nombreHabitacion: String,
        idReserva: String,
        fechaCheckIn: String,
        fechaCheckOut: String,
        montoTotal: String
    ): String {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Confirmación de Reserva - My Roomy</title>
                <style>
                    * {
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    
                    body {
                        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        padding: 20px;
                        color: #333;
                    }
                    
                    .container {
                        max-width: 600px;
                        margin: 0 auto;
                        background: white;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
                    }
                    
                    .header {
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                    }
                    
                    .header h1 {
                        font-size: 28px;
                        font-weight: 700;
                        margin-bottom: 8px;
                    }
                    
                    .header p {
                        font-size: 14px;
                        opacity: 0.95;
                        font-weight: 300;
                    }
                    
                    .check-icon {
                        display: inline-block;
                        width: 60px;
                        height: 60px;
                        background: rgba(255, 255, 255, 0.2);
                        border-radius: 50%;
                        margin-bottom: 15px;
                        font-size: 30px;
                        line-height: 60px;
                    }
                    
                    .content {
                        padding: 40px 30px;
                    }
                    
                    .welcome-text {
                        font-size: 16px;
                        color: #555;
                        margin-bottom: 30px;
                        line-height: 1.6;
                    }
                    
                    .reservation-info {
                        background: #f8f9ff;
                        border-left: 4px solid #667eea;
                        padding: 20px;
                        border-radius: 8px;
                        margin-bottom: 30px;
                    }
                    
                    .info-row {
                        display: flex;
                        justify-content: space-between;
                        align-items: center;
                        padding: 12px 0;
                        border-bottom: 1px solid #e0e0ff;
                    }
                    
                    .info-row:last-child {
                        border-bottom: none;
                    }
                    
                    .info-label {
                        font-weight: 600;
                        color: #667eea;
                        font-size: 13px;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    
                    .info-value {
                        color: #333;
                        font-size: 15px;
                        font-weight: 500;
                    }
                    
                    .divider {
                        height: 1px;
                        background: linear-gradient(90deg, transparent, #ddd, transparent);
                        margin: 30px 0;
                    }
                    
                    .payment-section {
                        text-align: center;
                        padding: 20px;
                        background: linear-gradient(135deg, #f5f7ff 0%, #f0f4ff 100%);
                        border-radius: 10px;
                        margin-bottom: 30px;
                    }
                    
                    .payment-label {
                        font-size: 12px;
                        color: #888;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        margin-bottom: 10px;
                    }
                    
                    .payment-amount {
                        font-size: 38px;
                        font-weight: 700;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        background-clip: text;
                    }
                    
                    .booking-reference {
                        background: #f0f4ff;
                        padding: 15px;
                        border-radius: 8px;
                        margin-bottom: 25px;
                        text-align: center;
                    }
                    
                    .reference-label {
                        font-size: 11px;
                        color: #999;
                        text-transform: uppercase;
                        letter-spacing: 1px;
                        display: block;
                        margin-bottom: 5px;
                    }
                    
                    .reference-code {
                        font-size: 20px;
                        font-weight: 700;
                        color: #667eea;
                        font-family: 'Courier New', monospace;
                        letter-spacing: 2px;
                    }
                    
                    .footer-message {
                        background: #f9f9f9;
                        padding: 20px;
                        border-radius: 8px;
                        margin-bottom: 25px;
                        border-left: 4px solid #764ba2;
                    }
                    
                    .footer-message p {
                        font-size: 13px;
                        color: #666;
                        line-height: 1.6;
                        margin: 0;
                    }
                    
                    .cta-button {
                        display: inline-block;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        color: white;
                        padding: 14px 40px;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        font-size: 14px;
                        margin-bottom: 30px;
                        transition: transform 0.2s;
                    }
                    
                    .cta-button:hover {
                        text-decoration: none;
                        color: white;
                    }
                    
                    .footer {
                        background: #f5f5f5;
                        padding: 25px 30px;
                        text-align: center;
                        border-top: 1px solid #e5e5e5;
                    }
                    
                    .footer-text {
                        font-size: 12px;
                        color: #999;
                        line-height: 1.8;
                        margin: 0;
                    }
                    
                    .social-icons {
                        margin-top: 15px;
                        font-size: 14px;
                    }
                    
                    .social-icons a {
                        color: #667eea;
                        text-decoration: none;
                        margin: 0 10px;
                        font-weight: 600;
                    }
                    
                    @media only screen and (max-width: 600px) {
                        .container {
                            border-radius: 0;
                        }
                        
                        .header {
                            padding: 30px 20px;
                        }
                        
                        .header h1 {
                            font-size: 24px;
                        }
                        
                        .content {
                            padding: 25px 20px;
                        }
                        
                        .payment-amount {
                            font-size: 32px;
                        }
                        
                        .info-row {
                            flex-direction: column;
                            align-items: flex-start;
                        }
                        
                        .info-value {
                            margin-top: 5px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <!-- Header -->
                    <div class="header">
                        <div class="check-icon">✓</div>
                        <h1>Confirmación de Reserva</h1>
                        <p>Tu hogar temporal te espera</p>
                    </div>
                    
                    <!-- Content -->
                    <div class="content">
                        <p class="welcome-text">
                            ¡Excelente! Tu reserva ha sido confirmada exitosamente. Estamos entusiasmados de recibirte en My Roomy. A continuación encontrarás los detalles de tu hospedaje.
                        </p>
                        
                        <!-- Booking Reference -->
                        <div class="booking-reference">
                            <span class="reference-label">Número de Reserva</span>
                            <span class="reference-code">#$idReserva</span>
                        </div>
                        
                        <!-- Reservation Details -->
                        <div class="reservation-info">
                            <div class="info-row">
                                <span class="info-label">🏠 Habitación</span>
                                <span class="info-value">$nombreHabitacion</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">📅 Check-in</span>
                                <span class="info-value">$fechaCheckIn</span>
                            </div>
                            <div class="info-row">
                                <span class="info-label">📅 Check-out</span>
                                <span class="info-value">$fechaCheckOut</span>
                            </div>
                        </div>
                        
                        <div class="divider"></div>
                        
                        <!-- Payment Section -->
                        <div class="payment-section">
                            <p class="payment-label">Monto Pagado</p>
                            <p class="payment-amount">S/ $montoTotal</p>
                        </div>
                        
                        <!-- Support Message -->
                        <div class="footer-message">
                            <p><strong>¿Necesitas ayuda?</strong> Si tienes preguntas sobre tu reserva, cambios o necesitas asistencia, nuestro equipo está disponible a través de la app para atenderte.</p>
                        </div>
                        
                        <center>
                            <a href="https://myroomy.app/reservas" class="cta-button">Ver mis Reservas</a>
                        </center>
                    </div>
                    
                    <!-- Footer -->
                    <div class="footer">
                        <p class="footer-text">
                            <strong>My Roomy</strong><br>
                            Tu plataforma de hospedaje confiable<br>
                            © 2024-2025. Todos los derechos reservados.
                        </p>
                        <div class="social-icons">
                            <a href="#">Centro de Ayuda</a> | 
                            <a href="#">Contacto</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }
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
