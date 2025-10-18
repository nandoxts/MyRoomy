package com.example.myroomy.dashboard.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.myroomy.dashboard.database.UsuarioDAO
import com.example.myroomy.dashboard.models.TipoUsuario
import com.example.myroomy.dashboard.models.Usuario
import com.example.myroomy.dashboard.utils.FragmentHelper
import com.example.myroomy.databinding.FragmentAdminUsuarioFormBinding
import java.io.File
import java.io.InputStream

class UsuarioFormFragment : Fragment() {

    private var _binding: FragmentAdminUsuarioFormBinding? = null
    private val binding get() = _binding!!

    private var usuario: Usuario? = null
    private lateinit var dao: UsuarioDAO
    private var rutaImagenGuardada: String? = null

    private val seleccionarImagenLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            binding.imgPreviewUsuario.setImageURI(it)
            guardarImagenEnLocal(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usuario = arguments?.getParcelable("usuario")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsuarioFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dao = UsuarioDAO(requireContext())

        val tipos = TipoUsuario.values().map {
            it.name.lowercase().replaceFirstChar(Char::titlecase)
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            tipos
        )
        binding.spinnerTipoUsuario.setAdapter(adapter)

        usuario?.let {
            binding.edtNombreUsuario.setText(it.nombre)
            binding.edtCorreoUsuario.setText(it.correo)
            binding.edtClaveUsuario.setText(it.clave)
            binding.spinnerTipoUsuario.setText(
                it.tipo.name.lowercase().replaceFirstChar(Char::titlecase),
                false
            )
            rutaImagenGuardada = it.urlFoto
            if (!rutaImagenGuardada.isNullOrBlank()) {
                Glide.with(this).load(File(rutaImagenGuardada!!)).into(binding.imgPreviewUsuario)
            }
            binding.btnGuardarUsuario.text = "Actualizar usuario"
            binding.txtTituloFormularioUsuario.text = "Editar usuario"
        }

        binding.btnSeleccionarImagenUsuario.setOnClickListener {
            seleccionarImagenLauncher.launch("image/*")
        }

        binding.btnGuardarUsuario.setOnClickListener {
            val nombre = binding.edtNombreUsuario.text.toString().trim()
            val correo = binding.edtCorreoUsuario.text.toString().trim()
            val clave = binding.edtClaveUsuario.text.toString().trim()
            val tipoTexto = binding.spinnerTipoUsuario.text.toString().trim()

            if (nombre.isEmpty() || correo.isEmpty() || clave.isEmpty() || tipoTexto.isEmpty() || rutaImagenGuardada.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Completa todos los campos e imagen", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tipo = TipoUsuario.values().firstOrNull {
                it.name.equals(tipoTexto, ignoreCase = true)
            }

            if (tipo == null) {
                Toast.makeText(requireContext(), "Tipo de usuario inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nuevo = Usuario(
                id = usuario?.id ?: 0,
                nombre = nombre,
                correo = correo,
                tipo = tipo,
                clave = clave,
                estado = 1,
                urlFoto = rutaImagenGuardada
            )

            if (usuario == null) {
                dao.insertar(nuevo)
                Toast.makeText(requireContext(), "Usuario registrado", Toast.LENGTH_SHORT).show()
            } else {
                dao.actualizar(nuevo)
                Toast.makeText(requireContext(), "Usuario actualizado", Toast.LENGTH_SHORT).show()
            }

            parentFragmentManager.popBackStack()
        }
    }

    private fun guardarImagenEnLocal(uri: Uri) {
        val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
        val nombreArchivo = "user_${System.currentTimeMillis()}.jpg"
        val carpetaDestino = File(requireContext().filesDir, "usuarios")
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
        fun nuevaInstancia(usuario: Usuario? = null): UsuarioFormFragment {
            val fragment = UsuarioFormFragment()
            fragment.arguments = Bundle().apply {
                putParcelable("usuario", usuario)
            }
            return fragment
        }
    }
}
