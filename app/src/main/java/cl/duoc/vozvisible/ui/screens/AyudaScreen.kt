package cl.duoc.vozvisible.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme
import kotlinx.coroutines.launch

/**
 * Una página del tutorial.
 *
 * @property consejo recomendación práctica que acompaña la explicación.
 */
private data class PasoTutorial(
    val icono: ImageVector,
    val titulo: String,
    val explicacion: String,
    val consejo: String
)

private val PASOS_TUTORIAL = listOf(
    PasoTutorial(
        icono = Icons.Filled.WavingHand,
        titulo = "Bienvenido a VozVisible",
        explicacion = "VozVisible convierte tu teléfono en un puente de comunicación con " +
            "las personas oyentes: escribe para que la aplicación hable por ti, o deja que " +
            "transcriba lo que te dicen.",
        consejo = "Toda la información de la aplicación es visual. Nunca necesitas oír un sonido."
    ),
    PasoTutorial(
        icono = Icons.AutoMirrored.Filled.VolumeUp,
        titulo = "Escribir: Texto a voz",
        explicacion = "Escribe tu mensaje y pulsa «Reproducir en voz alta». El texto se muestra " +
            "en grande y una barra se mueve mientras suena; al terminar, desaparece.",
        consejo = "Sube el volumen del teléfono y acércalo a tu interlocutor."
    ),
    PasoTutorial(
        icono = Icons.Filled.Mic,
        titulo = "Hablar: Voz a texto",
        explicacion = "Pulsa el micrófono y pide a la otra persona que hable. Sus palabras " +
            "aparecen en pantalla mientras las dice. Pulsa «Listo» cuando termine.",
        consejo = "La primera vez Android te pedirá permiso para usar el micrófono."
    ),
    PasoTutorial(
        icono = Icons.Filled.Star,
        titulo = "Frases rápidas",
        explicacion = "Las frases que usas a menudo están a un toque en Texto a voz. Marca " +
            "con una estrella cualquier mensaje del historial para sumarlo a ellas.",
        consejo = "Prepara antes las frases que necesitarás en un trámite o una consulta."
    ),
    PasoTutorial(
        icono = Icons.Filled.History,
        titulo = "Tu historial",
        explicacion = "Cada mensaje que reproduces y cada transcripción quedan guardados con " +
            "su fecha. Puedes filtrarlos, marcarlos como favoritos o eliminarlos.",
        consejo = "El historial se guarda en tu cuenta: lo verás en cualquier teléfono."
    ),
    PasoTutorial(
        icono = Icons.Filled.ManageAccounts,
        titulo = "Tu perfil",
        explicacion = "Desde el ícono de perfil puedes cambiar tus datos, tu modo de " +
            "comunicación preferido y tus apoyos de accesibilidad.",
        consejo = "El menú principal ordena las funciones según el modo que elijas."
    )
)

/**
 * View de ayuda: tutorial de uso en páginas deslizables.
 *
 * Usa un HorizontalPager, que permite avanzar deslizando el dedo o con los
 * botones. Los puntos inferiores indican la página actual y cambian de tamaño y
 * color con una animación.
 *
 * @param onVolver se invoca al pulsar el botón de retroceso.
 * @param onTerminar se invoca al completar el tutorial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaScreen(onVolver: () -> Unit, onTerminar: () -> Unit) {
    val paginas = rememberPagerState(pageCount = { PASOS_TUTORIAL.size })
    val alcance = rememberCoroutineScope()
    val esUltima = paginas.currentPage == PASOS_TUTORIAL.lastIndex

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y tutorial") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            HorizontalPager(
                state = paginas,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { indice ->
                PaginaTutorial(PASOS_TUTORIAL[indice])
            }

            IndicadorPaginas(total = PASOS_TUTORIAL.size, actual = paginas.currentPage)

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { alcance.launch { paginas.animateScrollToPage(paginas.currentPage - 1) } },
                    enabled = paginas.currentPage > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                ) { Text("Anterior") }

                Button(
                    onClick = {
                        if (esUltima) onTerminar()
                        else alcance.launch { paginas.animateScrollToPage(paginas.currentPage + 1) }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                ) { Text(if (esUltima) "Ir al menú" else "Siguiente") }
            }
        }
    }
}

@Composable
private fun PaginaTutorial(paso: PasoTutorial) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(112.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = paso.icono,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = paso.titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = paso.explicacion,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = paso.consejo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/** Puntos que indican la página actual; el activo se alarga y cambia de color. */
@Composable
private fun IndicadorPaginas(total: Int, actual: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        repeat(total) { indice ->
            val activo = indice == actual
            val ancho by animateDpAsState(if (activo) 24.dp else 8.dp, label = "ancho del punto")
            val color by animateColorAsState(
                if (activo) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                label = "color del punto"
            )
            Box(
                Modifier
                    .height(8.dp)
                    .width(ancho)
                    .background(color, CircleShape)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AyudaScreenPreview() {
    VozVisibleTheme {
        AyudaScreen(onVolver = {}, onTerminar = {})
    }
}
