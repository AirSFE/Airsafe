package com.example.airsafe10.ui.home

import android.graphics.Color
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
import com.example.airsafe10.databinding.FragmentHomeBinding
import com.google.firebase.database.FirebaseDatabase

class HomeFragment : Fragment(), Runnable {

    private var _binding: FragmentHomeBinding? = null

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
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        //Botón actualizar
        val Actualizar = binding.Actualizar

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    override fun run(){

        //Código de conexión y representación de datos en pantalla

        //Colores para representar los niveles de puntos imeca
        val colorBuena = ContextCompat.getColor(requireContext(), R.color.Buena)
        val colorReg = ContextCompat.getColor(requireContext(), R.color.Regular)
        val colorMala = ContextCompat.getColor(requireContext(), R.color.Mala)
        val colorMuyMala = ContextCompat.getColor(requireContext(), R.color.MuyMala)
        val colorExtrema = ContextCompat.getColor(requireContext(), R.color.Extrema)

        //Definición de los elementos en pantalla
        val PMdosValue = binding.pm25Value
        val PMdiezValue = binding.pm10Value
        val CodosValue = binding.co2Value
        val SodosValue = binding.so2Value
        val IMECAvalue = binding.ImecaValue
        val ImageCara = binding.Cara
        val CalidadAire = binding.Calidad
        val SwitchRecomendaciones = binding.switch2
        val RecomText = binding.Recomendaciones
        val NoValue = binding.noValue

        //LLamado de instancia de la base de datos en tiempo real de firebase
        val database = FirebaseDatabase.getInstance()
        val reference = database.getReference("Contaminantes")
        reference.get().addOnSuccessListener { dataSnapshot ->
            //Si el llamado de datos existe
            if (dataSnapshot.exists()) {
                //Referencia a el valor imeca
                val imecaReference = dataSnapshot.child("IMECA") // Asumiendo que "IMECA" es un nodo dentro de "Contaminantes"
                //Lista de valores de imeca
                val imecaList = mutableListOf<Int>()

                //llenado de Lista de imeca
                for (imecaSnapshot in imecaReference.children) {
                    val imecaValue = imecaSnapshot.getValue(Int::class.java)
                    if (imecaValue != null) {
                        imecaList.add(imecaValue)
                    }
                }

                //Ultimo valor de imeca
                val imeca = imecaList.last()

                //LLamado a cada uno de los contaminantes
                val coDos = dataSnapshot.child("CO").getValue(Int::class.java)
                val pmDiez = dataSnapshot.child("PMdiez").getValue(Int::class.java)
                val pmDos = dataSnapshot.child("PMdos").getValue(Int::class.java)
                val soDos = dataSnapshot.child("SOdos").getValue(Int::class.java)
                val NOv = dataSnapshot.child("NOdos").getValue(Int::class.java)

                //Lógica de el recuadro de puntos imeca
                if (imeca != null) {

                    //Calidad del aire "buena"
                    if(imeca <= 50){
                        ImageCara.setImageResource(resources.getIdentifier("feliz", "drawable", requireContext().packageName))
                        ImageCara.setBackgroundColor(colorBuena)
                        CalidadAire.text = "Buena"
                        CalidadAire.setBackgroundColor(colorBuena)
                        RecomText.text="\t-\tSe puede estar con normalidad."
                    }
                    //Calidad del aire "Regular"
                    else if(imeca in (51..100)){
                        ImageCara.setImageResource(resources.getIdentifier("mediofeliz", "drawable", requireContext().packageName))
                        ImageCara.setBackgroundColor(colorReg)
                        CalidadAire.text = "Regular"
                        CalidadAire.setBackgroundColor(colorReg)
                        if(SwitchRecomendaciones.isChecked){
                            RecomText.text="\t-\tConsidere reducir las actividades físicas al aire libre."
                        }
                        else{
                            RecomText.text="\t-\tRecomendamos ventilar el área donde se encuentra."
                        }
                    }
                    //Calidad del aire "Mala"
                    else if(imeca in (101..150)){
                        ImageCara.setImageResource(resources.getIdentifier("serio", "drawable", requireContext().packageName))
                        ImageCara.setBackgroundColor(colorMala)
                        CalidadAire.text = "Mala"
                        CalidadAire.setBackgroundColor(colorMala)
                        if(SwitchRecomendaciones.isChecked){
                            RecomText.text="\t-\tEvita las actividades físicas (Tanto moderadas como vigorosas) al aire libre.\n" +
                                    "Hidrátese continuamente.\n"
                        }
                        else{
                            RecomText.text="\t-\tRecomendamos ventilar el área donde se encuentra, de contar con un purificador de aire comenzar a utilizarlo"
                        }
                    }
                    //Calidad del aire "Muy mala"
                    else if(imeca in (151..200)){
                        ImageCara.setImageResource(resources.getIdentifier("triste", "drawable", requireContext().packageName))
                        ImageCara.setBackgroundColor(colorMuyMala)
                        CalidadAire.text = "Muy mala"
                        CalidadAire.setBackgroundColor(colorMuyMala)
                        if(SwitchRecomendaciones.isChecked){
                            RecomText.text="\t-\tNo realices actividades de ningún tipo al aire libre.\n" +
                                    "De necesitar salir utilizar cubrebocas.\n" +
                                    "Hidrátese continuamente.\n" +
                                    "Evita el uso de lentes de contacto.\n" +
                                    "No fumar.\n" +
                                    "Acudir al médico si se presentan síntomas respiratorios o cardiacos.\n"
                        }
                        else{
                            RecomText.text="\t-\tDe contar con aire acondicionado utilizarlo en modo recirculación.\n" +
                                    "De contar con un purificador de aire utilizarlo.\n" +
                                    "Hidrátese continuamente.\n" +
                                    "Evita el uso de lentes de contacto.\n" +
                                    "Acudir al médico si se presentan síntomas respiratorios o cardiacos.\n"
                        }
                    }
                    //Calidad del aire "Extremadamente mala"
                    else{
                        ImageCara.setImageResource(resources.getIdentifier("muerto", "drawable", requireContext().packageName))
                        ImageCara.setBackgroundColor(colorExtrema)
                        CalidadAire.text = "Extremadamente mala"
                        CalidadAire.setBackgroundColor(colorExtrema)
                        if(SwitchRecomendaciones.isChecked){
                            RecomText.text="\t-\tPermanece en espacios interiores.\n" +
                                    "De necesitar salir utilizar cubrebocas en todo momento.\n" +
                                    "Acudir al médico si se presentan síntomas respiratorios o cardiacos.\n"
                        }
                        else{
                            RecomText.text="\t-\tDe contar con un purificador de aire utilizarlo de inmediato.\n" +
                                    "Salir del área donde se encuentra hasta que se purifique el aire completamente o mejoren los niveles.\n" +
                                    "Acudir al médico si se presentan síntomas respiratorios o cardiacos.\n"
                        }
                    }
                }

                //Representación de valores de contaminantes en pantalla
                PMdosValue.text = "$pmDos µg/m³"
                PMdiezValue.text = "$pmDiez µg/m³"
                CodosValue.text = "$coDos PPM"
                SodosValue.text = "$soDos PPM"
                NoValue.text = "$NOv PPM"
                IMECAvalue.text = "Imeca: $imeca"

            } else {
                Log.e("firebase", "Los datos no existen")
            }
        }.addOnFailureListener { exception ->
            Log.e("firebase", "Error al obtener datos", exception)
        }
        handler.postDelayed(this, delayMillis.toLong())//Delay de actualización
    }
}