package cl.duoc.vozvisible.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.data.Usuario
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme

/** Opciones del combo box de región. */
private val REGIONES = listOf(
    "Arica y Parinacota",
    "Antofagasta",
    "Coquimbo",
    "Valparaíso",
    "Metropolitana",
    "Maule",
    "Biobío",
    "La Araucanía",
    "Los Lagos",
    "Magallanes"
)

/** Opciones excluyentes del grupo de radio buttons. */
private val MODOS_COMUNICACION = listOf(
    "Voz a texto" to "Transcribe lo que otros dicen",
    "Texto a voz" to "Reproduce en voz alta lo que escribes",
    "Ambos modos" to "Comunicación bidireccional completa"
)

/** Opciones múltiples de la check list de apoyos. */
private val APOYOS_ACCESIBILIDAD = listOf(
    "Alertas vibratorias",
    "Subtítulos automáticos",
    "Alto contraste",
    "Texto ampliado"
)

/**
 * View de registro de usuarios.
 *
 * Concentra los componentes UI exigidos por la actividad: campos de entrada,
 * combo box, radio buttons, check list, botones, vínculos y una tabla con
 * el arreglo de usuarios ya registrados.
 *
 * @param onVolver se invoca al pulsar el botón de retroceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(onVolver: () -> Unit) {

    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmacion by rememberSaveable { mutableStateOf("") }
    var region by rememberSaveable { mutableStateOf("") }
    var modoPreferido by rememberSaveable { mutableStateOf("") }
    var aceptaTerminos by rememberSaveable { mutableStateOf(false) }
    var menuRegionAbierto by remember { mutableStateOf(false) }

    // Lista de apoyos marcados en la check list. Es estado de la view, no del repositorio.
    val apoyosSeleccionados = remember { mutableStateListOf<String>() }

    var mensaje by remember { mutableStateOf("") }
    var esExito by remember { mutableStateOf(false) }

    fun limpiarFormulario() {
        nombre = ""
        correo = ""
        password = ""
        confirmacion = ""
        region = ""
        modoPreferido = ""
        aceptaTerminos = false
        apoyosSeleccionados.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de usuario") },
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

        // LazyColumn como contenedor único de scroll: evita anidar dos scrolls
        // verticales, que en Compose provoca una excepción de altura infinita.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Text(
                    text = "Usuarios registrados: ${RepositorioUsuarios.lista.size} de " +
                        "${RepositorioUsuarios.MAX_USUARIOS}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // ----------------------------- Datos de la cuenta -----------------------------
            item {
                Text("Datos de la cuenta", style = MaterialTheme.typography.titleMedium)
            }

            item {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = { Text("Correo electrónico") },
                    placeholder = { Text("nombre@correo.cl") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    supportingText = { Text("Mínimo 6 caracteres") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = confirmacion,
                    onValueChange = { confirmacion = it },
                    label = { Text("Repetir contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = confirmacion.isNotEmpty() && confirmacion != password,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // ---------------------------- Combo box de región -----------------------------
            item {
                ExposedDropdownMenuBox(
                    expanded = menuRegionAbierto,
                    onExpandedChange = { menuRegionAbierto = it }
                ) {
                    OutlinedTextField(
                        value = region,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Región de residencia") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuRegionAbierto)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuRegionAbierto,
                        onDismissRequest = { menuRegionAbierto = false }
                    ) {
                        REGIONES.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = {
                                    region = opcion
                                    menuRegionAbierto = false
                                }
                            )
                        }
                    }
                }
            }

            // -------------------------- Radio buttons: modo preferido ---------------------
            item {
                Spacer(Modifier.height(4.dp))
                Text("Modo de comunicación preferido", style = MaterialTheme.typography.titleMedium)
            }

            items(MODOS_COMUNICACION) { (opcion, descripcion) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // selectable en la fila completa: el área táctil es toda la
                        // línea, no solo el círculo. Role.RadioButton lo anuncia a TalkBack.
                        .selectable(
                            selected = modoPreferido == opcion,
                            onClick = { modoPreferido = opcion },
                            role = Role.RadioButton
                        )
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = modoPreferido == opcion,
                        onClick = null // el click lo gestiona el modifier selectable de la fila
                    )
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(opcion, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ------------------------- Check list: apoyos de accesibilidad ----------------
            item {
                Spacer(Modifier.height(4.dp))
                Text("Apoyos de accesibilidad", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Puedes seleccionar más de uno",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(APOYOS_ACCESIBILIDAD) { apoyo ->
                val marcado = apoyo in apoyosSeleccionados
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = marcado,
                            onValueChange = { activo ->
                                if (activo) apoyosSeleccionados.add(apoyo)
                                else apoyosSeleccionados.remove(apoyo)
                            },
                            role = Role.Checkbox
                        )
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = marcado, onCheckedChange = null)
                    Text(
                        text = apoyo,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // -------------------------------- Términos y envío ----------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = aceptaTerminos,
                            onValueChange = { aceptaTerminos = it },
                            role = Role.Checkbox
                        )
                        .heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = aceptaTerminos, onCheckedChange = null)
                    Text(
                        text = "Acepto los términos y condiciones de uso",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            item {
                if (mensaje.isNotEmpty()) {
                    Text(
                        text = mensaje,
                        color = if (esExito) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(
                    onClick = {
                        val error = validarRegistro(
                            nombre = nombre,
                            correo = correo,
                            password = password,
                            confirmacion = confirmacion,
                            region = region,
                            modoPreferido = modoPreferido,
                            aceptaTerminos = aceptaTerminos
                        ) ?: RepositorioUsuarios.registrar(
                            Usuario(
                                nombre = nombre.trim(),
                                correo = correo.trim(),
                                password = password,
                                region = region,
                                modoPreferido = modoPreferido,
                                preferencias = apoyosSeleccionados.toList()
                            )
                        )

                        if (error == null) {
                            esExito = true
                            mensaje = "Usuario registrado correctamente. Ya puedes iniciar sesión."
                            limpiarFormulario()
                        } else {
                            esExito = false
                            mensaje = error
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                ) {
                    Text("Registrar usuario", style = MaterialTheme.typography.titleMedium)
                }
            }

            // ----------------------- Tabla de usuarios registrados ------------------------
            item {
                Spacer(Modifier.height(8.dp))
                Text("Usuarios registrados", style = MaterialTheme.typography.titleMedium)
            }

            if (RepositorioUsuarios.lista.isEmpty()) {
                item {
                    Text(
                        text = "Todavía no hay usuarios registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                item { FilaEncabezadoTabla() }

                items(RepositorioUsuarios.lista) { usuario ->
                    FilaUsuarioTabla(usuario)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Encabezado de la tabla de usuarios. */
