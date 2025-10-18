package com.example.myroomy.dashboard.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myroomy.R
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.databinding.ItemHabitacionCatalogoBinding

class HabitacionCatalogoAdapter(
    private val onClick: (Habitacion) -> Unit
) : ListAdapter<Habitacion, HabitacionCatalogoAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(val binding: ItemHabitacionCatalogoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habitacion: Habitacion) {
            binding.txtNombreHabitacion.text = habitacion.nombre
            binding.txtPrecioHabitacion.text = "S/ ${habitacion.precio}"
            binding.txtEstadoHabitacion.text = habitacion.estado

            Glide.with(binding.root.context)
                .load(
                    if (habitacion.imagen.startsWith("http", true)) {
                        habitacion.imagen
                    } else {
                        java.io.File(habitacion.imagen)
                    }
                )
                .placeholder(R.drawable.ic_room)
                .error(R.drawable.ic_room)
                .into(binding.imgHabitacion)

            binding.root.setOnClickListener {
                onClick(habitacion)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHabitacionCatalogoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Habitacion>() {
        override fun areItemsTheSame(oldItem: Habitacion, newItem: Habitacion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Habitacion, newItem: Habitacion): Boolean {
            return oldItem == newItem
        }
    }
}
