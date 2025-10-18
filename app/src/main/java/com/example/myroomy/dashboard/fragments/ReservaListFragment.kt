package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myroomy.R
import com.example.myroomy.dashboard.adapters.ReservaAdapter
import com.example.myroomy.dashboard.database.HabitacionDAO
import com.example.myroomy.dashboard.database.ReservaDAO

class ReservaListFragment : Fragment() {

    private lateinit var dao: ReservaDAO
    private lateinit var habitacionDao: HabitacionDAO
    private lateinit var recyclerView: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var layoutVacio: LinearLayout // El LinearLayout para el mensaje
    private lateinit var txtSinHabitaciones: TextView // El TextView dentro del LinearLayout
    private var estadoFiltro: String = "Pendiente"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dao = ReservaDAO(requireContext())
        habitacionDao = HabitacionDAO(requireContext())
        val view = inflater.inflate(R.layout.fragment_reserva_list, container, false)
        recyclerView = view.findViewById(R.id.recyclerReservas)
        recyclerView.layoutManager = LinearLayoutManager(context)
        spinner = view.findViewById(R.id.spinnerFiltro)
        layoutVacio = view.findViewById(R.id.layoutVacio) // Inicializamos el LinearLayout
        txtSinHabitaciones = view.findViewById(R.id.txtSinHabitaciones) // Inicializamos el TextView

        val filtros = listOf("Pendiente", "Aceptada", "Rechazada", "Todas")
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, filtros)

        spinner.setSelection(0)
        spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
                estadoFiltro = filtros[position]
                cargarReservas()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        })

        cargarReservas() // Cargar las reservas inicialmente con el filtro por defecto

        return view
    }

    private fun cargarReservas() {
        val lista = when (estadoFiltro) {
            "Todas" -> dao.listarTodas()
            else -> dao.listarPorEstado(estadoFiltro)
        }

        // Cambiar el texto del TextView según el estadoFiltro
        when (estadoFiltro) {
            "Pendiente" -> txtSinHabitaciones.text = "No hay reservas pendientes"
            "Aceptada" -> txtSinHabitaciones.text = "No hay reservas aceptadas"
            "Rechazada" -> txtSinHabitaciones.text = "No hay reservas rechazadas"
            "Todas" -> txtSinHabitaciones.text = "No hay reservas registradas"
            else -> txtSinHabitaciones.text = "No hay reservas"
        }

        // Verificar si la lista está vacía
        if (lista.isEmpty()) {
            recyclerView.visibility = View.GONE
            layoutVacio.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            layoutVacio.visibility = View.GONE
            recyclerView.adapter = ReservaAdapter(lista) { reserva, accion ->
                if (accion == "aceptar") {
                    dao.actualizarEstado(reserva.id, "Aceptada")
                    habitacionDao.actualizarEstado(reserva.idHabitacion, "Ocupada")
                } else if (accion == "rechazar") {
                    dao.actualizarEstado(reserva.id, "Rechazada")
                    habitacionDao.actualizarEstado(reserva.idHabitacion, "Disponible")
                }
                cargarReservas() // Recargar las reservas después de actualizar
            }
        }
    }

}
