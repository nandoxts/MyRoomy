package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myroomy.dashboard.adapters.UsuarioAdapter
import com.example.myroomy.dashboard.database.UsuarioDAO
import com.example.myroomy.dashboard.models.Usuario
import com.example.myroomy.dashboard.utils.FragmentHelper
import com.example.myroomy.databinding.FragmentAdminUsuarioListBinding

class UsuarioListFragment : Fragment() {

    private var _binding: FragmentAdminUsuarioListBinding? = null
    private val binding get() = _binding!!

    private lateinit var usuarioDAO: UsuarioDAO
    private lateinit var adapter: UsuarioAdapter
    private val listaUsuarios = mutableListOf<Usuario>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminUsuarioListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usuarioDAO = UsuarioDAO(requireContext())

        adapter = UsuarioAdapter(
            listaUsuarios,
            onEditar = { usuarioSeleccionado ->
                val fragment = UsuarioFormFragment.nuevaInstancia(usuarioSeleccionado)
                FragmentHelper.replaceWithSmoothSlide(requireActivity() as AppCompatActivity, fragment)
            },
            onEliminar = { usuario ->
                eliminarUsuario(usuario)
            }
        )

        binding.recyclerUsuarios.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerUsuarios.adapter = adapter

        binding.btnAgregarUsuario.setOnClickListener {
            FragmentHelper.replaceWithSmoothSlide(requireActivity() as AppCompatActivity, UsuarioFormFragment())
        }

        cargarUsuarios()
    }

    override fun onResume() {
        super.onResume()
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        listaUsuarios.clear()
        listaUsuarios.addAll(usuarioDAO.obtenerTodos())
        adapter.notifyDataSetChanged()
        verificarListaVacia()
    }

    private fun eliminarUsuario(usuario: Usuario) {
        val alert = AlertDialog.Builder(requireContext())
            .setTitle("¿Eliminar usuario?")
            .setMessage("¿Estás seguro de eliminar a '${usuario.nombre}'?")
            .setPositiveButton("Sí") { _, _ ->
                val filasEliminadas = usuarioDAO.eliminar(usuario.id)
                if (filasEliminadas > 0) {
                    val index = listaUsuarios.indexOf(usuario)
                    if (index != -1) {
                        listaUsuarios.removeAt(index)
                        adapter.notifyItemRemoved(index)
                        verificarListaVacia()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()
        alert.show()
    }

    private fun verificarListaVacia() {
        if (listaUsuarios.isEmpty()) {
            binding.recyclerUsuarios.visibility = View.GONE
            binding.layoutVacio.visibility = View.VISIBLE
        } else {
            binding.recyclerUsuarios.visibility = View.VISIBLE
            binding.layoutVacio.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
