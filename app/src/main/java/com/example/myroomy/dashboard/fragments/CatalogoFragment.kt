package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myroomy.dashboard.adapters.DestacadosAdapter
import com.example.myroomy.dashboard.adapters.HabitacionCatalogoAdapter
import com.example.myroomy.dashboard.database.HabitacionDAO
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.databinding.FragmentCatalogoBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatalogoFragment : Fragment() {

    private var _binding: FragmentCatalogoBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapterCatalogo: HabitacionCatalogoAdapter
    private var listaHabitaciones: List<Habitacion> = emptyList()

    // Estado actual de filtros
    private var listaCategoriasSeleccionadas: List<String> = emptyList()
    private var precioMinSeleccionado: Float = 0f
    private var precioMaxSeleccionado: Float = 1000f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapterCatalogo = HabitacionCatalogoAdapter { habitacion ->
            mostrarDetalleReserva(habitacion)
        }

        binding.recyclerCatalogo.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerCatalogo.adapter = adapterCatalogo

        binding.btnAbrirFiltros.setOnClickListener {
            val sheet = FiltroBottomSheetFragment.newInstance(listaHabitaciones)
            sheet.setOnFiltroAplicadoListener { seleccionados, precioMin, precioMax ->
            aplicarFiltros(seleccionados, precioMin, precioMax)
            }
            sheet.show(parentFragmentManager, "FiltroBottomSheet")
        }

        lifecycleScope.launch {
            val dao = HabitacionDAO(requireContext())
            val habitaciones = withContext(Dispatchers.IO) {
                dao.obtenerTodos()
            }
            // Filtramos solo disponibles desde el inicio
            listaHabitaciones = habitaciones.filter { it.estado == "Disponible" }
            configurarDestacados()
            adapterCatalogo.submitList(listaHabitaciones)
        }
    }

    private fun configurarDestacados() {
        val destacados = listaHabitaciones.filter { it.categoria == "Suite" || it.precio > 200 }
        binding.viewPagerDestacados.adapter = DestacadosAdapter(destacados) { habitacion ->
            mostrarDetalleReserva(habitacion)
        }
    }

    private fun aplicarFiltros(
        seleccionados: List<String>,
        precioMin: Float,
        precioMax: Float
    ) {
        // Guardar estado actual
        listaCategoriasSeleccionadas = seleccionados
        precioMinSeleccionado = precioMin
        precioMaxSeleccionado = precioMax

        val filtrado = listaHabitaciones.filter { hab ->
            (seleccionados.isEmpty() || seleccionados.contains(hab.categoria)) &&
                    (hab.precio in precioMin..precioMax)
        }

        adapterCatalogo.submitList(filtrado)
        actualizarResumenFiltros()
    }

    private fun actualizarResumenFiltros() {
        binding.chipGroupResumen.removeAllViews()

        listaCategoriasSeleccionadas.forEach { categoria ->
            val chip = Chip(requireContext()).apply {
                text = categoria
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    eliminarFiltroCategoria(categoria)
                }
            }
            binding.chipGroupResumen.addView(chip)
        }

        if (precioMinSeleccionado > 0f || precioMaxSeleccionado < 1000f) {
            val chip = Chip(requireContext()).apply {
                text = "Precio: ${precioMinSeleccionado.toInt()} - ${precioMaxSeleccionado.toInt()}"
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    eliminarFiltroPrecio()
                }
            }
            binding.chipGroupResumen.addView(chip)
        }

        binding.contadorFiltros.text = binding.chipGroupResumen.childCount.toString()
    }

    private fun eliminarFiltroCategoria(categoria: String) {
        val nuevasCategorias = listaCategoriasSeleccionadas.toMutableList()
        nuevasCategorias.remove(categoria)
        aplicarFiltros(nuevasCategorias, precioMinSeleccionado, precioMaxSeleccionado)
    }

    private fun eliminarFiltroPrecio() {
        aplicarFiltros(listaCategoriasSeleccionadas, 0f, 1000f)
    }

    private fun mostrarDetalleReserva(habitacion: Habitacion) {
        val bottomSheet = DetalleReservaBottomSheet.nuevaInstancia(habitacion)
        bottomSheet.show(parentFragmentManager, "DetalleReserva")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
