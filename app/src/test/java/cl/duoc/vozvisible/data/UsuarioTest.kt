package cl.duoc.vozvisible.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests del modelo de usuario y de sus propiedades calculadas. */
class UsuarioTest {

    private val camila = Usuario(
        nombre = "Camila Reyes",
        correo = "camila.reyes@duocuc.cl",
        password = "Camila2024",
        region = Region.METROPOLITANA,
        modoPreferido = ModoComunicacion.VOZ_A_TEXTO,
        preferencias = setOf(
            ApoyoAccesibilidad.SUBTITULOS_AUTOMATICOS,
            ApoyoAccesibilidad.ALERTAS_VIBRATORIAS
        )
    )

    @Test
    fun `desdeFormulario normaliza el nombre y el correo`() {
        val usuario = Usuario.desdeFormulario(
            nombre = "  ana   maría SOTO ",
            correo = "  ANA.SOTO@Duocuc.CL ",
            password = "Clave2024",
            region = Region.COQUIMBO,
            modoPreferido = ModoComunicacion.AMBOS
        )

        assertEquals("Ana María Soto", usuario.nombre)
        assertEquals("ana.soto@duocuc.cl", usuario.correo)
        assertTrue(usuario.preferencias.isEmpty())
    }

    @Test
    fun `las iniciales toman el nombre y el primer apellido`() {
        assertEquals("CR", camila.iniciales)
        assertEquals("AM", camila.copy(nombre = "Ana María Soto Pérez").iniciales)
        assertEquals("?", camila.copy(nombre = "   ").iniciales)
    }

    @Test
    fun `el primer nombre corta en el primer espacio`() {
        assertEquals("Camila", camila.primerNombre)
    }

    @Test
    fun `autentica ignorando mayusculas en el correo pero no en la password`() {
        assertTrue(camila.autenticaCon("CAMILA.REYES@DUOCUC.CL", "Camila2024"))
        assertFalse(camila.autenticaCon("camila.reyes@duocuc.cl", "camila2024"))
    }

    @Test
    fun `el nivel de personalizacion depende de los apoyos marcados`() {
        assertEquals(
            "Sin apoyos configurados",
            camila.copy(preferencias = emptySet()).nivelPersonalizacion
        )
        assertEquals("Personalización intermedia", camila.nivelPersonalizacion)
        assertEquals(
            "Personalización completa",
            camila.copy(preferencias = ApoyoAccesibilidad.entries.toSet()).nivelPersonalizacion
        )
    }

    @Test
    fun `los apoyos se listan en el orden de la check list`() {
        assertEquals("Alertas vibratorias, Subtítulos automáticos", camila.apoyosComoTexto())
        assertEquals("Sin apoyos", camila.copy(preferencias = emptySet()).apoyosComoTexto())
    }

    @Test
    fun `expone el dominio del correo`() {
        assertEquals("duocuc.cl", camila.dominioCorreo)
    }

    @Test
    fun `la zona se deriva de la region`() {
        assertEquals("Centro", camila.region.zona)
        assertEquals("Sur", Region.MAGALLANES.zona)
        assertEquals("Norte", Region.ANTOFAGASTA.zona)
    }
}
