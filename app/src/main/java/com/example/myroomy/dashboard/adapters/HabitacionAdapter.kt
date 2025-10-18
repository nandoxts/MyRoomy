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
import com.example.myroomy.dashboard.models.Habitacion
import java.io.File

class HabitacionAdapter(
    private val lista: MutableList<Habitacion>,
    private val onItemClick: (Habitacion) -> Unit,
    private val onEliminarClick: (Habitacion) -> Unit
) : RecyclerView.Adapter<HabitacionAdapter.HabitacionViewHolder>() {

    inner class HabitacionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgHabitacion)
        val txtNombre: TextView = view.findViewById(R.id.txtNombreHabitacion)
        val txtInfo: TextView = view.findViewById(R.id.txtInfoHabitacion)
        val txtPrecio: TextView = view.findViewById(R.id.txtPrecioHabitacion)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarHabitacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitacionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_habitacion, parent, false)
        return HabitacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitacionViewHolder, position: Int) {
        val h = lista[position]

        holder.txtNombre.text = h.nombre
        holder.txtInfo.text = "${h.categoria} | Piso ${h.piso} | ${h.estado}"
        holder.txtPrecio.text = "S/ ${"%.2f".format(h.precio)}"

        val context = holder.itemView.context
        when {
            h.imagen.startsWith("http") -> {
                Glide.with(context)
                    .load(h.imagen)
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.img)
            }
            h.imagen.startsWith("/") -> {
                Glide.with(context)
                    .load(File(h.imagen))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.img)
            }
            else -> {
                val idDrawable = context.resources.getIdentifier(
                    h.imagen, "drawable", context.packageName
                )
                holder.img.setImageResource(idDrawable.takeIf { it != 0 } ?: R.drawable.ic_launcher_background)
            }
        }


        holder.itemView.setOnClickListener { onItemClick(h) }
        holder.btnEliminar.setOnClickListener { onEliminarClick(h) }
    }

    override fun getItemCount() = lista.size
}
