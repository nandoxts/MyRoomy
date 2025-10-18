package com.example.myroomy.dashboard.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myroomy.R
import com.example.myroomy.dashboard.models.Habitacion
import com.example.myroomy.databinding.ItemCarruselDestacadoBinding

class DestacadosAdapter(
    private val lista: List<Habitacion>,
    private val onItemClick: (Habitacion) -> Unit
) : RecyclerView.Adapter<DestacadosAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCarruselDestacadoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(habitacion: Habitacion) {
            Glide.with(binding.root.context)
                .load(habitacion.imagen)
                .placeholder(R.drawable.ic_room)
                .into(binding.imgHabitacion)

            binding.root.setOnClickListener {
                onItemClick(habitacion)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCarruselDestacadoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount() = lista.size
}
