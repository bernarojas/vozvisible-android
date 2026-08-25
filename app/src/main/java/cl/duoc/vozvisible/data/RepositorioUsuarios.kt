package cl.duoc.vozvisible.data

import androidx.compose.runtime.mutableStateListOf

/**
 * Almacena en memoria el arreglo de usuarios registrados desde la view de Registro.
 *
 * Se usa mutableStateListOf en lugar de una lista comun para que Compose
 * recomponga automaticamente las views que muestran estos datos.
 */
object RepositorioUsuarios {

    /** Cupo maximo de usuarios exigido por el enunciado de la actividad. */
    const val MAX_USUARIOS = 5

    private val usuarios = mutableStateListOf<Usuario>()

    /** Vista de solo lectura del arreglo, para que las views no lo modifiquen directamente. */
    val lista: List<Usuario> get() = usuarios

    fun hayCupo(): Boolean = usuarios.size < MAX_USUARIOS

    fun correoRegistrado(correo: String): Boolean =
        usuarios.any { it.correo.equals(correo.trim(), ignoreCase = true) }

    /**
     * Intenta agregar un usuario al arreglo.
     * @return null si se registro correctamente, o el mensaje de error si no.
     */
    fun registrar(usuario: Usuario): String? = when {
        !hayCupo() -> "Se alcanzo el maximo de $MAX_USUARIOS usuarios registrados."
        correoRegistrado(usuario.correo) -> "El correo ${usuario.correo} ya esta registrado."
        else -> {
            usuarios.add(usuario)
            null
        }
    }

    /** Valida las credenciales ingresadas en la view de Login. */
    fun credencialesValidas(correo: String, password: String): Boolean =
        usuarios.any {
            it.correo.equals(correo.trim(), ignoreCase = true) && it.password == password
        }

    /** Busca un usuario por correo, usado por la view de Recuperar contrasena. */
    fun buscarPorCorreo(correo: String): Usuario? =
        usuarios.firstOrNull { it.correo.equals(correo.trim(), ignoreCase = true) }
}
