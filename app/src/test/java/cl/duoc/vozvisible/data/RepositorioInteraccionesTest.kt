package cl.duoc.vozvisible.data

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Pruebas del historial de interacciones, con el almacén remoto simulado.
 *
 * Igual que en [RepositorioRemotoTest], lo que se verifica es la interacción
 * con el almacén: que lo válido llegue y que lo inválido no salga del
 * dispositivo. Se suman casos propios de este repositorio, como el plazo
 * máximo de espera cuando el servidor no responde.
 */
class RepositorioInteraccionesTest {

    private lateinit var fuente: FuenteInteracciones

    private val correo = "camila.reyes@duocuc.cl"

    @Before
    fun prepararRepositorio() {
        RepositorioInteracciones.restaurar()
        fuente = mock()
        RepositorioInteracciones.conectar(fuente)
    }

    private fun escrita(texto: String = "Hola, ¿me puede ayudar?", fecha: Long = 1_000L) =
        Interaccion.nueva(
            correoUsuario = correo,
            tipo = TipoInteraccion.TEXTO_A_VOZ,
            solicitud = texto,
            respuesta = "Reproducido en voz alta en 1 fragmento",
            fecha = fecha
        )

    private fun escuchada(texto: String = "Su número de atención es el doce", fecha: Long = 2_000L) =
        Interaccion.nueva(
            correoUsuario = correo,
            tipo = TipoInteraccion.VOZ_A_TEXTO,
            solicitud = TipoInteraccion.VOZ_A_TEXTO.solicitudTipica,
            respuesta = texto,
            fecha = fecha
        )

    @Test
    fun `una interaccion valida se guarda y llega al almacen`() = runTest {
        val nueva = escrita()

        val resultado = RepositorioInteracciones.registrar(nueva)

        assertEquals(ResultadoInteraccion.Guardada(nueva, Sincronizacion.CONFIRMADA), resultado)
        assertEquals(listOf(nueva), RepositorioInteracciones.delUsuario(correo))
        verify(fuente).guardar(nueva)
    }

    @Test
    fun `una solicitud vacia no se guarda en ninguna parte`() = runTest {
        val resultado = RepositorioInteracciones.registrar(escrita(texto = "   "))

        assertTrue(resultado is ResultadoInteraccion.Rechazada)
        assertTrue(RepositorioInteracciones.delUsuario(correo).isEmpty())
        verify(fuente, never()).guardar(any())
    }

    @Test
    fun `un mensaje demasiado largo se rechaza`() = runTest {
        val largo = "a".repeat(Interaccion.LARGO_MAXIMO + 1)

        val resultado = RepositorioInteracciones.registrar(escrita(texto = largo))

        assertTrue(resultado is ResultadoInteraccion.Rechazada)
        verify(fuente, never()).guardar(any())
    }

    @Test
    fun `si el servidor no responde a tiempo la copia local se conserva`() = runTest {
        // El almacén simulado nunca termina, como Firestore sin conexión. El
        // tiempo virtual de runTest hace vencer el plazo sin esperar de verdad.
        whenever(fuente.guardar(any())).doSuspendableAnswer { awaitCancellation() }
        val nueva = escrita()

        val resultado = RepositorioInteracciones.registrar(nueva)

        assertEquals(ResultadoInteraccion.Guardada(nueva, Sincronizacion.PENDIENTE), resultado)
        assertEquals(1, RepositorioInteracciones.delUsuario(correo).size)
    }

    @Test
    fun `un error del servidor no impide guardar en el dispositivo`() = runTest {
        whenever(fuente.guardar(any())).thenThrow(RuntimeException("sin conexión"))

        val nueva = escrita()

        val resultado = RepositorioInteracciones.registrar(nueva)

        // Un rechazo no se reintenta: el resultado no debe prometer sincronizar.
        assertEquals(ResultadoInteraccion.Guardada(nueva, Sincronizacion.RECHAZADA), resultado)
        assertEquals(1, RepositorioInteracciones.delUsuario(correo).size)
    }

    @Test
    fun `el historial se entrega del mas reciente al mas antiguo`() = runTest {
        val antigua = escrita(fecha = 1_000L)
        val reciente = escuchada(fecha = 5_000L)
        RepositorioInteracciones.registrar(antigua)
        RepositorioInteracciones.registrar(reciente)

        assertEquals(listOf(reciente, antigua), RepositorioInteracciones.delUsuario(correo))
    }

