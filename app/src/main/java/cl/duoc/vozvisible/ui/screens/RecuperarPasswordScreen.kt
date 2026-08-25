package cl.duoc.vozvisible.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme

/**
 * Canales por los que el usuario puede recibir las instrucciones de recuperación.
 * La notificación visual se incluye porque es el canal accesible para el
 * público objetivo de la aplicación.
 */
private val CANALES_RECUPERACION = listOf(
    "Correo electrónico",
    "Mensaje de texto (SMS)",
    "Notificación visual en la app"
)

/** Estados posibles del resultado de la búsqueda de la cuenta. */
private sealed interface ResultadoRecuperacion {
    data object SinConsultar : ResultadoRecuperacion
    data class Encontrado(val nombre: String, val canal: String) : ResultadoRecuperacion
    data class NoEncontrado(val mensaje: String) : ResultadoRecuperacion
}

/**
 * View de recuperación de contraseña.
 *
 * Busca el correo dentro del arreglo de usuarios registrados y simula el envío
 * de instrucciones por el canal elegido. Nunca muestra la contraseña en pantalla:
 * revelarla sería una mala práctica de seguridad, aunque los datos estén en memoria.
 *
 * @param onVolver se invoca al pulsar el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecuperarPasswordScreen(onVolver: () -> Unit) {

    var correo by rememberSaveable { mutableStateOf("") }
    var canal by rememberSaveable { mutableStateOf(CANALES_RECUPERACION.first()) }
    var resultado by remember {
        mutableStateOf<ResultadoRecuperacion>(ResultadoRecuperacion.SinConsultar)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a inicio de sesión"
                        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Ingresa el correo con el que te registraste y te enviaremos " +
                    "las instrucciones para restablecer tu contraseña.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = correo,
                onValueChange = {
                    correo = it
                    // Cualquier edición invalida la consulta anterior.
                    resultado = ResultadoRecuperacion.SinConsultar
                },
                label = { Text("Correo electrónico") },
                placeholder = { Text("nombre@correo.cl") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = "Icono de correo") },
                singleLine = true,
                isError = resultado is ResultadoRecuperacion.NoEncontrado,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "¿Cómo prefieres recibir las instrucciones?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            CANALES_RECUPERACION.forEach { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = canal == opcion,
                            onClick = { canal = opcion },
                            role = Role.RadioButton
                        )
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = canal == opcion, onClick = null)
                    Text(
                        text = opcion,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Button(
                onClick = { resultado = buscarCuenta(correo, canal) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text("Enviar instrucciones", style = MaterialTheme.typography.titleMedium)
            }

            // El resultado se muestra en una tarjeta para separarlo visualmente
            // del formulario y darle jerarquía propia.
            when (val actual = resultado) {
                is ResultadoRecuperacion.SinConsultar -> Unit

                is ResultadoRecuperacion.Encontrado -> TarjetaResultado(
                    icono = Icons.Filled.CheckCircle,
                    titulo = "Instrucciones enviadas",
                    detalle = "Hola ${actual.nombre}, enviamos las instrucciones de " +
                        "recuperación por ${actual.canal.lowercase()}. Revisa tu bandeja " +
                        "en los próximos minutos.",
                    colorContenedor = MaterialTheme.colorScheme.primaryContainer,
                    colorContenido = MaterialTheme.colorScheme.onPrimaryContainer
                )

                is ResultadoRecuperacion.NoEncontrado -> TarjetaResultado(
                    icono = Icons.Filled.ErrorOutline,
                    titulo = "No pudimos continuar",
                    detalle = actual.mensaje,
                    colorContenedor = MaterialTheme.colorScheme.errorContainer,
                    colorContenido = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            TextButton(
                onClick = onVolver,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver a iniciar sesión")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Tarjeta reutilizable para mostrar el desenlace de la consulta. */
@Composable
private fun TarjetaResultado(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    detalle: String,
    colorContenedor: androidx.compose.ui.graphics.Color,
    colorContenido: androidx.compose.ui.graphics.Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = colorContenedor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                imageVector = icono,
                // null porque el texto contiguo ya comunica el mismo significado:
                // describirlo otra vez haría que TalkBack lo repita.
                contentDescription = null,
                tint = colorContenido
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorContenido
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorContenido
                )
            }
        }
    }
}

/**
 * Busca la cuenta dentro del arreglo de usuarios registrados.
 *
 * Se mantiene fuera del composable para poder cubrirla con tests unitarios.
 */
private fun buscarCuenta(correo: String, canal: String): ResultadoRecuperacion = when {
    correo.isBlank() ->
        ResultadoRecuperacion.NoEncontrado("Debes ingresar tu correo electrónico.")

    !correo.contains("@") || !correo.substringAfterLast("@").contains(".") ->
        ResultadoRecuperacion.NoEncontrado("El correo ingresado no es válido.")

    else -> {
        val usuario = RepositorioUsuarios.buscarPorCorreo(correo)
        if (usuario != null) {
            ResultadoRecuperacion.Encontrado(nombre = usuario.nombre, canal = canal)
        } else {
            ResultadoRecuperacion.NoEncontrado(
                "No encontramos ninguna cuenta registrada con ${correo.trim()}."
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RecuperarPasswordScreenPreview() {
    VozVisibleTheme {
        RecuperarPasswordScreen(onVolver = {})
    }
}
