package cl.duoc.vozvisible.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Tests del resumen agregado que alimenta la tarjeta de la view de Inicio. */
class EstadisticasUsuariosTest {

    private lateinit var estadisticas: EstadisticasUsuarios

    @Before
    fun calcularSobreElArregloInicial() {
        RepositorioUsuarios.restaurar()
        estadisticas = RepositorioUsuarios.estadisticas()
    }

    @Test
    fun `cuenta el total del arreglo`() {
        assertEquals(5, estadisticas.total)
    }

    @Test
    fun `agrupa por region de mayor a menor`() {
        assertEquals(2, estadisticas.porRegion[Region.METROPOLITANA])
        assertEquals(1, estadisticas.porRegion[Region.VALPARAISO])
        assertEquals(Region.METROPOLITANA, estadisticas.regionMayoritaria)
    }

    @Test
    fun `agrupa por zona geografica`() {
        assertEquals(3, estadisticas.porZona["Centro"])
        assertEquals(2, estadisticas.porZona["Sur"])
        assertNull(estadisticas.porZona["Norte"])
    }

    @Test
    fun `cuenta todos los modos, incluso los que nadie eligio`() {
        assertEquals(ModoComunicacion.entries.size, estadisticas.porModo.size)
        assertEquals(2, estadisticas.porModo[ModoComunicacion.VOZ_A_TEXTO])
        assertEquals(1, estadisticas.porModo[ModoComunicacion.TEXTO_A_VOZ])
        assertEquals(2, estadisticas.porModo[ModoComunicacion.AMBOS])
    }

    @Test
    fun `ordena los apoyos por solicitudes y desempata por titulo`() {
        val titulos = estadisticas.apoyosMasSolicitados.map { (apoyo, _) -> apoyo }

        assertEquals(ApoyoAccesibilidad.entries.size, titulos.size)
        assertEquals(ApoyoAccesibilidad.ALERTAS_VIBRATORIAS, titulos.first())
        assertEquals(ApoyoAccesibilidad.ALTO_CONTRASTE, titulos.last())
    }

    @Test
    fun `calcula el promedio de apoyos por persona`() {
        // 2 + 1 + 3 + 1 + 4 apoyos repartidos entre 5 usuarios.
        assertEquals(2.2, estadisticas.promedioApoyos, 0.001)
    }

    @Test
    fun `calcula el porcentaje de solicitud de un apoyo`() {
        assertEquals(60, estadisticas.porcentajeDe(ApoyoAccesibilidad.SUBTITULOS_AUTOMATICOS))
        assertEquals(40, estadisticas.porcentajeDe(ApoyoAccesibilidad.ALTO_CONTRASTE))
    }

    @Test
    fun `reune los dominios de correo sin repetir`() {
        assertEquals(setOf("duocuc.cl"), estadisticas.dominiosCorreo)
    }

    @Test
    fun `soporta un arreglo vacio sin dividir por cero`() {
        val vacias = EstadisticasUsuarios.calcular(emptyList())

        assertTrue(vacias.estaVacio)
        assertEquals(0.0, vacias.promedioApoyos, 0.001)
        assertEquals(0, vacias.porcentajeDe(ApoyoAccesibilidad.ALTO_CONTRASTE))
        assertNull(vacias.regionMayoritaria)
    }
}
