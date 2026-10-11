package cl.duoc.vozvisible.voz

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import cl.duoc.vozvisible.util.fragmentosParaVoz
import java.util.Locale

/**
 * Motor de Texto a voz: reproduce en voz alta lo que el usuario escribe.
 *
 * Hereda de [MotorComunicacion] e implementa además la interfaz
 * [TextToSpeech.OnInitListener], porque el servicio de síntesis se inicializa de
 * forma asíncrona y avisa por ese medio cuando está listo.
 *
 * El texto se entrega por fragmentos y cada uno lleva un identificador. Con él,
 * el motor sabe cuál está sonando y lo publica en su estado: la view lo muestra
 * como barra de avance, que es la única forma de que una persona sorda sepa si
 * el mensaje terminó de reproducirse.
 */
class MotorTextoAVoz(contexto: Context) : MotorComunicacion(contexto), TextToSpeech.OnInitListener {

    override val nombre: String = "Texto a voz"

    /**
     * Número del envío en curso. Cada llamada a [hablar] lo incrementa, de modo
     * que los avisos atrasados de un envío anterior, ya interrumpido, se
     * reconocen por el identificador y se descartan.
     */
    private var envio = 0

    /** Idioma con que quedó configurado el sintetizador, para informarlo en pantalla. */
    var idioma: Locale? = null
        private set

    // Se declara al final de las propiedades: el sintetizador recibe `this` como
    // oyente, y para entonces el resto del objeto ya debe estar inicializado.
    private val sintetizador = TextToSpeech(contexto.applicationContext, this)

    override fun onInit(resultado: Int) {
        if (resultado != TextToSpeech.SUCCESS) {
            estado = EstadoMotor.NoDisponible(
                "El dispositivo no tiene un motor de síntesis de voz disponible."
            )
            return
        }

        // Se prueba primero el español de Chile y luego variantes más generales.
        // firstOrNull se detiene en el primer idioma que el motor acepte.
        idioma = IDIOMAS_PREFERIDOS.firstOrNull { candidato ->
            sintetizador.setLanguage(candidato) >= TextToSpeech.LANG_AVAILABLE
        }

        if (idioma == null) {
            estado = EstadoMotor.NoDisponible(
                "Falta instalar la voz en español. Puedes descargarla desde los ajustes de " +
                    "Texto a voz de Android."
            )
            return
        }

        sintetizador.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(identificador: String) = actualizarAvance(identificador, termino = false)
            override fun onDone(identificador: String) = actualizarAvance(identificador, termino = true)

            @Deprecated("Requerido por la clase abstracta en API anteriores a la 21")
            override fun onError(identificador: String) = informarError()

            override fun onError(identificador: String, codigo: Int) = informarError()
        })

        estado = EstadoMotor.Listo
    }

    /**
     * Reproduce el texto en voz alta.
     *
     * El primer fragmento vacía la cola del sintetizador y los siguientes se
     * agregan detrás, de modo que un mensaje nuevo interrumpe al anterior pero
     * sus propias partes suenan en orden.
     *
     * @return cantidad de fragmentos enviados, o 0 si el motor no estaba listo.
     */
    fun hablar(texto: String): Int {
        if (!estaListo || texto.isBlank()) return 0

        val fragmentos = texto.fragmentosParaVoz()
        envio++

        for ((indice, fragmento) in fragmentos.withIndex()) {
            val modo = if (indice == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            sintetizador.speak(fragmento, modo, null, identificador(envio, indice, fragmentos.size))
        }

        estado = EstadoMotor.Hablando(fragmento = 1, total = fragmentos.size)
        return fragmentos.size
    }

    override fun detener() {
        envio++ // invalida los avisos pendientes del envío interrumpido
        sintetizador.stop()
        if (estaTrabajando) estado = EstadoMotor.Listo
    }

    override fun liberar() {
        sintetizador.stop()
        sintetizador.shutdown()
        super.liberar()
    }

    override fun describirEstado(): String {
        val configurado = idioma
        return if (estado == EstadoMotor.Listo && configurado != null) {
            "Listo para hablar en ${configurado.getDisplayName(configurado)}"
        } else {
            super.describirEstado()
        }
    }

    private fun actualizarAvance(identificador: String, termino: Boolean) {
        val (numeroEnvio, indice, total) = interpretar(identificador) ?: return
        enHiloPrincipal {
            if (numeroEnvio != envio) return@enHiloPrincipal

            estado = when {
                termino && indice == total - 1 -> EstadoMotor.Listo
                termino -> EstadoMotor.Hablando(fragmento = indice + 2, total = total)
                else -> EstadoMotor.Hablando(fragmento = indice + 1, total = total)
            }
        }
    }

    private fun informarError() = enHiloPrincipal {
        estado = EstadoMotor.Fallo("No se pudo reproducir el mensaje. Inténtalo otra vez.")
    }

    companion object {

        private val IDIOMAS_PREFERIDOS = listOf(
            Locale.forLanguageTag("es-CL"),
            Locale.forLanguageTag("es-US"),
            Locale.forLanguageTag("es-ES"),
            Locale.forLanguageTag("es")
        )

        /** Compone el identificador de un fragmento: envío, posición y total. */
        fun identificador(envio: Int, indice: Int, total: Int): String = "$envio-$indice-$total"

        /**
         * Recupera las tres partes de un identificador.
         *
         * @return null si el texto no tiene la forma esperada.
         */
        fun interpretar(identificador: String): Triple<Int, Int, Int>? {
            val partes = identificador.split('-').mapNotNull { parte -> parte.toIntOrNull() }
            if (partes.size != 3) return null

            val (envio, indice, total) = partes
            return if (indice in 0 until total) Triple(envio, indice, total) else null
        }
    }
}
