package cl.duoc.vozvisible.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme
import kotlinx.coroutines.launch

/**
 * Función de accesibilidad ofrecida por la aplicación.
 *
 * @param titulo nombre visible en la tarjeta de la grilla.
 * @param icono pictograma representativo de la función.
 * @param descripcion texto para lectores de pantalla y detalle de la acción.
 */
private data class FuncionApp(
    val titulo: String,
    val icono: ImageVector,
    val descripcion: String
)

/** Catálogo de funciones que se despliegan en la grilla de inicio. */
private val FUNCIONES = listOf(
    FuncionApp(
        titulo = "Voz a texto",
        icono = Icons.Filled.Mic,
        descripcion = "Transcribe en pantalla lo que dice tu interlocutor"
    ),
    FuncionApp(
        titulo = "Texto a voz",
        icono = Icons.Filled.RecordVoiceOver,
        descripcion = "Reproduce en voz alta el mensaje que escribas"
    ),
    FuncionApp(
        titulo = "Frases rápidas",
        icono = Icons.Filled.Star,
        descripcion = "Guarda frases de uso frecuente para responder al instante"
    ),
    FuncionApp(
        titulo = "Alertas visuales",
        icono = Icons.Filled.Vibration,
        descripcion = "Avisa con vibración y destellos ante sonidos del entorno"
    ),
    FuncionApp(
        titulo = "Subtítulos",
        icono = Icons.Filled.ClosedCaption,
        descripcion = "Genera subtítulos en tiempo real durante una conversación"
    ),
    FuncionApp(
        titulo = "Configuración",
        icono = Icons.Filled.Settings,
        descripcion = "Ajusta contraste, tamaño de texto y preferencias"
    )
)

/**
 * View principal posterior al acceso.
 *
 * Presenta las funciones de la aplicación en una grilla de dos columnas.
 * En esta entrega las tarjetas no ejecutan la función todavía: informan
 * mediante un Snackbar que estarán disponibles en la siguiente iteración.
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

    // Se resuelve el nombre a partir del correo recibido. Si el usuario no
    // existiera en el arreglo, se cae al propio correo como respaldo.
    val nombre = RepositorioUsuarios.buscarPorCorreo(correoUsuario)?.nombre ?: correoUsuario

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Hola, $nombre",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Elige cómo quieres comunicarte",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            // Grilla adaptativa: dos columnas fijas, con separación uniforme.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(FUNCIONES) { funcion ->
                    TarjetaFuncion(
                        funcion = funcion,
                        onClick = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    "${funcion.titulo}: disponible en la próxima entrega"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/** Tarjeta individual de la grilla de funciones. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaFuncion(
    funcion: FuncionApp,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
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
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = funcion.titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = funcion.descripcion,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun InicioScreenPreview() {
    VozVisibleTheme {
        InicioScreen(correoUsuario = "prueba@correo.cl", onCerrarSesion = {})
    }
}