    @Test
    fun `marcar como favorita actualiza la copia local y el servidor`() = runTest {
        val nueva = escrita()
        RepositorioInteracciones.registrar(nueva)

        val actualizada = RepositorioInteracciones.alternarFavorita(nueva.id)

        assertTrue(actualizada?.favorita == true)
        verify(fuente).marcarFavorita(nueva.id, true)
    }

    @Test
    fun `marcar una interaccion inexistente no llega al servidor`() = runTest {
        assertNull(RepositorioInteracciones.alternarFavorita("no-existe"))
        verify(fuente, never()).marcarFavorita(any(), any())
    }

    @Test
    fun `eliminar quita la interaccion y propaga el borrado`() = runTest {
        val nueva = escrita()
        RepositorioInteracciones.registrar(nueva)

        assertTrue(RepositorioInteracciones.eliminar(nueva.id))
        assertTrue(RepositorioInteracciones.delUsuario(correo).isEmpty())
        verify(fuente).eliminar(nueva.id)
    }

    @Test
    fun `eliminar una interaccion inexistente no llega al servidor`() = runTest {
        assertFalse(RepositorioInteracciones.eliminar("no-existe"))
        verify(fuente, never()).eliminar(any())
    }

    @Test
    fun `vaciar borra solo el historial del usuario indicado`() = runTest {
        RepositorioInteracciones.registrar(escrita())
        RepositorioInteracciones.registrar(escuchada())
        val ajena = Interaccion.nueva("otra@correo.cl", TipoInteraccion.TEXTO_A_VOZ, "Hola", "Reproducido")
        RepositorioInteracciones.registrar(ajena)

        val eliminadas = RepositorioInteracciones.vaciar("CAMILA.REYES@duocuc.cl")

        assertEquals(2, eliminadas)
        assertEquals(listOf(ajena), RepositorioInteracciones.delUsuario("otra@correo.cl"))
        verify(fuente).vaciar(correo)
    }

    @Test
    fun `reasignar traspasa el historial al correo nuevo`() = runTest {
        RepositorioInteracciones.registrar(escrita())
        RepositorioInteracciones.registrar(escuchada())

        RepositorioInteracciones.reasignar(correo, "camila.nueva@duocuc.cl")

        assertTrue(RepositorioInteracciones.delUsuario(correo).isEmpty())
        assertEquals(2, RepositorioInteracciones.delUsuario("camila.nueva@duocuc.cl").size)
        verify(fuente).reasignar(correo, "camila.nueva@duocuc.cl")
    }

    @Test
    fun `cargar reemplaza la copia local con lo que entrega el servidor`() = runTest {
        RepositorioInteracciones.registrar(escrita(texto = "Copia local antigua"))
        val remotas = listOf(escuchada(), escrita(texto = "Del servidor"))
        whenever(fuente.cargarDe(correo)).thenReturn(remotas)

        val cargo = RepositorioInteracciones.cargar(correo)

        assertTrue(cargo)
        assertEquals(
            setOf("Del servidor", "Su número de atención es el doce"),
            RepositorioInteracciones.delUsuario(correo).map { it.textoReutilizable }.toSet()
        )
    }

    @Test
    fun `un fallo al cargar apaga el indicador de carga`() = runTest {
        whenever(fuente.cargarDe(any())).thenThrow(RuntimeException("sin conexión"))

        val cargo = RepositorioInteracciones.cargar(correo)

        assertFalse(cargo)
        // El finally de cargar() debe haber devuelto el indicador a falso.
        assertFalse(RepositorioInteracciones.cargando)
    }

    @Test
    fun `las frases favoritas aparecen antes que las predeterminadas y sin repetirse`() = runTest {
        val propia = escrita(texto = "Soy alérgica a la penicilina")
        val repetida = escrita(texto = "GRACIAS POR SU PACIENCIA.")
        RepositorioInteracciones.registrar(propia)
        RepositorioInteracciones.registrar(repetida)
        RepositorioInteracciones.alternarFavorita(propia.id)
        RepositorioInteracciones.alternarFavorita(repetida.id)

        val frases = RepositorioInteracciones.frasesRapidasDe(correo)

        assertEquals("Soy alérgica a la penicilina", frases.first())
        assertEquals(
            1,
            frases.count { it.equals("Gracias por su paciencia.", ignoreCase = true) }
        )
    }
}
