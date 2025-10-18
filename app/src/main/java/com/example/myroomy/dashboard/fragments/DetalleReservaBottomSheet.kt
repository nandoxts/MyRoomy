package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.myroomy.R
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.databinding.BottomsheetDetalleReservaBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import java.io.File

class DetalleReservaBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetDetalleReservaBinding? = null
    private val binding get() = _binding!!

    private var habitacion: Habitacion? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        habitacion = arguments?.getSerializable("habitacion") as? Habitacion
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetDetalleReservaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitacion?.let { hab ->
            binding.txtNombre.text = hab.nombre
            binding.txtCategoria.text = hab.categoria
            binding.txtPrecio.text = "S/ ${hab.precio}"
            binding.txtEstado.text = hab.estado
            binding.txtDescripcion.text = hab.descripcion

            Glide.with(this)
                .load(
                    if (hab.imagen.startsWith("http")) hab.imagen
                    else File(hab.imagen)
                )
                .placeholder(R.drawable.ic_room)
                .into(binding.imgDetalle)


            // Mostrar los servicios como chips
            binding.chipGroupServicios.removeAllViews()
            hab.servicios.forEach { servicio ->
                val chip = Chip(requireContext()).apply {
                    text = servicio
                    isClickable = false
                    isCheckable = false
                }
                binding.chipGroupServicios.addView(chip)
            }

            binding.btnSolicitarReserva.setOnClickListener {
                dismiss()

                // Abrir el formulario de reserva
                val bundle = Bundle().apply {
                    putInt("habitacion_id", hab.id)
                    putDouble("precio", hab.precio)
                }

                val formFragment = ReservaFormFragment().apply {
                    arguments = bundle
                }

                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.contenedor_main, formFragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun nuevaInstancia(habitacion: Habitacion): DetalleReservaBottomSheet {
            val sheet = DetalleReservaBottomSheet()
            sheet.arguments = Bundle().apply {
                putSerializable("habitacion", habitacion)
            }
            return sheet
        }
    }
}
