package cl.duoc.vozvisible.ui.componentes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cl.duoc.vozvisible.data.ApoyoAccesibilidad
import cl.duoc.vozvisible.data.ModoComunicacion
import cl.duoc.vozvisible.data.Region

/**
 * Selectores del perfil de usuario, compartidos por Registro y Perfil.
 *
 * Se extraen a un archivo propio porque ambas views ofrecen exactamente las
 * mismas opciones: duplicarlas obligaría a corregir dos veces cualquier cambio
 * de comportamiento o de accesibilidad.
 *
 * Los tres siguen el mismo patrón de state hoisting: no guardan la selección,
 * la reciben y avisan hacia arriba cuando el usuario elige algo.
 */

/** Altura mínima de cada fila seleccionable: objetivo táctil cómodo. */
private val ALTO_FILA = 56.dp

/**
 * Combo box con las regiones del enum [Region].
 *
 * @param seleccion región elegida, o null si todavía no hay ninguna.
 * @param onSeleccion se invoca con la región que el usuario escoge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComboRegion(
    seleccion: Region?,
    onSeleccion: (Region) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { abierto = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            // El campo muestra la etiqueta del enum, no su nombre interno.
            value = seleccion?.nombre.orEmpty(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Región de residencia") },
            supportingText = {
                seleccion?.let { region -> Text("Zona ${region.zona}") }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = abierto,
            onDismissRequest = { abierto = false }
        ) {
            Region.entries.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion.nombre) },
                    onClick = {
                        onSeleccion(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

/**
 * Grupo de radio buttons con los modos de comunicación.
 *
 * El modificador selectable se aplica a la fila completa y no al control: el
 * área táctil abarca toda la línea y no solo el círculo, criterio relevante en
 * una aplicación de accesibilidad. El parámetro role informa a los lectores de
 * pantalla qué tipo de control es, y por eso el RadioButton recibe onClick nulo.
 */
@Composable
fun SelectorModo(
    seleccion: ModoComunicacion?,
    onSeleccion: (ModoComunicacion) -> Unit
) {
    ModoComunicacion.entries.forEach { opcion ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = seleccion == opcion,
                    onClick = { onSeleccion(opcion) },
                    role = Role.RadioButton
                )
                .heightIn(min = ALTO_FILA),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = seleccion == opcion, onClick = null)
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
}

/**
 * Check list de apoyos de accesibilidad.
 *
 * A diferencia de los radio buttons admite varias opciones a la vez, por lo que
 * la selección es un conjunto. El modificador toggleable cumple aquí el mismo
 * papel que selectable en el caso anterior.
 *
 * @param seleccionados apoyos marcados actualmente.
 * @param onAlternar se invoca con el apoyo y su estado nuevo.
 */
@Composable
fun ChecklistApoyos(
    seleccionados: Set<ApoyoAccesibilidad>,
    onAlternar: (ApoyoAccesibilidad, Boolean) -> Unit
) {
    ApoyoAccesibilidad.entries.forEach { apoyo ->
        val marcado = apoyo in seleccionados
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = marcado,
                    onValueChange = { activo -> onAlternar(apoyo, activo) },
                    role = Role.Checkbox
                )
                .heightIn(min = ALTO_FILA),
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
}
