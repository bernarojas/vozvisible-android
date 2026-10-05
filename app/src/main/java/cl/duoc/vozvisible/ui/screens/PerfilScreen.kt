package cl.duoc.vozvisible.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.ApoyoAccesibilidad
import cl.duoc.vozvisible.data.AutenticacionFirebase
import cl.duoc.vozvisible.data.ModoComunicacion
import cl.duoc.vozvisible.data.Region
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.data.ResultadoEdicion
import cl.duoc.vozvisible.data.Usuario
import cl.duoc.vozvisible.ui.componentes.ChecklistApoyos
import cl.duoc.vozvisible.ui.componentes.ComboRegion
import cl.duoc.vozvisible.ui.componentes.SelectorModo
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme
import cl.duoc.vozvisible.util.esCorreoValido
import kotlinx.coroutines.launch

/**
 * View de perfil del usuario autenticado.
 *
 * Completa las operaciones CRUD que faltaban: aquí se modifican los datos de la
 * cuenta y se elimina. El alta vive en Registro y la consulta, en la tabla de
 * esa misma view y en el resumen de Inicio.
 *
 * El formulario parte precargado con los datos actuales, de modo que el usuario
 * edita sobre lo que ya tiene en vez de volver a escribirlo todo.
 *
 * @param correoUsuario correo de la cuenta a editar, recibido como argumento.
 * @param onVolver se invoca al pulsar el botón de retroceso.
 * @param onCorreoCambiado se invoca cuando la edición cambia el correo, que es
 *   la clave de la cuenta y viaja en la ruta de navegación.
 * @param onCuentaEliminada se invoca tras borrar la cuenta, para cerrar sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    correoUsuario: String,
    onVolver: () -> Unit,
    onCorreoCambiado: (Usuario) -> Unit,
    onCuentaEliminada: () -> Unit
) {
    val original = RepositorioUsuarios.buscarPorCorreo(correoUsuario)

    // Si la cuenta no existe no hay nada que editar: puede ocurrir si se
    // elimina desde la tabla de Registro mientras el perfil está abierto.
    if (original == null) {
        PerfilNoDisponible(correoUsuario = correoUsuario, onVolver = onVolver)
        return
    }

    var nombre by remember(original) { mutableStateOf(original.nombre) }
    var correo by remember(original) { mutableStateOf(original.correo) }
    var indiceRegion by remember(original) { mutableStateOf(original.region.ordinal) }
    var indiceModo by remember(original) { mutableStateOf(original.modoPreferido.ordinal) }
    val apoyos = remember(original) { mutableStateListOf<ApoyoAccesibilidad>().apply { addAll(original.preferencias) } }

    var mensaje by remember { mutableStateOf("") }
    var esError by remember { mutableStateOf(false) }
    var confirmandoBorrado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    val alcance = rememberCoroutineScope()
    val auth = remember { AutenticacionFirebase() }

    val region = Region.entries[indiceRegion]
    val modo = ModoComunicacion.entries[indiceModo]

    fun guardar() {
        val error = validarPerfil(nombre, correo)
        if (error != null) {
            mensaje = error
            esError = true
            return
        }

        val datosNuevos = Usuario.desdeFormulario(
            nombre = nombre,
            correo = correo,
            password = original.password,
            region = region,
            modoPreferido = modo,
            preferencias = apoyos
        )

        alcance.launch {
            guardando = true
            val resultado =
                RepositorioUsuarios.actualizarEnAlmacen(original.correoNormalizado, datosNuevos)
            guardando = false

            mensaje = resultado.mensaje
            esError = !resultado.fueExitoso

            // Si el correo cambió, la ruta actual apunta a una cuenta que ya no
            // existe con ese identificador: se avisa hacia arriba para rehacerla.
            if (resultado is ResultadoEdicion.Actualizado &&
                !resultado.usuario.correspondeA(correoUsuario)
            ) {
                onCorreoCambiado(resultado.usuario)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a inicio"
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
                text = "Modifica tus datos o elimina tu cuenta. Los cambios se guardan " +
                    "sobre el mismo registro, sin crear uno nuevo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text("Datos de la cuenta", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre completo") },
                singleLine = true,
                isError = nombre.isBlank(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo electrónico") },
                singleLine = true,
                isError = correo.isNotEmpty() && !correo.esCorreoValido(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    capitalization = KeyboardCapitalization.None
                ),
                modifier = Modifier.fillMaxWidth()
            )

            ComboRegion(
                seleccion = region,
                onSeleccion = { elegida -> indiceRegion = elegida.ordinal }
            )

            Spacer(Modifier.height(4.dp))
            Text("Modo de comunicación preferido", style = MaterialTheme.typography.titleMedium)

            SelectorModo(
                seleccion = modo,
                onSeleccion = { elegido -> indiceModo = elegido.ordinal }
            )

            Spacer(Modifier.height(4.dp))
            Text("Apoyos de accesibilidad", style = MaterialTheme.typography.titleMedium)

            ChecklistApoyos(
                seleccionados = apoyos.toSet(),
                onAlternar = { apoyo, activo ->
                    if (activo) apoyos.add(apoyo) else apoyos.remove(apoyo)
                }
            )

            if (mensaje.isNotEmpty()) {
                Text(
                    text = mensaje,
                    color = if (esError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = { guardar() },
                enabled = !guardando,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Text(
                    text = if (guardando) "Guardando…" else "Guardar cambios",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            OutlinedButton(
                onClick = { confirmandoBorrado = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                Text(
                    text = "Eliminar mi cuenta",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // Borrar es irreversible, así que se pide confirmación explícita antes.
    if (confirmandoBorrado) {
        AlertDialog(
            onDismissRequest = { confirmandoBorrado = false },
            title = { Text("¿Eliminar la cuenta?") },
            text = {
                Text(
                    "Se borrarán los datos de ${original.nombre} y se cerrará la sesión. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmandoBorrado = false
                        alcance.launch {
                            // El borrado alcanza a los dos servicios: el perfil
                            // en Firestore y la credencial en Authentication.
                            // Sin lo segundo la cuenta seguiría pudiendo entrar.
                            val resultado =
                                RepositorioUsuarios.eliminarDelAlmacen(original.correoNormalizado)
                            if (resultado.fueExitoso) {
                                auth.eliminarCuenta()
                                onCuentaEliminada()
                            } else {
                                mensaje = resultado.mensaje
                                esError = true
                            }
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmandoBorrado = false }) { Text("Cancelar") }
            }
        )
    }
}

/** Estado de respaldo cuando la cuenta ya no está en el arreglo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PerfilNoDisponible(correoUsuario: String, onVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi perfil") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver a inicio"
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No encontramos la cuenta $correoUsuario en el arreglo de usuarios.",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onVolver, modifier = Modifier.heightIn(min = 56.dp)) {
                Text("Volver")
            }
        }
    }
}

/**
 * Valida los campos editables del perfil.
 *
 * Se mantiene fuera del composable para poder cubrirla con tests unitarios.
 * La contraseña no se valida aquí porque esta view no la modifica.
 *
 * @return null si los datos son válidos, o el mensaje de error correspondiente.
 */
private fun validarPerfil(nombre: String, correo: String): String? = when {
    nombre.isBlank() -> "Debes ingresar tu nombre completo."
    correo.isBlank() -> "Debes ingresar tu correo electrónico."
    !correo.esCorreoValido() -> "El correo ingresado no es válido."
    else -> null
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun PerfilScreenPreview() {
    VozVisibleTheme {
        PerfilScreen(
            correoUsuario = "camila.reyes@duocuc.cl",
            onVolver = {},
            onCorreoCambiado = {},
            onCuentaEliminada = {}
        )
    }
}
