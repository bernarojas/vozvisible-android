package cl.duoc.vozvisible.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.EstadisticasUsuarios
import cl.duoc.vozvisible.data.ModoComunicacion
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.data.Usuario
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme
import kotlinx.coroutines.launch

/**
 * Función de accesibilidad ofrecida por la aplicación.
 *
 * @property titulo nombre visible en la tarjeta de la grilla.
 * @property icono pictograma representativo de la función.
 * @property descripcion texto para lectores de pantalla y detalle de la acción.
 * @property modoRelacionado modo de comunicación al que sirve la función, o null
 *   si es transversal. Se usa para ordenar la grilla según la preferencia del usuario.
 */
private data class FuncionApp(
    val titulo: String,
    val icono: ImageVector,
    val descripcion: String,
    val modoRelacionado: ModoComunicacion? = null
)

/** Catálogo de funciones que se despliegan en la grilla de inicio. */
private val FUNCIONES = listOf(
    FuncionApp(
        titulo = "Voz a texto",
        icono = Icons.Filled.Mic,
        descripcion = "Transcribe en pantalla lo que dice tu interlocutor",
        modoRelacionado = ModoComunicacion.VOZ_A_TEXTO
    ),
    FuncionApp(
        titulo = "Texto a voz",
        icono = Icons.Filled.RecordVoiceOver,
        descripcion = "Reproduce en voz alta el mensaje que escribas",
        modoRelacionado = ModoComunicacion.TEXTO_A_VOZ
    ),
    FuncionApp(
        titulo = "Frases rápidas",
        icono = Icons.Filled.Star,
        descripcion = "Guarda frases de uso frecuente para responder al instante",
        modoRelacionado = ModoComunicacion.TEXTO_A_VOZ
    ),
    FuncionApp(
        titulo = "Alertas visuales",
        icono = Icons.Filled.Vibration,
        descripcion = "Avisa con vibración y destellos ante sonidos del entorno",
        modoRelacionado = ModoComunicacion.VOZ_A_TEXTO
    ),
    FuncionApp(
        titulo = "Subtítulos",
        icono = Icons.Filled.ClosedCaption,
        descripcion = "Genera subtítulos en tiempo real durante una conversación",
        modoRelacionado = ModoComunicacion.VOZ_A_TEXTO
    ),
    FuncionApp(
        titulo = "Configuración",
        icono = Icons.Filled.Settings,
        descripcion = "Ajusta contraste, tamaño de texto y preferencias"
    )
)

/**
 * Ordena el catálogo dejando adelante lo que sirve al modo preferido del usuario.
 *
 * `sortedWith` recibe un comparador construido por composición: primero agrupa
 * por afinidad con el modo elegido y, dentro de cada grupo, conserva el orden
 * alfabético para que la grilla no cambie de forma entre recomposiciones.
 *
 * @param modo preferencia del usuario, o null cuando no se conoce.
 */
private fun List<FuncionApp>.priorizadasPara(modo: ModoComunicacion?): List<FuncionApp> {
    if (modo == null) return this

    return sortedWith(
        compareByDescending<FuncionApp> { funcion ->
            when (funcion.modoRelacionado) {
                modo -> 2                      // sirve exactamente al modo elegido
                null -> 0                      // función transversal
                else -> if (modo == ModoComunicacion.AMBOS) 1 else 0
            }
        }.thenBy { funcion -> funcion.titulo }
    )
}

