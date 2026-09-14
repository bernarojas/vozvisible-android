package cl.duoc.vozvisible.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests del arreglo de usuarios y de sus operaciones de colección.
 *
 * El repositorio es un singleton compartido, por lo que cada test parte
 * restaurándolo a los cinco usuarios iniciales.
 */
class RepositorioUsuariosTest {

    @Before
    fun prepararArreglo() {
        RepositorioUsuarios.restaurar()
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
    fun `el arreglo parte con los cinco usuarios exigidos`() {
        assertEquals(5, RepositorioUsuarios.total)
        assertEquals(5, RepositorioUsuarios.usuariosPrecargados)
    }

    @Test
    fun `los correos precargados no se repiten`() {
        val correos = RepositorioUsuarios.lista.map { usuario -> usuario.correoNormalizado }
        assertEquals(correos.size, correos.distinct().size)
    }

    @Test
    fun `registrar agrega el usuario y devuelve el desenlace exitoso`() {
        val resultado = RepositorioUsuarios.registrar(usuarioDePrueba())

        assertTrue(resultado is ResultadoRegistro.Exitoso)
        assertEquals(6, RepositorioUsuarios.total)
        assertEquals(6, (resultado as ResultadoRegistro.Exitoso).totalRegistrados)
    }

    @Test
    fun `registrar rechaza un correo ya presente sin importar mayusculas`() {
        val resultado = RepositorioUsuarios.registrar(usuarioDePrueba("CAMILA.REYES@duocuc.cl"))

        assertTrue(resultado is ResultadoRegistro.CorreoDuplicado)
        assertEquals(5, RepositorioUsuarios.total)
    }

    @Test
    fun `registrar rechaza cuando se agota el cupo`() {
        // Se llena el arreglo hasta el máximo antes de intentar uno más.
        repeat(RepositorioUsuarios.cuposDisponibles) { indice ->
            RepositorioUsuarios.registrar(usuarioDePrueba("relleno$indice@correo.cl"))
        }

        assertFalse(RepositorioUsuarios.hayCupo())

        val resultado = RepositorioUsuarios.registrar(usuarioDePrueba("sobrante@correo.cl"))

        assertTrue(resultado is ResultadoRegistro.SinCupo)
        assertEquals(RepositorioUsuarios.MAX_USUARIOS, RepositorioUsuarios.total)
    }

    @Test
    fun `autenticar devuelve el usuario cuando las credenciales coinciden`() {
        val usuario = RepositorioUsuarios.autenticar("CAMILA.REYES@duocuc.cl", "Camila2024")

        assertNotNull(usuario)
        assertEquals("Camila Reyes", usuario?.nombre)
    }

    @Test
    fun `autenticar distingue mayusculas en la password`() {
        assertNull(RepositorioUsuarios.autenticar("camila.reyes@duocuc.cl", "camila2024"))
    }

    @Test
    fun `filtrar aplica el criterio recibido`() {
        val metropolitanos = RepositorioUsuarios.filtrar { usuario ->
            usuario.region == Region.METROPOLITANA
        }

        assertEquals(2, metropolitanos.size)
    }

    @Test
    fun `ordenadosPor respeta el selector y la direccion`() {
        val ascendente = RepositorioUsuarios.ordenadosPor { usuario -> usuario.nombre }
        val descendente = RepositorioUsuarios.ordenadosPor(descendente = true) { usuario ->
            usuario.nombre
        }

        assertEquals("Camila Reyes", ascendente.first().nombre)
        assertEquals(ascendente.reversed(), descendente)
    }

    @Test
    fun `conApoyo devuelve solo a quienes marcaron ese apoyo`() {
        val conTextoAmpliado = RepositorioUsuarios.conApoyo(ApoyoAccesibilidad.TEXTO_AMPLIADO)

        assertTrue(
            conTextoAmpliado.all { usuario ->
                usuario.tieneApoyo(ApoyoAccesibilidad.TEXTO_AMPLIADO)
            }
        )
        assertEquals(3, conTextoAmpliado.size)
    }

    @Test
    fun `indicePorCorreo permite una busqueda directa`() {
        val indice = RepositorioUsuarios.indicePorCorreo()

        assertEquals(RepositorioUsuarios.total, indice.size)
        assertEquals("Valentina Soto", indice["valentina.soto@duocuc.cl"]?.nombre)
    }

    @Test
    fun `la extension buscarCorreo opera sobre una lista ya filtrada`() {
        val delSur = RepositorioUsuarios.filtrar { usuario -> usuario.region.zona == "Sur" }

        assertNotNull(delSur.buscarCorreo("VALENTINA.SOTO@duocuc.cl"))
        assertNull(delSur.buscarCorreo("camila.reyes@duocuc.cl"))
    }
}
