package com.example.myroomy.dashboard.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myroomy.R
import android.widget.ArrayAdapter
import android.widget.Spinner

class UbicacionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ubicacion, container, false)

        val spinnerDepartamento: Spinner = view.findViewById(R.id.spinnerDepartamento)
        val departamentos = listOf("Lima", "Cusco", "Arequipa")
        spinnerDepartamento.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, departamentos)

        return view
    }
}
