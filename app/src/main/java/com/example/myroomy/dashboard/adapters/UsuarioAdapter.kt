package com.example.myroomy.dashboard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myroomy.R
import com.example.myroomy.dashboard.models.Usuario
import java.io.File

class UsuarioAdapter(
    private val lista: MutableList<Usuario>,
    private val onEditar: (Usuario) -> Unit,
    private val onEliminar: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    inner class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgUsuario: ImageView = view.findViewById(R.id.imgUsuario)
        val txtNombre: TextView = view.findViewById(R.id.txtNombreUsuario)
        val txtCorreo: TextView = view.findViewById(R.id.txtCorreoUsuario)
        val txtTipo: TextView = view.findViewById(R.id.txtTipoUsuario)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarUsuario)
        val cardView: View = view // Toda la tarjeta
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = lista[position]

        holder.txtNombre.text = usuario.nombre
        holder.txtCorreo.text = usuario.correo
        holder.txtTipo.text = "Tipo: ${usuario.tipo.name.lowercase().replaceFirstChar(Char::titlecase)}"

        // Cargar imagen si existe, o un placeholder
        if (!usuario.urlFoto.isNullOrBlank()) {
            Glide.with(holder.imgUsuario.context)
                .load(File(usuario.urlFoto))
                .placeholder(R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.imgUsuario)
        } else {
            holder.imgUsuario.setImageResource(R.drawable.ic_launcher_background)
        }

        // Clic en toda la tarjeta para editar
        holder.cardView.setOnClickListener {
            onEditar(usuario)
        }

        // Botón eliminar
        holder.btnEliminar.setOnClickListener {
            onEliminar(usuario)
        }
    }

    override fun getItemCount(): Int = lista.size
}
