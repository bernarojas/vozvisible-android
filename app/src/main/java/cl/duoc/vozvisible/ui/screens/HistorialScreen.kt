package cl.duoc.vozvisible.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.Interaccion
import cl.duoc.vozvisible.data.RepositorioInteracciones
import cl.duoc.vozvisible.data.ResumenHistorial
import cl.duoc.vozvisible.data.TipoInteraccion
import cl.duoc.vozvisible.ui.componentes.BotonVolverAlMenu
import cl.duoc.vozvisible.util.comoFechaHora
import cl.duoc.vozvisible.util.enPlural
import kotlinx.coroutines.launch

/**
 * Filtros del historial.
 *
 * Cada constante guarda su propio criterio como una función lambda. La view
 * aplica el criterio del filtro activo sin preguntar cuál es: agregar un filtro
 * nuevo consiste en agregar una constante, sin tocar la lista ni el `when`.
 */
private enum class FiltroHistorial(val titulo: String, val criterio: (Interaccion) -> Boolean) {
    TODAS("Todas", { true }),
    ESCRITAS("Texto a voz", { it.tipo == TipoInteraccion.TEXTO_A_VOZ }),
    ESCUCHADAS("Voz a texto", { it.tipo == TipoInteraccion.VOZ_A_TEXTO }),
    FAVORITAS("Favoritas", { it.favorita })
}

/**
 * View Historial: consulta, marca y elimina las interacciones guardadas.
 *
 * Al abrirse trae del servidor el historial del usuario. Mientras tanto
 * muestra la copia local, de modo que lo recién guardado aparece aunque la red
 * tarde o no responda.
 *
 * @param correoUsuario dueño del historial.
 * @param onVolver se invoca al pulsar el botón de retroceso.
 * @param onVolverAlMenu regresa al menú principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    correoUsuario: String,
    onVolver: () -> Unit,
    onVolverAlMenu: () -> Unit
) {
    val alcance = rememberCoroutineScope()
    val avisos = remember { SnackbarHostState() }

    var filtro by rememberSaveable { mutableStateOf(FiltroHistorial.TODAS) }
    var confirmandoVaciado by remember { mutableStateOf(false) }

    LaunchedEffect(correoUsuario) {
        RepositorioInteracciones.cargar(correoUsuario)
    }

    val historial = RepositorioInteracciones.delUsuario(correoUsuario)
    val visibles = historial.filter(filtro.criterio)
    val resumen = ResumenHistorial.calcular(historial)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { confirmandoVaciado = true },
                        enabled = historial.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "Vaciar el historial")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(avisos) }
    ) { padding ->
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            item {
                AnimatedVisibility(visible = RepositorioInteracciones.cargando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            item { TarjetaResumen(resumen) }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FiltroHistorial.entries) { opcion ->
                        FilterChip(
                            selected = filtro == opcion,
                            onClick = { filtro = opcion },
                            label = { Text(opcion.titulo) }
                        )
                    }
                }
            }

            if (visibles.isEmpty()) {
                item {
                    Text(
                        text = if (historial.isEmpty()) {
                            "Aún no hay interacciones. Usa Texto a voz o Voz a texto y aparecerán aquí."
                        } else {
                            "No hay interacciones que coincidan con el filtro."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            }

            // La clave estable por id permite a LazyColumn conservar cada fila
            // al eliminar otra, en vez de redibujar toda la lista.
            items(visibles, key = { interaccion -> interaccion.id }) { interaccion ->
                FilaInteraccion(
                    interaccion = interaccion,
                    onAlternarFavorita = {
                        alcance.launch { RepositorioInteracciones.alternarFavorita(interaccion.id) }
                    },
                    onEliminar = {
                        alcance.launch {
                            RepositorioInteracciones.eliminar(interaccion.id)
                            avisos.showSnackbar("Interacción eliminada")
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(4.dp))
                BotonVolverAlMenu(onClick = onVolverAlMenu)
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (confirmandoVaciado) {
        AlertDialog(
            onDismissRequest = { confirmandoVaciado = false },
            title = { Text("¿Vaciar el historial?") },
            text = {
                Text(
                    "Se eliminarán ${historial.size.enPlural("interacción", "interacciones")}. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoVaciado = false
                    alcance.launch {
                        val eliminadas = RepositorioInteracciones.vaciar(correoUsuario)
                        avisos.showSnackbar(
                            "Se eliminó el historial: ${eliminadas.enPlural("interacción", "interacciones")}"
                        )
                    }
                }) {
                    Text("Vaciar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoVaciado = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Cabecera con las cifras del historial. */
@Composable
private fun TarjetaResumen(resumen: ResumenHistorial) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "Tus solicitudes y respuestas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = resumen.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!resumen.estaVacio) {
                Text(
                    text = "Palabras comunicadas: ${resumen.palabrasComunicadas} · " +
                        resumen.favoritas.enPlural("favorita"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Una interacción con sus acciones de favorita y borrado. */
@Composable
private fun FilaInteraccion(
    interaccion: Interaccion,
    onAlternarFavorita: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 12.dp, end = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when (interaccion.tipo) {
                        TipoInteraccion.TEXTO_A_VOZ -> Icons.AutoMirrored.Filled.VolumeUp
                        TipoInteraccion.VOZ_A_TEXTO -> Icons.Filled.Mic
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = interaccion.tipo.titulo,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = interaccion.fecha.comoFechaHora(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAlternarFavorita) {
                    Icon(
                        imageVector = if (interaccion.favorita) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (interaccion.favorita) "Quitar de frases rápidas"
                        else "Guardar como frase rápida",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(Icons.Filled.Delete, contentDescription = "Eliminar interacción")
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                text = "Solicitud: ${interaccion.solicitud}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "Respuesta: ${interaccion.respuesta}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}
