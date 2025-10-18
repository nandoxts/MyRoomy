package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager

import com.example.myroomy.dashboard.adapters.HabitacionAdapter
import com.example.myroomy.dashboard.database.HabitacionDAO
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.dashboard.utils.FragmentHelper
import com.example.myroomy.databinding.FragmentAdminHabitacionListBinding

class HabitacionListFragment : Fragment() {

    private var _binding: FragmentAdminHabitacionListBinding? = null
    private val binding get() = _binding!!

    private lateinit var habitacionDAO: HabitacionDAO
    private lateinit var adapter: HabitacionAdapter
    private val listaHabitaciones = mutableListOf<Habitacion>()
    private lateinit var spinnerFiltroHabitacion: Spinner

    private var estadoFiltro: String = "Todas"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHabitacionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitacionDAO = HabitacionDAO(requireContext())

        // Configurar el Spinner
        spinnerFiltroHabitacion = binding.spinnerFiltroHabitacion
        val filtros = listOf("Todas", "Ocupada", "Disponible","Reservada","Mantenimiento")  // Puedes agregar más filtros si lo necesitas
        val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filtros)
        spinnerFiltroHabitacion.adapter = adapterSpinner

        spinnerFiltroHabitacion.setSelection(0)
        spinnerFiltroHabitacion.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                estadoFiltro = filtros[position]
                cargarHabitaciones()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        adapter = HabitacionAdapter(
            listaHabitaciones,
            onItemClick = { habitacionSeleccionada ->
                FragmentHelper.replaceWithSmoothSlide(
                    requireActivity() as AppCompatActivity,
                    HabitacionFormFragment.nuevaInstancia(habitacionSeleccionada)
                )
            },
            onEliminarClick = { habitacion ->
                confirmarEliminacion(habitacion)
            }
        )

        binding.recyclerHabitaciones.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerHabitaciones.adapter = adapter

        binding.btnAgregarHabitacion.setOnClickListener {
            FragmentHelper.replaceWithSmoothSlide(
                requireActivity() as AppCompatActivity,
                HabitacionFormFragment.nuevaInstancia()
            )
        }

        cargarHabitaciones()
    }

    override fun onResume() {
        super.onResume()
        cargarHabitaciones()
    }

    // Lógica para cargar las habitaciones con el filtro seleccionado
    private fun cargarHabitaciones() {
        listaHabitaciones.clear()

        // Filtrar según el estado seleccionado en el Spinner
        val habitacionesFiltradas = when (estadoFiltro) {
            "Ocupada" -> habitacionDAO.obtenerPorEstado("Ocupada")
            "Disponible" -> habitacionDAO.obtenerPorEstado("Disponible")
            "Reservada" -> habitacionDAO.obtenerPorEstado("Reservada")
            "Mantenimiento" -> habitacionDAO.obtenerPorEstado("Mantenimiento")
            else -> habitacionDAO.obtenerTodos()
        }

        listaHabitaciones.addAll(habitacionesFiltradas)
        adapter.notifyDataSetChanged()
        verificarListaVacia()
    }


    private fun confirmarEliminacion(habitacion: Habitacion) {
        AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar habitación?")
            .setMessage("¿Estás seguro de eliminar '${habitacion.nombre}'?")
            .setPositiveButton("Sí") { _, _ -> eliminarHabitacion(habitacion) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarHabitacion(habitacion: Habitacion) {
        val filasEliminadas = habitacionDAO.eliminar(habitacion.id)
        if (filasEliminadas > 0) {
            // Buscar por ID para mayor seguridad
            val index = listaHabitaciones.indexOfFirst { it.id == habitacion.id }
            if (index != -1) {
                listaHabitaciones.removeAt(index)
                adapter.notifyItemRemoved(index)
                verificarListaVacia()
                Toast.makeText(requireContext(), "Habitación eliminada", Toast.LENGTH_SHORT).show()
            } else {
                // Refrescar por si acaso no se encontró
                cargarHabitaciones()
            }
        } else {
            Toast.makeText(requireContext(), "Error al eliminar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun verificarListaVacia() {
        if (listaHabitaciones.isEmpty()) {
            binding.recyclerHabitaciones.visibility = View.GONE
            binding.layoutVacio.visibility = View.VISIBLE
        } else {
            binding.recyclerHabitaciones.visibility = View.VISIBLE
            binding.layoutVacio.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
