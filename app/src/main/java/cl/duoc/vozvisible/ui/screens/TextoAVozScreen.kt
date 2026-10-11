package cl.duoc.vozvisible.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.Interaccion
import cl.duoc.vozvisible.data.RepositorioInteracciones
import cl.duoc.vozvisible.data.TipoInteraccion
import cl.duoc.vozvisible.ui.componentes.AvisoResultado
import cl.duoc.vozvisible.ui.componentes.BotonVolverAlMenu
import cl.duoc.vozvisible.ui.componentes.TarjetaGuia
import cl.duoc.vozvisible.util.enPlural
import cl.duoc.vozvisible.voz.EstadoMotor
import cl.duoc.vozvisible.voz.MotorTextoAVoz
import kotlinx.coroutines.launch

/** Pasos que la view muestra como guía al usuario. */
private val PASOS_TEXTO_A_VOZ = listOf(
    "Escribe tu mensaje o toca una frase rápida.",
    "Pulsa «Reproducir en voz alta» y acerca el teléfono a tu interlocutor.",
    "Mira la barra: mientras se mueve, el mensaje sigue sonando. Al terminar, desaparece."
)

/**
 * View de Texto a voz: el usuario escribe y la aplicación habla por él.
 *
 * Cada mensaje reproducido se guarda en el historial como una interacción, con
 * el texto como solicitud y la confirmación como respuesta.
 *
 * Toda la retroalimentación es visual. Mientras el mensaje suena, la view
 * muestra el texto en tamaño grande, para que el interlocutor también pueda
 * leerlo, y una barra con el avance por fragmentos.
 *
 * @param correoUsuario dueño de las interacciones que se guarden.
 * @param onVolver se invoca al pulsar el botón de retroceso.
 * @param onVolverAlMenu se invoca al terminar, para regresar al menú principal.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TextoAVozScreen(
    correoUsuario: String,
    onVolver: () -> Unit,
    onVolverAlMenu: () -> Unit
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    // El motor se crea una sola vez por visita a la view y se libera al salir:
    // el sintetizador del sistema mantiene recursos que no deben quedar abiertos.
    val motor = remember { MotorTextoAVoz(contexto) }
    DisposableEffect(motor) {
        onDispose { motor.liberar() }
    }

    var texto by rememberSaveable { mutableStateOf("") }
    var textoEnCurso by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val frases = RepositorioInteracciones.frasesRapidasDe(correoUsuario)
    val estado = motor.estado

    // La barra avanza con una animación en lugar de saltar entre valores.
    val progreso by animateFloatAsState(
        targetValue = (estado as? EstadoMotor.Hablando)?.progreso ?: 0f,
        label = "avance de la reproducción"
    )

    fun reproducir() {
        val fragmentos = motor.hablar(texto)
        if (fragmentos == 0) {
            mensaje = motor.describirEstado()
            esError = true
            return
        }
        // Se fija el texto enviado: el usuario puede seguir escribiendo mientras
        // se guarda, y lo que corresponde registrar es lo que efectivamente sonó.
        val enviado = texto
        textoEnCurso = enviado

        alcance.launch {
            val resultado = try {
                guardando = true
                RepositorioInteracciones.registrar(
                    Interaccion.nueva(
                        correoUsuario = correoUsuario,
                        tipo = TipoInteraccion.TEXTO_A_VOZ,
                        solicitud = enviado,
                        respuesta = "Reproducido en voz alta en ${fragmentos.enPlural("fragmento")}"
                    )
                )
            } finally {
                guardando = false
            }
            mensaje = resultado.mensaje
            esError = !resultado.fueExitoso
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Texto a voz") },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            TarjetaGuia(titulo = "Cómo funciona", pasos = PASOS_TEXTO_A_VOZ)

            Text(
                text = motor.describirEstado(),
                style = MaterialTheme.typography.labelLarge,
                color = if (estado is EstadoMotor.NoDisponible || estado is EstadoMotor.Fallo)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Mientras suena, el mensaje se muestra grande para que la otra
            // persona también pueda leerlo, junto con el avance de la lectura.
            AnimatedVisibility(visible = estado is EstadoMotor.Hablando) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = textoEnCurso,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        // Un mensaje corto suena de una vez: la barra indeterminada
                        // indica que sigue sonando sin aparentar un avance que no
                        // se puede medir.
                        val hablando = estado as? EstadoMotor.Hablando
                        if (hablando?.avanceMedible == true) {
                            LinearProgressIndicator(
                                progress = { progreso },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = texto,
                onValueChange = { nuevo ->
                    // Se corta en el máximo en lugar de rechazar la escritura.
                    texto = nuevo.take(Interaccion.LARGO_MAXIMO)
                    mensaje = ""
                },
                label = { Text("Tu mensaje") },
                placeholder = { Text("Escribe lo que quieres decir") },
                supportingText = { Text("${texto.length} / ${Interaccion.LARGO_MAXIMO}") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = ::reproducir,
                    enabled = motor.estaListo && texto.isNotBlank() && !guardando,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text("Reproducir en voz alta", modifier = Modifier.padding(start = 8.dp))
                }

                AnimatedVisibility(visible = motor.estaTrabajando) {
                    OutlinedButton(
                        onClick = { motor.detener() },
                        modifier = Modifier.heightIn(min = 56.dp)
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Detener la reproducción")
                    }
                }
            }

            AvisoResultado(mensaje = mensaje, esError = esError)

            Text(
                text = "Frases rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Toca una frase para usarla. Las que marques con estrella en tu " +
                    "historial aparecen primero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                frases.forEach { frase ->
                    SuggestionChip(
                        onClick = {
                            texto = frase
                            mensaje = ""
                        },
                        label = { Text(frase) }
                    )
                }
            }

            BotonVolverAlMenu(onClick = onVolverAlMenu)

            Spacer(Modifier.height(16.dp))
        }
    }
}
