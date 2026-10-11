package cl.duoc.vozvisible.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de las funciones de extensión que validan los formularios.
 *
 * Al estar fuera de los composables se ejecutan en la JVM, sin emulador.
 */
class ExtensionesTextoTest {

    @Test
    fun `acepta correos con formato valido`() {
        val validos = listOf(
            "camila.reyes@duocuc.cl",
            "MATIAS@GMAIL.COM",
            "  espacios@correo.cl  "
        )

        validos.forEach { correo ->
            assertTrue("Debía aceptar $correo", correo.esCorreoValido())
        }
    }

    @Test
    fun `rechaza correos mal formados`() {
        val invalidos = listOf(
            "",
            "sinarroba.cl",
            "@sinusuario.cl",
            "sin@dominio",
            "doble@@arroba.cl",
            "punto@final."
        )

        invalidos.forEach { correo ->
            assertFalse("Debía rechazar $correo", correo.esCorreoValido())
        }
    }

    @Test
    fun `la password exige el largo minimo`() {
        assertFalse("12345".esPasswordValida())
        assertTrue("123456".esPasswordValida())
        assertFalse("      ".esPasswordValida())
    }

    @Test
    fun `la fortaleza sube con la variedad de caracteres`() {
        assertEquals("", "".fortalezaPassword())
        assertEquals("Contraseña débil", "abc".fortalezaPassword())
        assertEquals("Contraseña aceptable", "abc123".fortalezaPassword())
        assertEquals("Contraseña robusta", "Abcdefgh1!".fortalezaPassword())
    }

    @Test
    fun `normaliza nombres con espacios y mayusculas irregulares`() {
        assertEquals("Ana María Soto", "  ana   maría SOTO ".aNombrePropio())
        assertEquals("", "   ".aNombrePropio())
    }

    @Test
    fun `enmascara el usuario del correo y conserva el dominio`() {
        assertEquals(
            "ca" + "•".repeat(10) + "@duocuc.cl",
            "camila.reyes@duocuc.cl".correoEnmascarado()
        )
    }

    @Test
    fun `deja intacto un texto que no es un correo`() {
        assertEquals("no-es-correo", "no-es-correo".correoEnmascarado())
    }

    @Test
    fun `un texto corto se reproduce en un solo fragmento`() {
        assertEquals(listOf("Hola, buenos días."), "  Hola, buenos días.  ".fragmentosParaVoz())
        assertTrue("   ".fragmentosParaVoz().isEmpty())
    }

    @Test
    fun `un texto largo se corta en el ultimo signo de puntuacion`() {
        val fragmentos = "Necesito un turno. Vengo por un control médico".fragmentosParaVoz(largoMaximo = 25)

        assertEquals(listOf("Necesito un turno.", "Vengo por un control", "médico"), fragmentos)
    }

    @Test
    fun `ningun fragmento supera el largo maximo ni se pierde texto`() {
        val texto = "palabra ".repeat(120).trim()

        val fragmentos = texto.fragmentosParaVoz(largoMaximo = 50)

        assertTrue(fragmentos.all { fragmento -> fragmento.length <= 50 })
        assertEquals(texto, fragmentos.joinToString(" "))
    }

    @Test
    fun `una palabra mas larga que el limite se corta igual`() {
        assertEquals(listOf("abcde", "fghij", "k"), "abcdefghijk".fragmentosParaVoz(largoMaximo = 5))
    }

    @Test
    fun `el plural depende de la cantidad`() {
        assertEquals("1 fragmento", 1.enPlural("fragmento"))
        assertEquals("3 fragmentos", 3.enPlural("fragmento"))
        assertEquals("0 interacciones", 0.enPlural("interacción", "interacciones"))
    }
}
