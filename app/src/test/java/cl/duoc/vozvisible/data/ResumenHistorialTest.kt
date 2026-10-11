package cl.duoc.vozvisible.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas del resumen del historial y del conteo de palabras. */
class ResumenHistorialTest {

    private fun interaccion(tipo: TipoInteraccion, texto: String, favorita: Boolean = false) =
        Interaccion.nueva(
            correoUsuario = "valentina.soto@duocuc.cl",
            tipo = tipo,
            solicitud = if (tipo == TipoInteraccion.TEXTO_A_VOZ) texto else tipo.solicitudTipica,
            respuesta = if (tipo == TipoInteraccion.VOZ_A_TEXTO) texto else "Reproducido"
        ).copy(favorita = favorita)

    @Test
    fun `un historial vacio produce un resumen vacio`() {
        val resumen = ResumenHistorial.calcular(emptyList())

        assertTrue(resumen.estaVacio)
        assertEquals("Todavía no tienes interacciones guardadas", resumen.descripcion)
    }

    @Test
    fun `el resumen cuenta cada tipo, las favoritas y las palabras`() {
        val historial = listOf(
            interaccion(TipoInteraccion.TEXTO_A_VOZ, "Buenos días", favorita = true),
            interaccion(TipoInteraccion.TEXTO_A_VOZ, "¿Cuánto cuesta?"),
            interaccion(TipoInteraccion.VOZ_A_TEXTO, "Son tres mil pesos")
        )

        val resumen = ResumenHistorial.calcular(historial)

        assertEquals(3, resumen.total)
        assertEquals(2, resumen.mensajesEscritos)
        assertEquals(1, resumen.transcripciones)
        assertEquals(1, resumen.favoritas)
        // 2 + 2 + 4: en Voz a texto cuenta lo transcrito, no la solicitud fija.
        assertEquals(8, resumen.palabrasComunicadas)
    }

    @Test
    fun `contar palabras ignora signos y espacios repetidos`() {
        assertEquals(0, ResumenHistorial.contarPalabras(""))
        assertEquals(0, ResumenHistorial.contarPalabras("  ¿? ¡! "))
        assertEquals(3, ResumenHistorial.contarPalabras("  Hola,   ¿cómo   estás?  "))
        assertEquals(4, ResumenHistorial.contarPalabras("Tengo 2 hijos pequeños"))
    }
}
