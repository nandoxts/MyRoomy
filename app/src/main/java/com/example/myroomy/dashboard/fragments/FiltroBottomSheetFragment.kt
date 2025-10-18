package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.databinding.BottomsheetFiltrosBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class FiltroBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomsheetFiltrosBinding? = null
    private val binding get() = _binding!!

    private var habitaciones: List<Habitacion> = emptyList()
    private var onFiltroAplicado: ((List<String>, Float, Float) -> Unit)? = null

    companion object {
        fun newInstance(habitaciones: List<Habitacion>): FiltroBottomSheetFragment {
            val fragment = FiltroBottomSheetFragment()
            fragment.habitaciones = habitaciones
            return fragment
        }
    }

    fun setOnFiltroAplicadoListener(listener: (List<String>, Float, Float) -> Unit) {
        onFiltroAplicado = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetFiltrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categorias = habitaciones.map { it.categoria }.distinct()
        categorias.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = cat
                isCheckable = true
            }
            binding.chipGroupFiltros.addView(chip)
        }

        binding.sliderPrecio.valueFrom = 0f
        binding.sliderPrecio.valueTo = 1000f
        binding.sliderPrecio.values = listOf(0f, 1000f)

        binding.btnAplicar.setOnClickListener {
            val seleccionados = binding.chipGroupFiltros.checkedChipIds.mapNotNull { id ->
                binding.chipGroupFiltros.findViewById<Chip>(id)?.text?.toString()
            }

            val precioMin = binding.sliderPrecio.values[0]
            val precioMax = binding.sliderPrecio.values[1]

            onFiltroAplicado?.invoke(seleccionados, precioMin, precioMax)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
