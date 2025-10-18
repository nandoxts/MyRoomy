package com.example.myroomy.dashboard.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myroomy.R
import com.example.myroomy.dashboard.models.Reserva

class MisReservasAdapter(
    private val lista: List<Reserva>
) : RecyclerView.Adapter<MisReservasAdapter.MisReservasViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MisReservasViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mis_reservas, parent, false)
        return MisReservasViewHolder(view)
    }

    override fun onBindViewHolder(holder: MisReservasViewHolder, position: Int) {
        val reserva = lista[position]
        holder.txtHabitacion.text = reserva.nombreHabitacion
        holder.txtFechas.text = "Del ${reserva.fechaIngreso} al ${reserva.fechaSalida}"
        holder.txtEstado.text = reserva.estado

        val ctx = holder.itemView.context


        val bg = when (reserva.estado.lowercase()) {
            "aceptada" -> ctx.getDrawable(R.drawable.bg_chip_aceptada)
            "rechazada" -> ctx.getDrawable(R.drawable.bg_chip_rechazada)
            "pendiente" -> ctx.getDrawable(R.drawable.bg_chip_pendiente)
            else -> ctx.getDrawable(R.drawable.bg_chip_estado)
        }

        holder.txtEstado.background = bg
    }


    override fun getItemCount(): Int = lista.size

    class MisReservasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtHabitacion: TextView = view.findViewById(R.id.txtMisReservaHabitacion)
        val txtFechas: TextView = view.findViewById(R.id.txtMisReservaFechas)
        val txtEstado: TextView = view.findViewById(R.id.txtMisReservaEstado)
    }
}
