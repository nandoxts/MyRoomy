package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.example.myroomy.R

class PerfilFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_perfil, container, false)

        // Obtener SharedPreferences para recuperar datos del usuario
        val prefs = requireContext().getSharedPreferences("MyRoomyPrefs", 0)
        val nombre = prefs.getString("usuario_nombre", "Nombre no disponible")
        val correo = prefs.getString("usuario_correo", "Correo no disponible")
        val fotoUrl = prefs.getString("usuario_foto", "")
        val tipoUsuario = prefs.getString("usuario_tipo", "Cliente") // Tipo de usuario
        val estadoUsuario = prefs.getString("usuario_estado", "Activo") // Estado del usuario

        // Referencias a los elementos de la UI
        val txtNombre = view.findViewById<TextView>(R.id.txtNombrePerfil)
        val txtCorreo = view.findViewById<TextView>(R.id.txtCorreoPerfil)
        val imgFoto = view.findViewById<ImageView>(R.id.imgFotoPerfil)
        val txtTipo = view.findViewById<TextView>(R.id.txtTipoPerfil)
        val txtEstado = view.findViewById<TextView>(R.id.txtEstadoPerfil)
        val btnEditarPerfil = view.findViewById<Button>(R.id.btnEditarPerfil)

        // Asignar los valores a las vistas
        txtNombre.text = nombre
        txtCorreo.text = correo
        txtTipo.text = "Tipo de usuario: $tipoUsuario"
        txtEstado.text = "Estado: $estadoUsuario"

        // Cargar la foto del perfil si existe una URL, si no, cargar una por defecto
        if (!fotoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(fotoUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_person_placeholder)
                .into(imgFoto)
        } else {
            imgFoto.setImageResource(R.drawable.ic_person_placeholder)
        }

        // Manejar el botón de editar perfil
        btnEditarPerfil.setOnClickListener {
            // Lógica para editar el perfil
            // Podrías abrir otro fragmento para editar los datos o lanzar una nueva actividad
        }

        return view
    }
}
