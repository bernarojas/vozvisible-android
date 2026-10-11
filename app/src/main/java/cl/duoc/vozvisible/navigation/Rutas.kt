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

    /** View de perfil, donde se modifican y eliminan los datos de la cuenta. */
    const val PERFIL = "perfil/{$ARG_CORREO}"

    /** View de Texto a voz: el usuario escribe y la aplicación habla. */
    const val TEXTO_A_VOZ = "texto-a-voz/{$ARG_CORREO}"

    /** View de Voz a texto: la aplicación escucha y transcribe. */
    const val VOZ_A_TEXTO = "voz-a-texto/{$ARG_CORREO}"

    /** View con el historial de solicitudes y respuestas. */
    const val HISTORIAL = "historial/{$ARG_CORREO}"

    /** Tutorial de uso. No necesita saber quién es el usuario. */
    const val AYUDA = "ayuda"

    fun textoAVozDe(correo: String): String = "texto-a-voz/${Uri.encode(correo)}"

    fun vozATextoDe(correo: String): String = "voz-a-texto/${Uri.encode(correo)}"

    fun historialDe(correo: String): String = "historial/${Uri.encode(correo)}"

    /**
     * Construye la ruta concreta hacia Inicio para un usuario dado.
     *
     * El correo se codifica con Uri.encode porque viaja dentro de la URL de
     * navegación y contiene caracteres como @ que deben escaparse.
     */
    fun inicioDe(correo: String): String = "inicio/${Uri.encode(correo)}"

    /** Construye la ruta concreta hacia Perfil para un usuario dado. */
    fun perfilDe(correo: String): String = "perfil/${Uri.encode(correo)}"
}
