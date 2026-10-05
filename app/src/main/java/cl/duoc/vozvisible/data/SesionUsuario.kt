package cl.duoc.vozvisible.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Guarda los datos básicos de la sesión en SharedPreferences.
 *
 * SharedPreferences es el almacenamiento clave-valor de Android, pensado para
 * datos pequeños que deben sobrevivir al cierre de la aplicación. Aquí se usa
 * para recordar quién inició sesión, de modo que al volver a abrir la app el
 * usuario entre directamente a Inicio sin repetir sus credenciales.
 *
 * No guarda la contraseña: solo el correo, el nombre y el instante del acceso.
 * La verificación de identidad corresponde al servicio de autenticación.
 */
class SesionUsuario(context: Context) {

    private val preferencias: SharedPreferences =
        context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)

    /** Correo de la sesión abierta, o null si no hay ninguna. */
    val correo: String? get() = preferencias.getString(CLAVE_CORREO, null)

    /** Nombre de quien tiene la sesión abierta, para saludarlo sin consultar el backend. */
    val nombre: String? get() = preferencias.getString(CLAVE_NOMBRE, null)

    /** Marca de tiempo del último acceso, en milisegundos desde época. */
    val ultimoAcceso: Long get() = preferencias.getLong(CLAVE_ACCESO, 0L)

    /** Indica si hay una sesión guardada que permita saltarse el Login. */
    val haySesionActiva: Boolean get() = !correo.isNullOrBlank()

    /**
     * Registra el inicio de sesión.
     *
     * `edit` es la extensión de androidx.core que abre la transacción, aplica
     * los cambios y los confirma, evitando el olvido habitual de llamar apply.
     */
    fun abrir(usuario: Usuario) {
        preferencias.edit {
            putString(CLAVE_CORREO, usuario.correoNormalizado)
            putString(CLAVE_NOMBRE, usuario.nombre)
            putLong(CLAVE_ACCESO, System.currentTimeMillis())
        }
    }

    /** Actualiza el nombre guardado tras una edición del perfil. */
    fun actualizarNombre(nombre: String) {
        if (!haySesionActiva) return
        preferencias.edit { putString(CLAVE_NOMBRE, nombre) }
    }

    /** Borra la sesión. Se invoca al cerrar sesión y al eliminar la cuenta. */
    fun cerrar() {
        preferencias.edit { clear() }
    }

    private companion object {
        const val ARCHIVO = "vozvisible_sesion"
        const val CLAVE_CORREO = "correo"
        const val CLAVE_NOMBRE = "nombre"
        const val CLAVE_ACCESO = "ultimo_acceso"
    }
}
