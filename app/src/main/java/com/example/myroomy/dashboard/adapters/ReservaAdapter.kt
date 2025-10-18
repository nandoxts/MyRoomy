package com.example.myroomy.dashboard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myroomy.R
import com.example.myroomy.dashboard.fragments.ReservaDetalleBottomSheet

import com.example.myroomy.dashboard.models.Reserva
import java.io.File

class ReservaAdapter(
    private val lista: List<Reserva>,
    private val onAccion: (Reserva, String) -> Unit
) : RecyclerView.Adapter<ReservaAdapter.ReservaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reserva, parent, false)
        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        val reserva = lista[position]

        holder.txtNombreUsuario.text = reserva.nombreUsuario
        holder.txtCategoriaPrecio.text = "${reserva.nombreHabitacion} - S/ ${String.format("%.2f", reserva.precio)}"

        // Cambia el texto del estado como chip (puedes mejorar el fondo en XML drawable)
        holder.txtEstado.text = reserva.estado
        holder.txtEstado.setBackgroundResource(when (reserva.estado) {
            "Pendiente" -> R.drawable.bg_chip_pendiente
            "Aceptada" -> R.drawable.bg_chip_aceptada
            "Rechazada" -> R.drawable.bg_chip_rechazada
            else -> R.drawable.bg_chip_default
        })

        Glide.with(holder.itemView)
            .load(File(reserva.urlFotoUsuario))
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgUsuario)


        holder.itemView.setOnClickListener {
            val fragment = ReservaDetalleBottomSheet(reserva) { res, accion ->
                onAccion(res, accion)
            }
            fragment.show((holder.itemView.context as AppCompatActivity).supportFragmentManager, "DetalleReserva")
        }

    }

    override fun getItemCount(): Int = lista.size

    class ReservaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgUsuario: ImageView = view.findViewById(R.id.imgUsuarioReserva)
        val txtNombreUsuario: TextView = view.findViewById(R.id.txtNombreUsuarioReserva)
        val txtCategoriaPrecio: TextView = view.findViewById(R.id.txtCategoriaPrecioReserva)
        val txtEstado: TextView = view.findViewById(R.id.txtEstadoReserva)
    }
}
