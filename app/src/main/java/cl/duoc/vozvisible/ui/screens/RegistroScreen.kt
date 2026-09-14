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
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import cl.duoc.vozvisible.data.ApoyoAccesibilidad
import cl.duoc.vozvisible.data.ModoComunicacion
import cl.duoc.vozvisible.data.Region
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.data.ResultadoRegistro
import cl.duoc.vozvisible.data.Usuario
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme
import cl.duoc.vozvisible.util.LARGO_MINIMO_PASSWORD
import cl.duoc.vozvisible.util.esCorreoValido
import cl.duoc.vozvisible.util.esPasswordValida
import cl.duoc.vozvisible.util.fortalezaPassword

/**
 * Datos crudos del formulario, agrupados en un solo objeto.
 *
 * Reunirlos evita que la función de validación reciba siete parámetros sueltos
 * y permite construirla como una función pura que se prueba sin la interfaz.
 */
private data class FormularioRegistro(
    val nombre: String,
    val correo: String,
    val password: String,
    val confirmacion: String,
    val region: Region?,
    val modoPreferido: ModoComunicacion?,
    val aceptaTerminos: Boolean
)

/**
 * Valor que representa "todavía no se ha elegido nada" en los selectores.
 *
 * Las selecciones del combo box y de los radio buttons se guardan como el
 * `ordinal` de la constante elegida: un entero viaja sin problemas en el Bundle
 * del sistema, por lo que la selección sobrevive a la rotación de la pantalla.
 */
private const val SIN_SELECCION = -1

/** Traduce el índice guardado de vuelta a la constante del enum, o a null. */
private fun <T : Enum<T>> List<T>.seleccion(indice: Int): T? = getOrNull(indice)

