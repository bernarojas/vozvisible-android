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
import cl.duoc.vozvisible.data.Usuario
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme
import cl.duoc.vozvisible.util.esCorreoValido

/** Campo del formulario que se resalta cuando el acceso es rechazado. */
private enum class CampoAcceso { CORREO, PASSWORD, AMBOS }

/**
 * Desenlace del intento de acceso.
 *
 * La interfaz declara las tres propiedades que consume la view y cada subtipo
 * decide su valor. Así el composable no pregunta de qué caso se trata: lee
 * `mensaje`, `errorCorreo` y `errorPassword` sin ramificar.
 */
private sealed interface ResultadoAcceso {

    val mensaje: String
    val errorCorreo: Boolean
    val errorPassword: Boolean

    /** Todavía no se ha pulsado Ingresar, o el usuario está editando el formulario. */
    data object Pendiente : ResultadoAcceso {
        override val mensaje: String = ""
        override val errorCorreo: Boolean = false
        override val errorPassword: Boolean = false
    }

    /** Las credenciales coinciden con un usuario del arreglo. */
    data class Autenticado(val usuario: Usuario) : ResultadoAcceso {
        override val mensaje: String = ""
        override val errorCorreo: Boolean = false
        override val errorPassword: Boolean = false
    }

    /**
     * El acceso fue denegado.
     *
     * Se guarda qué campo lo provocó para resaltar únicamente ese campo, en
     * lugar de marcar todo el formulario en rojo.
     */
    data class Rechazado(
        override val mensaje: String,
        val campo: CampoAcceso
    ) : ResultadoAcceso {
        override val errorCorreo: Boolean get() = campo != CampoAcceso.PASSWORD
        override val errorPassword: Boolean get() = campo != CampoAcceso.CORREO
    }
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
    var resultado by remember { mutableStateOf<ResultadoAcceso>(ResultadoAcceso.Pendiente) }

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
                        resultado = ResultadoAcceso.Pendiente
                    },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("nombre@correo.cl") },
                    leadingIcon = {
                        Icon(Icons.Filled.Email, contentDescription = "Icono de correo")
                    },
                    singleLine = true,
                    isError = resultado.errorCorreo,
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
                        resultado = ResultadoAcceso.Pendiente
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
                    isError = resultado.errorPassword,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                // El espacio se reserva siempre para que el formulario no salte
                // verticalmente cuando aparece o desaparece el mensaje de error.
                Text(
                    text = resultado.mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(top = 8.dp)
                )

                Button(
                    onClick = {
                        val intento = validarAcceso(correo, password)
                        resultado = intento
                        // El smart cast permite leer `intento.usuario` sin volver
                        // a consultar el arreglo ni convertir el tipo a mano.
                        if (intento is ResultadoAcceso.Autenticado) {
                            onLoginExitoso(intento.usuario.correoNormalizado)
                        }
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

        // El arreglo llega con usuarios ya cargados: se indica en pantalla para
        // que la aplicación pueda probarse sin registrar una cuenta primero.
        Text(
            text = "${RepositorioUsuarios.total} cuentas disponibles en el arreglo de usuarios",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Valida las credenciales contra el arreglo de usuarios registrados.
 *
 * Se separa de la función composable para poder probarla con tests unitarios
 * sin necesidad de levantar la interfaz.
 */
private fun validarAcceso(correo: String, password: String): ResultadoAcceso = when {
    correo.isBlank() ->
        ResultadoAcceso.Rechazado("Debes ingresar tu correo electrónico.", CampoAcceso.CORREO)

    !correo.esCorreoValido() ->
        ResultadoAcceso.Rechazado("El correo ingresado no es válido.", CampoAcceso.CORREO)

    password.isBlank() ->
        ResultadoAcceso.Rechazado("Debes ingresar tu contraseña.", CampoAcceso.PASSWORD)

    else -> {
        // autenticar devuelve el propio usuario, de modo que una sola pasada por
        // el arreglo resuelve la validación y entrega el dato que necesita Inicio.
        val usuario = RepositorioUsuarios.autenticar(correo, password)

        usuario?.let { ResultadoAcceso.Autenticado(it) }
            ?: ResultadoAcceso.Rechazado("Correo o contraseña incorrectos.", CampoAcceso.AMBOS)
    }
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
