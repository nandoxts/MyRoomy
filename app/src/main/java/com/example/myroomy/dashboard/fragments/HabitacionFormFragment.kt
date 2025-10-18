package com.example.myroomy.dashboard.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myroomy.dashboard.database.HabitacionDAO
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.databinding.FragmentAdminHabitacionFormBinding
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.io.File
import java.io.InputStream

class HabitacionFormFragment : Fragment() {

    private var _binding: FragmentAdminHabitacionFormBinding? = null
    private val binding get() = _binding!!

    private lateinit var habitacionDAO: HabitacionDAO
    private var rutaImagenGuardada: String? = null
    private var habitacionExistente: Habitacion? = null

    private val categorias = arrayOf("Suite", "Doble", "Individual")
    private val estados = arrayOf("Disponible", "Ocupada", "Mantenimiento")

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.previewImagen.setImageURI(it)
            guardarImagenEnLocal(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminHabitacionFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        habitacionDAO = HabitacionDAO(requireContext())

        (binding.spinnerCategoria as? MaterialAutoCompleteTextView)?.setSimpleItems(categorias)
        (binding.spinnerEstado as? MaterialAutoCompleteTextView)?.setSimpleItems(estados)

        habitacionExistente = arguments?.getSerializable("habitacion") as? Habitacion
        habitacionExistente?.let { h ->
            binding.txtTituloFormulario.text = "Editar habitación"
            binding.inputNombre.setText(h.nombre)
            binding.inputDescripcion.setText(h.descripcion)
            binding.inputPiso.setText(h.piso.toString())
            binding.inputNumeroHabitacion.setText(h.numeroHabitacion)
            binding.inputPrecio.setText(h.precio.toString())
            (binding.spinnerCategoria as? MaterialAutoCompleteTextView)?.setText(h.categoria, false)
            (binding.spinnerEstado as? MaterialAutoCompleteTextView)?.setText(h.estado, false)
            rutaImagenGuardada = h.imagen

            if (h.imagen.startsWith("http")) {
                Glide.with(this)
                    .load(h.imagen)
                    .placeholder(android.R.color.darker_gray)
                    .into(binding.previewImagen)
            } else {
                Glide.with(this)
                    .load(File(h.imagen))
                    .placeholder(android.R.color.darker_gray)
                    .into(binding.previewImagen)
            }

            h.servicios.forEach { servicio ->
                binding.chipGroupServicios.addView(crearChip(servicio))
            }
        } ?: run {
            binding.txtTituloFormulario.text = "Agregar habitación"
        }

        binding.btnAgregarServicio.setOnClickListener {
            val serviciosActuales = mutableListOf<String>()
            for (i in 0 until binding.chipGroupServicios.childCount) {
                val chip = binding.chipGroupServicios.getChildAt(i) as Chip
                serviciosActuales.add(chip.text.toString())
            }

            ServiciosBottomSheet(serviciosActuales) { servicio ->
                agregarServicioChip(servicio)
            }.show(parentFragmentManager, "ServiciosBottomSheet")
        }


        binding.btnSeleccionarImagen.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        binding.btnGuardarHabitacion.setOnClickListener {
            guardarHabitacion()
        }
    }



    private fun agregarServicioChip(servicio: String) {
        val yaExiste = (0 until binding.chipGroupServicios.childCount).any { i ->
            val chip = binding.chipGroupServicios.getChildAt(i) as Chip
            chip.text.toString().equals(servicio, ignoreCase = true)
        }

        if (yaExiste) {
            Toast.makeText(requireContext(), "Este servicio ya fue agregado", Toast.LENGTH_SHORT).show()
        } else {
            binding.chipGroupServicios.addView(crearChip(servicio))
        }
    }

    private fun crearChip(servicio: String): Chip {
        return Chip(requireContext()).apply {
            text = servicio
            isCloseIconVisible = true
            setOnCloseIconClickListener { binding.chipGroupServicios.removeView(this) }
        }
    }

    private fun guardarHabitacion() {
        val nombre = binding.inputNombre.text.toString()
        val descripcion = binding.inputDescripcion.text.toString()
        val categoria = (binding.spinnerCategoria as? MaterialAutoCompleteTextView)?.text.toString()
        val estado = (binding.spinnerEstado as? MaterialAutoCompleteTextView)?.text.toString()
        val piso = binding.inputPiso.text.toString().toIntOrNull() ?: 0
        val numeroHabitacion = binding.inputNumeroHabitacion.text.toString()
        val precio = binding.inputPrecio.text.toString().toDoubleOrNull() ?: 0.0

        val servicios = mutableListOf<String>()
        for (i in 0 until binding.chipGroupServicios.childCount) {
            val chip = binding.chipGroupServicios.getChildAt(i) as Chip
            servicios.add(chip.text.toString())
        }

        if (nombre.isBlank() || rutaImagenGuardada.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Completa el nombre e imagen", Toast.LENGTH_SHORT).show()
            return
        }

        val habitacion = habitacionExistente?.copy(
            nombre = nombre,
            descripcion = descripcion,
            categoria = categoria,
            imagen = rutaImagenGuardada!!,
            estado = estado,
            precio = precio,
            numeroHabitacion = numeroHabitacion,
            piso = piso,
            servicios = servicios
        ) ?: Habitacion(
            nombre = nombre,
            descripcion = descripcion,
            categoria = categoria,
            imagen = rutaImagenGuardada!!,
            estado = estado,
            precio = precio,
            numeroHabitacion = numeroHabitacion,
            piso = piso,
            servicios = servicios
        )

        if (habitacionExistente != null) {
            habitacionDAO.actualizar(habitacion)
            Toast.makeText(requireContext(), "Habitación actualizada", Toast.LENGTH_SHORT).show()
        } else {
            habitacionDAO.insertar(habitacion)
            Toast.makeText(requireContext(), "Habitación guardada", Toast.LENGTH_SHORT).show()
        }

        parentFragmentManager.popBackStack()
    }

    private fun guardarImagenEnLocal(uri: Uri) {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        val nombreArchivo = "hab_${System.currentTimeMillis()}.jpg"
        val carpetaDestino = File(requireContext().filesDir, "habitaciones")
        if (!carpetaDestino.exists()) carpetaDestino.mkdirs()

        val archivoDestino = File(carpetaDestino, nombreArchivo)
        inputStream?.use { input ->
            archivoDestino.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        rutaImagenGuardada = archivoDestino.absolutePath
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun nuevaInstancia(habitacion: Habitacion? = null): HabitacionFormFragment {
            val fragment = HabitacionFormFragment()
            fragment.arguments = Bundle().apply {
                putSerializable("habitacion", habitacion)
            }
            return fragment
        }
    }
}
