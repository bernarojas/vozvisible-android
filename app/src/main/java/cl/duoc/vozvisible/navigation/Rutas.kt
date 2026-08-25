package cl.duoc.vozvisible.navigation

import android.net.Uri

/**
 * Identificadores de las views registradas en el NavHost.
 * Se centralizan como constantes para evitar errores de tipeo en las llamadas a navigate().
 */
object Rutas {
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val RECUPERAR = "recuperar"

    /** Nombre del argumento que viaja hacia la view de Inicio. */
    const val ARG_CORREO = "correo"

    /** Patrón de la ruta: el tramo entre llaves es un argumento de navegación. */
    const val INICIO = "inicio/{$ARG_CORREO}"

    /**
     * Construye la ruta concreta hacia Inicio para un usuario dado.
     *
     * El correo se codifica con Uri.encode porque viaja dentro de la URL de
     * navegación y contiene caracteres como @ que deben escaparse.
     */
    fun inicioDe(correo: String): String = "inicio/${Uri.encode(correo)}"
}
