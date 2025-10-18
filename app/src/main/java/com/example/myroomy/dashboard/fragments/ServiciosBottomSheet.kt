package com.example.myroomy.dashboard.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.myroomy.databinding.BottomsheetServiciosBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class ServiciosBottomSheet(
    private val serviciosExistentes: List<String>,
    private val onServicioAgregado: (String) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomsheetServiciosBinding? = null
    private val binding get() = _binding!!

    private val sugerenciasServicios = listOf(
        "TV", "Wifi", "Jacuzzi", "Minibar", "Balcón", "Escritorio", "Cuna"
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        _binding = BottomsheetServiciosBinding.inflate(LayoutInflater.from(context))
        dialog.setContentView(binding.root)

        cargarChips()

        binding.btnAgregarPersonalizado.setOnClickListener {
            val servicio = binding.inputServicioPersonalizado.text.toString().trim()
            if (servicio.isNotEmpty()) {
                if (serviciosExistentes.any { it.equals(servicio, ignoreCase = true) }) {
                    Toast.makeText(requireContext(), "Ya agregado", Toast.LENGTH_SHORT).show()
                } else {
                    onServicioAgregado(servicio)
                    dismiss()
                }
            } else {
                Toast.makeText(requireContext(), "Escribe un servicio", Toast.LENGTH_SHORT).show()
            }
        }

        return dialog
    }

    private fun cargarChips() {
        val disponibles = sugerenciasServicios.filterNot { s ->
            serviciosExistentes.any { it.equals(s, ignoreCase = true) }
        }

        if (disponibles.isEmpty()) {
            binding.chipGroupSugerencias.addView(Chip(requireContext()).apply {
                text = "Sin sugerencias disponibles"
                isEnabled = false
            })
        } else {
            disponibles.forEach { servicio ->
                val chip = Chip(requireContext()).apply {
                    text = servicio
                    isClickable = true
                    isCheckable = false
                    setOnClickListener {
                        onServicioAgregado(servicio)
                        dismiss()
                    }
                }
                binding.chipGroupSugerencias.addView(chip)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
