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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme

/**
 * Resultado de validar el formulario de acceso.
 *
 * Se distingue qué campo provocó el error para resaltar únicamente ese campo,
 * en lugar de marcar todo el formulario en rojo.
 */
private data class ValidacionAcceso(
    val mensaje: String = "",
    val errorCorreo: Boolean = false,
    val errorPassword: Boolean = false
) {
    val esValido: Boolean get() = mensaje.isEmpty()
}

/**
 * View de acceso a la aplicación.
 *
 * No recibe el NavController: notifica los eventos hacia arriba mediante lambdas,
 * de modo que la view no depende de la navegación y puede previsualizarse aislada.
 *
 * @param onLoginExitoso se invoca con el correo autenticado cuando las credenciales son válidas.
 * @param onIrARegistro se invoca al pulsar el vínculo "Crear cuenta".
 * @param onIrARecuperar se invoca al pulsar el vínculo de recuperación.
 */
@Composable
fun LoginScreen(
    onLoginExitoso: (String) -> Unit,
    onIrARegistro: () -> Unit,
    onIrARecuperar: () -> Unit
) {
    // rememberSaveable conserva el estado al rotar la pantalla; remember no lo haría.
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }
    // Aquí sí se usa remember y no rememberSaveable: el resultado de la validación
    // es feedback momentáneo, no un dato que deba sobrevivir a la rotación.
    var validacion by remember { mutableStateOf(ValidacionAcceso()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "VozVisible",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Comunicación accesible para personas con discapacidad auditiva",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {

                Text(
                    text = "Iniciar sesión",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = correo,
                    onValueChange = {
                        correo = it
                        validacion = ValidacionAcceso()
                    },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("nombre@correo.cl") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = "Icono de correo")
                    },
                    singleLine = true,
                    isError = validacion.errorCorreo,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        validacion = ValidacionAcceso()
                    },
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        Icon(Icons.Filled.Lock, contentDescription = "Icono de contraseña")
                    },
                    trailingIcon = {
                        IconButton(onClick = { verPassword = !verPassword }) {
                            Icon(
                                imageVector = if (verPassword) Icons.Filled.VisibilityOff
                                else Icons.Filled.Visibility,
                                contentDescription = if (verPassword) "Ocultar contraseña"
                                else "Mostrar contraseña"
                            )
                        }
                    },
                    visualTransformation = if (verPassword) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    singleLine = true,
                    isError = validacion.errorPassword,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                // El espacio se reserva siempre para que el formulario no salte
                // verticalmente cuando aparece o desaparece el mensaje de error.
                Text(
                    text = validacion.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(top = 8.dp)
                )

                Button(
                    onClick = {
                        validacion = validarAcceso(correo, password)
                        if (validacion.esValido) onLoginExitoso(correo.trim())
                    },
                    // Altura mínima de 56dp: objetivo táctil cómodo, criterio de accesibilidad.
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Text("Ingresar", style = MaterialTheme.typography.titleMedium)
                }

                Spacer(Modifier.height(4.dp))

                TextButton(
                    onClick = onIrARecuperar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Olvidé mi contraseña")
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "¿No tienes cuenta?",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onIrARegistro) {
                Text("Crear cuenta", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Valida las credenciales contra el arreglo de usuarios registrados.
 *
 * Se separa de la función composable para poder probarla con tests unitarios
 * sin necesidad de levantar la interfaz.
 */
private fun validarAcceso(correo: String, password: String): ValidacionAcceso = when {
    correo.isBlank() ->
        ValidacionAcceso("Debes ingresar tu correo electrónico.", errorCorreo = true)

    !correo.contains("@") || !correo.substringAfterLast("@").contains(".") ->
        ValidacionAcceso("El correo ingresado no es válido.", errorCorreo = true)

    password.isBlank() ->
        ValidacionAcceso("Debes ingresar tu contraseña.", errorPassword = true)

    RepositorioUsuarios.lista.isEmpty() ->
        ValidacionAcceso(
            "Aún no hay usuarios registrados. Crea una cuenta primero.",
            errorCorreo = true
        )

    !RepositorioUsuarios.credencialesValidas(correo, password) ->
        ValidacionAcceso(
            "Correo o contraseña incorrectos.",
            errorCorreo = true,
            errorPassword = true
        )

    else -> ValidacionAcceso()
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun LoginScreenPreview() {
    VozVisibleTheme {
        LoginScreen(
            onLoginExitoso = {},
            onIrARegistro = {},
            onIrARecuperar = {}
        )
    }
}
