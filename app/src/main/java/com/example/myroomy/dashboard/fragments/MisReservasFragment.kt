package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myroomy.R
import com.example.myroomy.dashboard.adapters.MisReservasAdapter
import com.example.myroomy.dashboard.database.ReservaDAO

class MisReservasFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutVacio: View
    private lateinit var adapter: MisReservasAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mis_reservas, container, false)

        recyclerView = view.findViewById(R.id.recyclerMisReservas)
        layoutVacio = view.findViewById(R.id.layoutVacio)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val prefs = requireContext().getSharedPreferences("MyRoomyPrefs", 0)
        val idUsuario = prefs.getInt("usuario_id", -1)

        if (idUsuario != -1) {
            val dao = ReservaDAO(requireContext())
            val listaReservas = dao.obtenerPorUsuario(idUsuario)

            if (listaReservas.isNotEmpty()) {
                adapter = MisReservasAdapter(listaReservas)
                recyclerView.adapter = adapter
                recyclerView.visibility = View.VISIBLE
                layoutVacio.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                layoutVacio.visibility = View.VISIBLE
            }
        } else {
            // Usuario inválido: mostramos vacío
            recyclerView.visibility = View.GONE
            layoutVacio.visibility = View.VISIBLE
        }

        return view
    }
}
