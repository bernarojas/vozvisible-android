package cl.duoc.vozvisible.data

/**
 * Las dos formas de comunicarse que ofrece la aplicación.
 *
 * Cada interacción guardada en el historial pertenece a uno de estos tipos,
 * que corresponden a las dos funciones del menú principal: escribir para que
 * la aplicación hable, o escuchar para que la aplicación transcriba.
 *
 * @property titulo nombre visible de la función.
 * @property solicitudTipica texto que describe lo que pidió el usuario cuando la
 *   solicitud no es un texto escrito por él, como ocurre al transcribir.
 */
enum class TipoInteraccion(val titulo: String, val solicitudTipica: String) {
    TEXTO_A_VOZ("Texto a voz", "Reproducir en voz alta un mensaje escrito"),
    VOZ_A_TEXTO("Voz a texto", "Transcribir lo que dice el interlocutor");

    override fun toString(): String = titulo
}