@Composable
private fun FilaEncabezadoTabla() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            "Nombre",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Correo",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1.4f)
        )
        Text(
            "Región",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Fila de datos de la tabla de usuarios. */
@Composable
private fun FilaUsuarioTabla(usuario: Usuario) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(usuario.nombre, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text(usuario.correo, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1.4f))
            Text(usuario.region, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        }
        HorizontalDivider()
    }
}

/**
 * Valida el formulario de registro.
 *
 * Se mantiene fuera del composable para poder cubrirla con tests unitarios.
 *
 * @return null si los datos son válidos, o el mensaje de error correspondiente.
 */
private fun validarRegistro(
    nombre: String,
    correo: String,
    password: String,
    confirmacion: String,
    region: String,
    modoPreferido: String,
    aceptaTerminos: Boolean
): String? = when {
    nombre.isBlank() -> "Debes ingresar tu nombre completo."
    correo.isBlank() -> "Debes ingresar tu correo electrónico."
    !correo.contains("@") || !correo.substringAfterLast("@").contains(".") ->
        "El correo ingresado no es válido."
    password.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
    password != confirmacion -> "Las contraseñas no coinciden."
    region.isBlank() -> "Debes seleccionar tu región."
    modoPreferido.isBlank() -> "Debes elegir un modo de comunicación preferido."
    !aceptaTerminos -> "Debes aceptar los términos y condiciones."
    else -> null
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistroScreenPreview() {
    VozVisibleTheme {
        RegistroScreen(onVolver = {})
    }
}
