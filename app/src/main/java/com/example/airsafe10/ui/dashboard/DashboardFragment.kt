package com.example.airsafe10.ui.dashboard

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.airsafe10.R
import com.example.airsafe10.databinding.FragmentDashboardBinding
import com.google.firebase.database.FirebaseDatabase
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.Viewport
import com.jjoe64.graphview.series.BarGraphSeries
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries

class DashboardFragment : Fragment(), Runnable {

    private var _binding: FragmentDashboardBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val handler = Handler(Looper.getMainLooper())

    //Delay para actualización de datos de firestore
    private val delayMillis = 30000
    private var isUpdating = false
    //Delay para habilitar el botón actualizar
    private val enableButtonDelayMillis = 10000

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        //Botón actualizar
        val Actualizar = binding.Actualizars

        //Boton actualizar presionado
        Actualizar.setOnClickListener {
            if (!isUpdating) {
                isUpdating = true
                Actualizar.isEnabled = false
                run()
                Handler(Looper.getMainLooper()).postDelayed({
                    isUpdating = false
                    Actualizar.isEnabled = true
                }, enableButtonDelayMillis.toLong())//Delay de 10s
            }
        }
        return root
    }
    override fun onResume() {
        super.onResume()
        //Al terminar la funcion on create, correr el código de conexión
        run()
    }
    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(this)
    }
    override fun run(){
        val colorBuena = ContextCompat.getColor(requireContext(), R.color.Buena)
        val colorReg = ContextCompat.getColor(requireContext(), R.color.Regular)
        val colorMala = ContextCompat.getColor(requireContext(), R.color.Mala)
        val colorMuyMala = ContextCompat.getColor(requireContext(), R.color.MuyMala)
        val colorExtrema = ContextCompat.getColor(requireContext(), R.color.Extrema)

        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("Contaminantes")

        reference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val imecaList = mutableListOf<Int>()
                val imecaReference = dataSnapshot.child("IMECA") // Asumiendo que "IMECA" es un nodo dentro de "Contaminantes"

                for (imecaSnapshot in imecaReference.children) {
                    val imecaValue = imecaSnapshot.getValue(Int::class.java)
                    if (imecaValue != null) {
                        imecaList.add(imecaValue)
                    }
                }
                val imeca = imecaList.last()
                imecaList.reverse()

                val graphView: GraphView = binding.idGraphView
                graphView.removeAllSeries()
                if (imecaList.size >= 5) {
                    val seriesDataPoints = (0 until 5).map { index ->
                        DataPoint(index.toDouble(), imecaList[index].toDouble())
                    }

                    val series: BarGraphSeries<DataPoint> =
                        BarGraphSeries(seriesDataPoints.toTypedArray())
                    series.spacing = 50
                    series.isAnimated = false
                    graphView.addSeries(series)
                    val viewport: Viewport = graphView.viewport
                    viewport.isScalable = false
                    viewport.isScrollable = false
                    viewport.setYAxisBoundsManual(true)
                    viewport.setMinY(imecaList.min()-1.0)

                    if (imeca <= 50) {
                        series.color = colorBuena
                    } else if (imeca in (51..100)) {
                        series.color = colorReg
                    } else if (imeca in (101..150)) {
                        series.color = colorMala
                    } else if (imeca in (151..200)) {
                        series.color = colorMuyMala
                    } else {
                        series.color = colorExtrema
                    }
                }


            } else {
                Log.e("firebase", "Los datos no existen")
            }
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error al obtener datos", exception)
        }
        handler.postDelayed(this, delayMillis.toLong())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}