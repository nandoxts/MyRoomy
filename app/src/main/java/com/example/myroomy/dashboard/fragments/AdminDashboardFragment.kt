package com.example.myroomy.dashboard.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.myroomy.R
import com.example.myroomy.dashboard.database.HabitacionDAO
import com.example.myroomy.dashboard.database.UsuarioDAO
import com.example.myroomy.databinding.FragmentAdminDashboardBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            val daoHabitacion = HabitacionDAO(requireContext())
            val daoUsuario = UsuarioDAO(requireContext())

            val habitacionesDeferred = async(Dispatchers.IO) { daoHabitacion.obtenerTodos() }
            val habitacionesOcupadasDeferred = async(Dispatchers.IO) { daoHabitacion.obtenerPorEstado("Ocupada") }
            val usuariosDeferred = async(Dispatchers.IO) { daoUsuario.obtenerTodos() }

            val habitaciones = habitacionesDeferred.await()
            val habitacionesOcupadas = habitacionesOcupadasDeferred.await()
            val usuarios = usuariosDeferred.await()

            Log.d("Dashboard", "Habitaciones: ${habitaciones.size}, Ocupadas: ${habitacionesOcupadas.size}, Usuarios: ${usuarios.size}")

            binding.txtHabitacionesCount.text = habitaciones.size.toString()
            binding.txtReservasCount.text = habitacionesOcupadas.size.toString()
            binding.txtUsuariosCount.text = usuarios.size.toString()

            val total = habitaciones.size.takeIf { it > 0 } ?: 1
            val porcentajeOcupacion = (habitacionesOcupadas.size * 100f) / total

            binding.txtOcupacionLabel.text = "Ocupación: %.1f%%".format(porcentajeOcupacion)

            setupPieChart(porcentajeOcupacion)
        }
    }

    private fun setupPieChart(porcentaje: Float) {
        val entries = listOf(
            PieEntry(porcentaje, "Ocupadas"),
            PieEntry(100f - porcentaje, "Libres")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.red), // Ocupadas
                ContextCompat.getColor(requireContext(), R.color.green) // Libres
            )
            valueTextColor = Color.WHITE
            valueTextSize = 14f
            sliceSpace = 2f
        }

        val data = PieData(dataSet)

        binding.pieChartOcupacion.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            setUsePercentValues(true)
            setHoleColor(Color.TRANSPARENT)
            setCenterText("${porcentaje.toInt()}%")
            setCenterTextSize(18f)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