/**
 * View de registro de usuarios.
 *
 * Concentra los componentes UI exigidos por la actividad: campos de entrada,
 * combo box, radio buttons, check list, botones, vínculos y una tabla con
 * el arreglo de usuarios ya registrados.
 *
 * Las opciones de los tres selectores no se escriben aquí: se recorren desde
 * `entries` de cada enum, de modo que agregar una región o un apoyo nuevo no
 * obliga a tocar la interfaz.
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
    var indiceRegion by rememberSaveable { mutableIntStateOf(SIN_SELECCION) }
    var indiceModo by rememberSaveable { mutableIntStateOf(SIN_SELECCION) }
    var aceptaTerminos by rememberSaveable { mutableStateOf(false) }
    var menuRegionAbierto by remember { mutableStateOf(false) }

    // Selecciones vigentes, derivadas del índice guardado. Al ser valores
    // calculados no hay dos fuentes de verdad que puedan desincronizarse.
    val region: Region? = Region.entries.seleccion(indiceRegion)
    val modoPreferido: ModoComunicacion? = ModoComunicacion.entries.seleccion(indiceModo)

    // Apoyos marcados en la check list. Es estado de la view, no del repositorio.
    val apoyosSeleccionados = remember { mutableStateListOf<ApoyoAccesibilidad>() }

    var resultado by remember { mutableStateOf<ResultadoRegistro?>(null) }
    var errorValidacion by remember { mutableStateOf("") }

    fun limpiarFormulario() {
        nombre = ""
        correo = ""
        password = ""
        confirmacion = ""
        indiceRegion = SIN_SELECCION
        indiceModo = SIN_SELECCION
        aceptaTerminos = false
        apoyosSeleccionados.clear()
    }

    fun enviarFormulario() {
        val formulario = FormularioRegistro(
            nombre = nombre,
            correo = correo,
            password = password,
            confirmacion = confirmacion,
            region = region,
            modoPreferido = modoPreferido,
            aceptaTerminos = aceptaTerminos
        )

        val error = validarRegistro(formulario)
        if (error != null) {
            errorValidacion = error
            resultado = null
            return
        }

        errorValidacion = ""
        // Los campos ya fueron validados, por lo que aquí las selecciones no
        // pueden ser nulas: el operador !! documenta esa garantía.
        resultado = RepositorioUsuarios.registrar(
            Usuario.desdeFormulario(
                nombre = nombre,
                correo = correo,
                password = password,
                region = region!!,
                modoPreferido = modoPreferido!!,
                preferencias = apoyosSeleccionados
            )
        )

        if (resultado?.fueExitoso == true) limpiarFormulario()
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
                    text = "Usuarios registrados: ${RepositorioUsuarios.total} de " +
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
                    isError = correo.isNotEmpty() && !correo.esCorreoValido(),
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
                    // El texto de apoyo cambia según lo escrito: mientras el campo
                    // está vacío indica la regla; después, la robustez alcanzada.
                    supportingText = {
                        Text(
                            text = password.fortalezaPassword()
                                .ifEmpty { "Mínimo $LARGO_MINIMO_PASSWORD caracteres" }
                        )
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = password.isNotEmpty() && !password.esPasswordValida(),
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
                        // El campo muestra la etiqueta del enum, no su nombre interno.
                        value = region?.nombre.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Región de residencia") },
                        supportingText = {
                            region?.let { seleccionada -> Text("Zona ${seleccionada.zona}") }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuRegionAbierto)
                        },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = menuRegionAbierto,
                        onDismissRequest = { menuRegionAbierto = false }
                    ) {
                        Region.entries.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion.nombre) },
                                onClick = {
                                    indiceRegion = opcion.ordinal
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

            items(ModoComunicacion.entries) { opcion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // selectable en la fila completa: el área táctil es toda la
                        // línea, no solo el círculo. Role.RadioButton lo anuncia a TalkBack.
                        .selectable(
                            selected = modoPreferido == opcion,
                            onClick = { indiceModo = opcion.ordinal },
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
                        Text(opcion.titulo, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            opcion.descripcion,
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

            items(ApoyoAccesibilidad.entries) { apoyo ->
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
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(apoyo.titulo, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            apoyo.beneficio,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                // Un solo bloque de mensajes: el error de validación previa o el
                // desenlace que devolvió el repositorio, nunca ambos a la vez.
                val mensaje = errorValidacion.ifEmpty { resultado?.mensaje.orEmpty() }
                val esExito = errorValidacion.isEmpty() && resultado?.fueExitoso == true

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
                    onClick = { enviarFormulario() },
                    enabled = RepositorioUsuarios.hayCupo(),
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

                // La tabla se muestra ordenada por nombre. ordenadosPor recibe el
                // criterio como lambda, de modo que cambiar el orden es cambiar
                // esta línea y nada más.
                items(RepositorioUsuarios.ordenadosPor { usuario -> usuario.nombre }) { usuario ->
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
            Text(
                usuario.nombre,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            Text(
                // Se muestra el correo enmascarado: la tabla queda visible en
                // pantalla y no tiene por qué exponer la dirección completa.
                usuario.correoVisible,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1.4f)
            )
            Text(
                usuario.region.nombre,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = "${usuario.modoPreferido.titulo} · ${usuario.apoyosComoTexto()}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
    }
}

/**
 * Valida el formulario de registro.
 *
 * Las reglas se declaran como una lista de pares condición-mensaje y se recorre
 * hasta encontrar la primera que no se cumple. Agregar una regla es agregar una
 * línea, sin tocar la lógica que las evalúa.
 *
 * Se mantiene fuera del composable para poder cubrirla con tests unitarios.
 *
 * @return null si los datos son válidos, o el mensaje de error correspondiente.
 */
private fun validarRegistro(formulario: FormularioRegistro): String? = with(formulario) {
    listOf<Pair<() -> Boolean, String>>(
        { nombre.isNotBlank() } to "Debes ingresar tu nombre completo.",
        { correo.isNotBlank() } to "Debes ingresar tu correo electrónico.",
        { correo.esCorreoValido() } to "El correo ingresado no es válido.",
        { password.esPasswordValida() } to
            "La contraseña debe tener al menos $LARGO_MINIMO_PASSWORD caracteres.",
        { password == confirmacion } to "Las contraseñas no coinciden.",
        { region != null } to "Debes seleccionar tu región.",
        { modoPreferido != null } to "Debes elegir un modo de comunicación preferido.",
        { aceptaTerminos } to "Debes aceptar los términos y condiciones."
    ).firstOrNull { (condicion, _) -> !condicion() }?.second
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RegistroScreenPreview() {
    VozVisibleTheme {
        RegistroScreen(onVolver = {})
    }
}
