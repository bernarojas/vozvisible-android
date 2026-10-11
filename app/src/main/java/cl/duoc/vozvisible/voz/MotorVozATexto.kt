package cl.duoc.vozvisible.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Motor de Voz a texto: transcribe en pantalla lo que dice el interlocutor.
 *
 * Hereda de [MotorComunicacion] e implementa [RecognitionListener], la interfaz
 * por la que el reconocedor de Android informa cada etapa: cuándo empieza a
 * escuchar, la intensidad del sonido, las transcripciones parciales y el
 * resultado final.
 *
 * Las parciales y la intensidad se publican en el estado mientras la persona
 * habla. Para el usuario sordo cumplen el papel que para un oyente cumple el
 * propio sonido: confirman que el teléfono está captando la voz.
 *
 * @param alTranscribir se invoca con el texto final de cada escucha.
 */
class MotorVozATexto(
    contexto: Context,
    private val alTranscribir: (String) -> Unit
) : MotorComunicacion(contexto), RecognitionListener {

    override val nombre: String = "Voz a texto"

    /** Nulo cuando el dispositivo no tiene un servicio de reconocimiento instalado. */
    private val reconocedor: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(contexto)) {
            SpeechRecognizer.createSpeechRecognizer(contexto)
        } else {
            null
        }

    init {
        reconocedor?.setRecognitionListener(this)
        estado = if (reconocedor != null) EstadoMotor.Listo
        else EstadoMotor.NoDisponible(
            "Este dispositivo no tiene un servicio de reconocimiento de voz instalado."
        )
    }

    /**
     * Comienza a escuchar.
     *
     * @return true si la escucha arrancó.
     */
    fun escuchar(): Boolean {
        val servicio = reconocedor ?: return false
        if (!estaListo) return false

        val solicitud = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, IDIOMA)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        return try {
            servicio.startListening(solicitud)
            estado = EstadoMotor.Escuchando()
            true
        } catch (e: SecurityException) {
            // Se llega aquí si el permiso de micrófono se revocó entre la
            // comprobación de la view y el inicio de la escucha.
            estado = EstadoMotor.Fallo(mensajeDeError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS))
            false
        }
    }

    /** Deja de escuchar y pide la transcripción de lo captado hasta ahora. */
    fun terminar() {
        if (estado is EstadoMotor.Escuchando) reconocedor?.stopListening()
    }

    /** Descarta la escucha en curso sin transcribir nada. */
    override fun detener() {
        reconocedor?.cancel()
        if (estaTrabajando) estado = EstadoMotor.Listo
    }

    override fun liberar() {
        reconocedor?.destroy()
        super.liberar()
    }

    // --- RecognitionListener -------------------------------------------------

    override fun onReadyForSpeech(parametros: Bundle?) {
        estado = EstadoMotor.Escuchando()
    }

    override fun onRmsChanged(decibeles: Float) {
        val actual = estado as? EstadoMotor.Escuchando ?: return
        estado = actual.copy(nivel = normalizarNivel(decibeles))
    }

    override fun onPartialResults(resultados: Bundle?) {
        val actual = estado as? EstadoMotor.Escuchando ?: return
        val parcial = resultados.primeraFrase() ?: return
        estado = actual.copy(parcial = parcial)
    }

    override fun onResults(resultados: Bundle?) {
        val texto = resultados.primeraFrase()
        if (texto.isNullOrBlank()) {
            estado = EstadoMotor.Fallo(mensajeDeError(SpeechRecognizer.ERROR_NO_MATCH))
            return
        }
        estado = EstadoMotor.Listo
        alTranscribir(texto)
    }

    override fun onError(codigo: Int) {
        // Tras cancelar, el reconocedor suele informar un error de cliente que
        // ya no corresponde mostrar: solo se atiende si se estaba escuchando.
        if (estado !is EstadoMotor.Escuchando) return
        estado = EstadoMotor.Fallo(mensajeDeError(codigo))
    }

    override fun onBeginningOfSpeech() = Unit
    override fun onBufferReceived(buffer: ByteArray?) = Unit
    override fun onEndOfSpeech() = Unit
    override fun onEvent(tipo: Int, parametros: Bundle?) = Unit

    /** Primera alternativa del reconocedor, que es la de mayor confianza. */
    private fun Bundle?.primeraFrase(): String? =
        this?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.trim()

    companion object {

        private const val IDIOMA = "es-CL"

        /** Rango de decibeles que entrega el reconocedor en la práctica. */
        private const val NIVEL_MINIMO = -2f
        private const val NIVEL_MAXIMO = 10f

        /** Lleva la intensidad del sonido al rango de 0 a 1 que usa la barra visual. */
        fun normalizarNivel(decibeles: Float): Float =
            ((decibeles - NIVEL_MINIMO) / (NIVEL_MAXIMO - NIVEL_MINIMO)).coerceIn(0f, 1f)

        /** Traduce los códigos de error del reconocedor a mensajes para el usuario. */
        fun mensajeDeError(codigo: Int): String = when (codigo) {
            SpeechRecognizer.ERROR_NO_MATCH ->
                "No se reconoció ninguna frase. Pide a tu interlocutor que hable más cerca del teléfono."

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "No se detectó ninguna voz. Vuelve a intentarlo cuando la persona empiece a hablar."

            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER, SpeechRecognizer.ERROR_SERVER_DISCONNECTED ->
                "No hay conexión con el servicio de reconocimiento. Revisa tu conexión a internet."

            SpeechRecognizer.ERROR_AUDIO ->
                "No se pudo usar el micrófono. Revisa que ninguna otra aplicación lo esté ocupando."

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "VozVisible necesita permiso para usar el micrófono."

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "El reconocedor está ocupado. Espera un momento y vuelve a intentarlo."

            else -> "No se pudo transcribir (código $codigo). Vuelve a intentarlo."
        }
    }
}