/**
 * View principal posterior al acceso.
 *
 * Presenta las funciones de la aplicación en una grilla de dos columnas,
 * precedida por un resumen del arreglo de usuarios calculado con operaciones
 * de colección. En esta entrega las tarjetas no ejecutan la función todavía:
 * informan mediante un Snackbar que estarán disponibles más adelante.
 *
 * @param correoUsuario correo recibido como argumento de navegación desde Login.
 * @param onCerrarSesion se invoca al pulsar el botón de salida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    correoUsuario: String,
    onCerrarSesion: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Se resuelve el usuario a partir del correo recibido. Si no existiera en el
    // arreglo se trabaja con null y la interfaz usa sus valores de respaldo.
    val usuario: Usuario? = RepositorioUsuarios.buscarPorCorreo(correoUsuario)

    // Las funciones se reordenan según la preferencia declarada en el registro.
    val funciones = FUNCIONES.priorizadasPara(usuario?.modoPreferido)
    val estadisticas = RepositorioUsuarios.estadisticas()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VozVisible") },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        // Una sola grilla contiene toda la pantalla: la cabecera y el resumen
        // ocupan filas completas mediante span, de modo que existe un único
        // contenedor con scroll y nada queda fuera de la vista en pantallas bajas.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {

            item(span = { GridItemSpan(maxLineSpan) }) {
                CabeceraUsuario(usuario = usuario, correoUsuario = correoUsuario)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                ResumenUsuarios(estadisticas)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Elige cómo quieres comunicarte",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(funciones) { funcion ->
                TarjetaFuncion(
                    funcion = funcion,
                    destacada = funcion.modoRelacionado != null &&
                        funcion.modoRelacionado == usuario?.modoPreferido,
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                "${funcion.titulo}: disponible en la próxima entrega"
                            )
                        }
                    }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** Saludo con las iniciales del usuario y los apoyos que dejó configurados. */
@Composable
private fun CabeceraUsuario(usuario: Usuario?, correoUsuario: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(52.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = usuario?.iniciales ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = "Hola, ${usuario?.primerNombre ?: correoUsuario}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                // Cuando no se conoce al usuario se omite el detalle en vez de
                // mostrar un texto vacío que descuadre la cabecera.
                text = usuario?.let { registrado ->
                    "${registrado.modoPreferido.titulo} · ${registrado.nivelPersonalizacion}"
                } ?: "Sesión iniciada",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Resumen del arreglo de usuarios.
 *
 * Todo lo que se muestra proviene de [EstadisticasUsuarios]: la view no calcula
 * nada, solo dibuja el resultado que ya vino agregado.
 */
@Composable
private fun ResumenUsuarios(estadisticas: EstadisticasUsuarios) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Comunidad VozVisible",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = buildString {
                    append("${estadisticas.total} personas registradas")
                    estadisticas.regionMayoritaria?.let { region ->
                        append(" · mayoría en ${region.nombre}")
                    }
                    append(" · ${estadisticas.promedioApoyosFormateado} apoyos por persona")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Apoyos más solicitados",
                style = MaterialTheme.typography.labelLarge
            )

            Spacer(Modifier.height(4.dp))

            // take(3) limita el resumen a los tres primeros: la lista ya viene
            // ordenada de mayor a menor desde el cálculo de estadísticas.
            estadisticas.apoyosMasSolicitados.take(3).forEach { (apoyo, solicitudes) ->
                val porcentaje = estadisticas.porcentajeDe(apoyo)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = apoyo.titulo,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1.2f)
                    )
                    LinearProgressIndicator(
                        progress = { porcentaje / 100f },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                    )
                    Text(
                        text = "  $solicitudes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta individual de la grilla de funciones.
 *
 * @param destacada resalta la tarjeta cuando coincide con el modo preferido.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaFuncion(
    funcion: FuncionApp,
    destacada: Boolean,
    onClick: () -> Unit
) {
    val colorContenedor = if (destacada) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.secondaryContainer
    val colorContenido = if (destacada) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSecondaryContainer

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colorContenedor),
        modifier = Modifier
            .fillMaxWidth()
            // aspectRatio 1f mantiene las tarjetas cuadradas en cualquier ancho
            // de pantalla, lo que sostiene el requisito de app adaptativa.
            .aspectRatio(1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = funcion.icono,
                contentDescription = null, // el título contiguo ya lo describe
                tint = colorContenido,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = funcion.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = colorContenido
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = funcion.descripcion,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = colorContenido
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InicioScreenPreview() {
    VozVisibleTheme {
        InicioScreen(correoUsuario = "camila.reyes@duocuc.cl", onCerrarSesion = {})
    }
}
