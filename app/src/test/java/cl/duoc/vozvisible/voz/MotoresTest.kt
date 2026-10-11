package cl.duoc.vozvisible.voz

import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la lógica pura de los motores de voz.
 *
 * Los motores envuelven servicios de Android que no existen en la máquina
 * virtual de Java, así que aquí no se instancian. Lo que sí se prueba son las
 * funciones de sus companion object, que no dependen del sistema: la
 * interpretación de los identificadores de fragmento, la escala de la barra de
 * intensidad y la traducción de los códigos de error.
 */
class MotoresTest {

    @Test
    fun `el identificador de un fragmento se recupera completo`() {
        val identificador = MotorTextoAVoz.identificador(envio = 4, indice = 1, total = 3)

        assertEquals(Triple(4, 1, 3), MotorTextoAVoz.interpretar(identificador))
    }

    @Test
    fun `un identificador mal formado se descarta`() {
        assertNull(MotorTextoAVoz.interpretar("texto-cualquiera"))
        assertNull(MotorTextoAVoz.interpretar("1-2"))
        // El índice debe quedar dentro del total de fragmentos.
        assertNull(MotorTextoAVoz.interpretar("1-3-3"))
    }

    @Test
    fun `el avance cuenta solo los fragmentos ya terminados`() {
        assertEquals(0f, EstadoMotor.Hablando(fragmento = 1, total = 4).progreso, 0.001f)
        assertEquals(0.75f, EstadoMotor.Hablando(fragmento = 4, total = 4).progreso, 0.001f)
        // Un mensaje de un solo fragmento no tiene avance medible.
        assertFalse(EstadoMotor.Hablando(fragmento = 1, total = 1).avanceMedible)
    }

    @Test
    fun `la intensidad de la voz queda entre cero y uno`() {
        assertEquals(0f, MotorVozATexto.normalizarNivel(-10f), 0.001f)
        assertEquals(0f, MotorVozATexto.normalizarNivel(-2f), 0.001f)
        assertEquals(0.5f, MotorVozATexto.normalizarNivel(4f), 0.001f)
        assertEquals(1f, MotorVozATexto.normalizarNivel(25f), 0.001f)
    }

    @Test
    fun `los errores de red comparten un mismo mensaje`() {
        val red = MotorVozATexto.mensajeDeError(SpeechRecognizer.ERROR_NETWORK)

        assertEquals(red, MotorVozATexto.mensajeDeError(SpeechRecognizer.ERROR_NETWORK_TIMEOUT))
        assertEquals(red, MotorVozATexto.mensajeDeError(SpeechRecognizer.ERROR_SERVER))
        assertTrue(red.contains("conexión"))
    }

    @Test
    fun `un codigo desconocido se informa con su numero`() {
        assertTrue(MotorVozATexto.mensajeDeError(999).contains("999"))
    }
}
