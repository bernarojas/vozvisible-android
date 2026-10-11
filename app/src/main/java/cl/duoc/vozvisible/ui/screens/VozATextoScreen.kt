package cl.duoc.vozvisible.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import cl.duoc.vozvisible.data.Interaccion
import cl.duoc.vozvisible.data.RepositorioInteracciones
import cl.duoc.vozvisible.data.TipoInteraccion
import cl.duoc.vozvisible.ui.componentes.AvisoResultado
import cl.duoc.vozvisible.ui.componentes.BotonVolverAlMenu
import cl.duoc.vozvisible.ui.componentes.TarjetaGuia
import cl.duoc.vozvisible.voz.EstadoMotor
import cl.duoc.vozvisible.voz.MotorVozATexto
import kotlinx.coroutines.launch

/** Pasos que la view muestra como guía al usuario. */
private val PASOS_VOZ_A_TEXTO = listOf(
    "Pulsa el micrófono y acerca el teléfono a la persona que te habla.",
    "Mientras habla, la barra se mueve y el texto aparece en pantalla.",
    "Cuando termine, pulsa «Listo» para guardar la transcripción.",
    "Si quieres contestar, usa «Responder escribiendo»."
)

/**
 * View de Voz a texto: la aplicación escucha al interlocutor y lo transcribe.
 *
 * Cada transcripción terminada se guarda en el historial y se agrega a la
 * conversación de la visita, que se muestra de la más reciente a la más antigua.
 *
 * El reconocimiento necesita el permiso de micrófono, que Android exige pedir
 * en tiempo de ejecución. Si el usuario lo niega, la view lo explica en lugar
 * de fallar en silencio.
 *
 * @param correoUsuario dueño de las interacciones que se guarden.
 * @param onVolver se invoca al pulsar el botón de retroceso.
 * @param onResponder abre Texto a voz para contestar por escrito.
 * @param onVolverAlMenu se invoca al terminar, para regresar al menú principal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VozATextoScreen(
    correoUsuario: String,
    onVolver: () -> Unit,
    onResponder: () -> Unit,
    onVolverAlMenu: () -> Unit
) {
    val contexto = LocalContext.current
    val alcance = rememberCoroutineScope()

    val conversacion = remember { mutableStateListOf<String>() }
    var mensaje by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }

    // El motor se crea una sola vez, pero la función que guarda puede cambiar
    // entre recomposiciones: rememberUpdatedState hace que el motor invoque
    // siempre la versión vigente y no la que existía al crearlo.
    val alTranscribir by rememberUpdatedState { transcripcion: String ->
        conversacion.add(0, transcripcion)
        alcance.launch {
            val resultado = RepositorioInteracciones.registrar(
                Interaccion.nueva(
                    correoUsuario = correoUsuario,
                    tipo = TipoInteraccion.VOZ_A_TEXTO,
                    solicitud = TipoInteraccion.VOZ_A_TEXTO.solicitudTipica,
                    respuesta = transcripcion
                )
            )
            mensaje = resultado.mensaje
            esError = !resultado.fueExitoso
        }
    }

    val motor = remember { MotorVozATexto(contexto) { texto -> alTranscribir(texto) } }
    DisposableEffect(motor) {
        onDispose { motor.liberar() }
    }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            motor.escuchar()
        } else {
            mensaje = "Sin permiso de micrófono no es posible transcribir. Puedes " +
                "concederlo en los ajustes de Android, sección Aplicaciones."
            esError = true
        }
    }

    fun escuchar() {
        mensaje = ""
        val tienePermiso = ContextCompat.checkSelfPermission(
            contexto, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (tienePermiso) motor.escuchar() else pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
    }

    val estado = motor.estado
    val escuchando = estado as? EstadoMotor.Escuchando

    // La barra sigue la intensidad de la voz y el micrófono late mientras
    // escucha: son la confirmación visual de que el teléfono está captando.
    val nivel by animateFloatAsState(
        targetValue = escuchando?.nivel ?: 0f,
        label = "intensidad de la voz"
    )
    val latido = rememberInfiniteTransition(label = "latido del micrófono")
    val escala by latido.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 600), RepeatMode.Reverse),
        label = "escala del micrófono"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voz a texto") },
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

            TarjetaGuia(titulo = "Cómo funciona", pasos = PASOS_VOZ_A_TEXTO)

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                FilledIconButton(
                    onClick = { if (escuchando != null) motor.terminar() else escuchar() },
                    enabled = escuchando != null || motor.estaListo,
                    colors = if (escuchando != null) IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ) else IconButtonDefaults.filledIconButtonColors(),
                    modifier = Modifier
                        .size(96.dp)
                        .scale(if (escuchando != null) escala else 1f),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (estado is EstadoMotor.NoDisponible) Icons.Filled.MicOff
                        else Icons.Filled.Mic,
                        contentDescription = if (escuchando != null) "Terminar de escuchar"
                        else "Comenzar a escuchar",
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                text = motor.describirEstado(),
                style = MaterialTheme.typography.labelLarge,
                color = if (estado is EstadoMotor.NoDisponible || estado is EstadoMotor.Fallo)
                    MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Subtítulos en vivo: lo que el reconocedor va entendiendo aparece
            // en letra grande antes de que la persona termine de hablar.
            AnimatedVisibility(visible = escuchando != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        LinearProgressIndicator(
                            progress = { nivel },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = escuchando?.parcial?.ifBlank { null } ?: "Habla ahora…",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { motor.terminar() }) { Text("Listo") }
                            TextButton(onClick = { motor.detener() }) { Text("Cancelar") }
                        }
                    }
                }
            }

            AvisoResultado(mensaje = mensaje, esError = esError)

            if (conversacion.isNotEmpty()) {
                Text(
                    text = "Lo que se dijo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                conversacion.forEach { frase ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = frase,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = onResponder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null)
                    Text("Responder escribiendo", modifier = Modifier.padding(start = 8.dp))
                }
            }

            BotonVolverAlMenu(onClick = onVolverAlMenu)

            Spacer(Modifier.height(16.dp))
        }
    }
}
