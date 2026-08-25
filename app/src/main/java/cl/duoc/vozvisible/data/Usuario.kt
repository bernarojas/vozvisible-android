package cl.duoc.vozvisible.data

/**
 * Representa a un usuario registrado en la aplicacion.
 *
 * @param nombre nombre completo ingresado en la view de Registro.
 * @param correo correo electronico, actua como identificador unico de acceso.
 * @param password contrasena de acceso asociada al correo.
 * @param region region de residencia, seleccionada desde el combo box.
 * @param modoPreferido modo de comunicacion preferido, elegido con radio buttons.
 * @param preferencias apoyos de accesibilidad marcados en la check list.
 */
data class Usuario(
    val nombre: String,
    val correo: String,
    val password: String,
    val region: String,
    val modoPreferido: String,
    val preferencias: List<String>
)
