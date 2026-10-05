package cl.duoc.vozvisible.data

/**
 * Desenlace de una modificación o una eliminación sobre el arreglo de usuarios.
 *
 * Acompaña a [ResultadoRegistro], que cubre el alta. Entre ambas quedan
 * representadas las cuatro operaciones CRUD: crear, consultar, modificar y
 * eliminar. Se modela igual que aquella, con una interfaz sellada, para que el
 * `when` de la view sea exhaustivo y cada desenlace transporte sus datos.
 */
sealed interface ResultadoEdicion {

    /** El usuario quedó modificado con los datos nuevos. */
    data class Actualizado(val usuario: Usuario) : ResultadoEdicion

    /** La cuenta se eliminó del arreglo. */
    data class Eliminado(val correo: String) : ResultadoEdicion

    /** No existe ninguna cuenta con el correo indicado. */
    data class NoEncontrado(val correo: String) : ResultadoEdicion

    /** El correo nuevo ya pertenece a otra cuenta distinta de la editada. */
    data class CorreoEnUso(val correo: String) : ResultadoEdicion

    /** Verdadero cuando la operación se completó. */
    val fueExitoso: Boolean get() = this is Actualizado || this is Eliminado

    /** Mensaje listo para mostrar, derivado del propio desenlace. */
    val mensaje: String
        get() = when (this) {
            is Actualizado -> "Los datos de ${usuario.primerNombre} se actualizaron correctamente."
            is Eliminado -> "La cuenta $correo se eliminó del arreglo."
            is NoEncontrado -> "No existe ninguna cuenta registrada con $correo."
            is CorreoEnUso -> "El correo $correo ya pertenece a otra cuenta."
        }
}
