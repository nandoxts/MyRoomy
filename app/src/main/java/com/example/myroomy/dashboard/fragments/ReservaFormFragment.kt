package com.example.myroomy.dashboard.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.myroomy.databinding.FragmentReservaFormBinding
import com.example.myroomy.dashboard.database.ReservaDAO
import com.example.myroomy.dashboard.models.Reserva
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.widget.ArrayAdapter
import com.example.myroomy.R
import com.example.myroomy.dashboard.database.HabitacionDAO

class ReservaFormFragment : Fragment() {

    private lateinit var binding: FragmentReservaFormBinding
    private lateinit var reservaDAO: ReservaDAO
    private var habitacionId: Int = -1
    private var precioPorNoche: Double = 0.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentReservaFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        reservaDAO = ReservaDAO(requireContext())

        // Recibir argumentos
        arguments?.let {
            habitacionId = it.getInt("habitacion_id")
            precioPorNoche = it.getDouble("precio")
        }

        // Configurar MaterialAutoCompleteTextView: Cantidad de personas
        val cantidadAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, (1..6).map { it.toString() })
        binding.inputCantidadPersonas.setAdapter(cantidadAdapter)

        // Configurar MaterialAutoCompleteTextView: Método de pago
        val metodoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, listOf("Tarjeta (Culqi)", "Efectivo", "Yape/Plin"))
        binding.inputMetodoPago.setAdapter(metodoAdapter)

        // Configurar datepickers
        val dateListener = { target: View ->
            val calendar = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                val fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
                (target as? com.google.android.material.textfield.TextInputEditText)?.setText(fecha)
                calcularTotal()
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        binding.inputFechaInicio.setOnClickListener { dateListener(it) }
        binding.inputFechaFin.setOnClickListener { dateListener(it) }

        binding.btnConfirmarReserva.setOnClickListener {
            procederConReserva()
        }
    }

    private fun calcularTotal(): Double {
        val inicio = binding.inputFechaInicio.text.toString()
        val fin = binding.inputFechaFin.text.toString()
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val startDate = sdf.parse(inicio)
            val endDate = sdf.parse(fin)
            val diffDays = TimeUnit.DAYS.convert(endDate.time - startDate.time, TimeUnit.MILLISECONDS)
            val noches = if (diffDays <= 0L) 1 else diffDays
            val total = noches * precioPorNoche
            binding.txtTotal.text = "Total: S/ %.2f".format(total)
            total
        } catch (e: Exception) {
            binding.txtTotal.text = "Total: S/ 0.00"
            0.0
        }
    }

    private fun procederConReserva() {
        val dni = binding.inputDNICliente.text.toString().trim()
        val fechaInicio = binding.inputFechaInicio.text.toString().trim()
        val fechaFin = binding.inputFechaFin.text.toString().trim()
        val cantidadPersonas = binding.inputCantidadPersonas.text.toString().toIntOrNull()
        val metodoPago = binding.inputMetodoPago.text.toString()
        val comentario = binding.inputComentario.text.toString().trim()
        val total = calcularTotal()

        if (dni.isBlank() || fechaInicio.isBlank() || fechaFin.isBlank() || cantidadPersonas == null || metodoPago.isBlank()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = requireContext().getSharedPreferences("MyRoomyPrefs", 0)
        val idUsuario = prefs.getInt("usuario_id", -1)

        if (idUsuario == -1) {
            Toast.makeText(requireContext(), "Error: no se pudo obtener el usuario", Toast.LENGTH_SHORT).show()
            return
        }

        // ✅ Verificar el método de pago seleccionado
        when (metodoPago) {
            "Tarjeta (Culqi)" -> {
                // Ir al PagoFragment
                irAPagoFragment(idUsuario, fechaInicio, fechaFin, cantidadPersonas, comentario, total)
            }
            else -> {
                // Confirmar reserva directamente (Efectivo, Yape, etc.)
                confirmarReservaSinPago(idUsuario, fechaInicio, fechaFin, cantidadPersonas, metodoPago, comentario, total)
            }
        }
    }

    private fun irAPagoFragment(
        idUsuario: Int,
        fechaInicio: String,
        fechaFin: String,
        cantidadPersonas: Int,
        comentario: String,
        total: Double
    ) {
        val pagoFragment = PagoFragment()
        val bundle = Bundle().apply {
            putInt("usuario_id", idUsuario)
            putInt("habitacion_id", habitacionId)
            putString("fecha_inicio", fechaInicio)
            putString("fecha_fin", fechaFin)
            putInt("cantidad_personas", cantidadPersonas)
            putString("comentario", comentario)
            putDouble("total", total)
        }
        pagoFragment.arguments = bundle

        parentFragmentManager.beginTransaction()  // ✅ Cambio aquí
            .replace(R.id.contenedor_main, pagoFragment)  // ✅ Cambio aquí
            .addToBackStack(null)
            .commit()
    }

    private fun confirmarReservaSinPago(
        idUsuario: Int,
        fechaInicio: String,
        fechaFin: String,
        cantidadPersonas: Int,
        metodoPago: String,
        comentario: String,
        total: Double
    ) {
        val fechaSolicitud = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val reserva = Reserva(
            idUsuario = idUsuario,
            idHabitacion = habitacionId,
            fechaSolicitud = fechaSolicitud,
            estado = "Pendiente",
            comentario = comentario,
            fechaIngreso = fechaInicio,
            fechaSalida = fechaFin,
            total = total,
            metodoPago = metodoPago,
            cantidadPersonas = cantidadPersonas
        )

        val idReserva = reservaDAO.insertar(reserva)
        if (idReserva != -1L) {
            val habitacionDao = HabitacionDAO(requireContext())
            habitacionDao.actualizarEstado(habitacionId, "Reservada")

            Toast.makeText(requireContext(), "Reserva registrada con éxito", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        } else {
            Toast.makeText(requireContext(), "Error al registrar la reserva", Toast.LENGTH_SHORT).show()
        }
    }
}