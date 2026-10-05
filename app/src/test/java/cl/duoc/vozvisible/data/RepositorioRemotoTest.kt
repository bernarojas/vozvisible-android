package cl.duoc.vozvisible.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Pruebas de la escritura contra el almacén remoto, con Mockito.
 *
 * Aquí no interesa qué guarda Firestore, sino que el repositorio lo llame
 * cuando corresponde y, sobre todo, que no lo llame cuando la validación local
 * rechaza la operación. Un objeto simulado permite comprobar exactamente eso
 * sin levantar red ni emulador: se verifica la interacción, no el resultado.
 */
class RepositorioRemotoTest {

    private lateinit var fuente: FuenteUsuarios

    @Before
    fun prepararRepositorio() {
        RepositorioUsuarios.restaurar()
        fuente = mock()
        RepositorioUsuarios.conectar(fuente)
    }

    private fun usuarioDePrueba(correo: String = "nuevo@correo.cl") = Usuario.desdeFormulario(
        nombre = "usuario de prueba",
        correo = correo,
        password = "Prueba2024",
        region = Region.MAULE,
        modoPreferido = ModoComunicacion.TEXTO_A_VOZ,
        preferencias = listOf(ApoyoAccesibilidad.ALTO_CONTRASTE)
    )

    @Test
    fun `un alta valida se propaga al almacen`() = runTest {
        val nuevo = usuarioDePrueba()

        val resultado = RepositorioUsuarios.registrarEnAlmacen(nuevo)

        assertTrue(resultado is ResultadoRegistro.Exitoso)
        verify(fuente).guardar(nuevo)
    }

    @Test
    fun `un correo duplicado no llega al almacen`() = runTest {
        val repetido = usuarioDePrueba("CAMILA.REYES@duocuc.cl")

        val resultado = RepositorioUsuarios.registrarEnAlmacen(repetido)

        assertTrue(resultado is ResultadoRegistro.CorreoDuplicado)
        // La regla de negocio se aplica antes de tocar la red: si el repositorio
        // escribiera igual, el servidor acabaría con datos que el dominio rechaza.
        verify(fuente, never()).guardar(any())
    }

    @Test
    fun `una modificacion valida se propaga con el correo original`() = runTest {
        val datosNuevos = usuarioDePrueba("camila.reyes@duocuc.cl").copy(nombre = "Camila Reyes Díaz")

        val resultado =
            RepositorioUsuarios.actualizarEnAlmacen("camila.reyes@duocuc.cl", datosNuevos)

        assertTrue(resultado.fueExitoso)
        verify(fuente).actualizar(eq("camila.reyes@duocuc.cl"), eq(datosNuevos))
    }

    @Test
    fun `modificar una cuenta inexistente no llega al almacen`() = runTest {
        val resultado =
            RepositorioUsuarios.actualizarEnAlmacen("fantasma@correo.cl", usuarioDePrueba())

        assertTrue(resultado is ResultadoEdicion.NoEncontrado)
        verify(fuente, never()).actualizar(any(), any())
    }

    @Test
    fun `un borrado valido se propaga al almacen`() = runTest {
        val resultado = RepositorioUsuarios.eliminarDelAlmacen("matias.fuentes@duocuc.cl")

        assertTrue(resultado is ResultadoEdicion.Eliminado)
        assertEquals(4, RepositorioUsuarios.total)
        verify(fuente).eliminar("matias.fuentes@duocuc.cl")
    }

    @Test
    fun `borrar una cuenta inexistente no llega al almacen`() = runTest {
        val resultado = RepositorioUsuarios.eliminarDelAlmacen("fantasma@correo.cl")

        assertTrue(resultado is ResultadoEdicion.NoEncontrado)
        verify(fuente, never()).eliminar(any())
    }

    @Test
    fun `sincronizar reemplaza el arreglo con lo que entrega el almacen`() = runTest {
        val remotos = listOf(
            usuarioDePrueba("uno@correo.cl"),
            usuarioDePrueba("dos@correo.cl")
        )
        whenever(fuente.cargarTodos()).thenReturn(remotos)

        val sincronizo = RepositorioUsuarios.sincronizar()

        assertTrue(sincronizo)
        assertEquals(2, RepositorioUsuarios.total)
        assertEquals(remotos.map { it.correo }, RepositorioUsuarios.lista.map { it.correo })
    }

    @Test
    fun `un almacen vacio deja intacto el arreglo precargado`() = runTest {
        whenever(fuente.cargarTodos()).thenReturn(emptyList())

        RepositorioUsuarios.sincronizar()

        // No se vacía la lista: si el servidor no tiene nada, la aplicación
        // sigue siendo usable con los usuarios iniciales.
        assertEquals(5, RepositorioUsuarios.total)
    }

    @Test
    fun `un fallo del almacen no tumba la sincronizacion`() = runTest {
        whenever(fuente.cargarTodos()).thenThrow(RuntimeException("sin conexión"))

        val sincronizo = RepositorioUsuarios.sincronizar()

        assertFalse(sincronizo)
        assertEquals(5, RepositorioUsuarios.total)
    }
}
