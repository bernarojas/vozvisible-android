package cl.duoc.vozvisible.data

import cl.duoc.vozvisible.util.normalizado
import java.util.UUID

/**
 * Una solicitud del usuario y la respuesta que generó la aplicación.
 *
 * Es el registro que se guarda cada vez que el usuario usa una de las dos
 * funciones de comunicación. En Texto a voz la solicitud es el mensaje escrito
 * y la respuesta, la confirmación de que se reprodujo; en Voz a texto la
 * solicitud es la escucha y la respuesta, el texto transcrito.
 *
 * @property id identificador único; también es el del documento en Firestore.
 * @property correoUsuario dueño de la interacción, ya normalizado.
 * @property tipo función que la originó.
 * @property solicitud lo que pidió el usuario.
 * @property respuesta lo que entregó la aplicación.
 * @property fecha instante de creación, en milisegundos desde época.
 * @property favorita marcada por el usuario para reutilizarla como frase rápida.
 */
data class Interaccion(
    val id: String,
    val correoUsuario: String,
    val tipo: TipoInteraccion,
    val solicitud: String,
    val respuesta: String,
    val fecha: Long,
    val favorita: Boolean = false
) {

    /**
     * Texto que se puede volver a decir en voz alta.
     *
     * En Texto a voz es lo que escribió el usuario; en Voz a texto, lo que se
     * transcribió. Así cualquier interacción puede reutilizarse como frase.
     */
    val textoReutilizable: String
        get() = when (tipo) {
            TipoInteraccion.TEXTO_A_VOZ -> solicitud
            TipoInteraccion.VOZ_A_TEXTO -> respuesta
        }

    companion object {

        /** Largo máximo de la solicitud y de la respuesta. */
        const val LARGO_MAXIMO = 1000

        /**
         * Crea una interacción nueva con identificador y fecha propios.
         *
         * El identificador se genera en el dispositivo y no en Firestore: así el
         * registro existe completo antes de salir a la red, y si la escritura
         * remota falla la copia local ya tiene con qué identificarse.
         */
        fun nueva(
            correoUsuario: String,
            tipo: TipoInteraccion,
            solicitud: String,
            respuesta: String,
            fecha: Long = System.currentTimeMillis()
        ): Interaccion = Interaccion(
            id = UUID.randomUUID().toString(),
            correoUsuario = correoUsuario.normalizado(),
            tipo = tipo,
            solicitud = solicitud.trim(),
            respuesta = respuesta.trim(),
            fecha = fecha
        )
    }
}

/**
 * Desenlace de un intento de guardar una interacción.
 *
 * Igual que los demás resultados del proyecto, se modela con una interfaz
 * sellada para que el `when` de la view sea exhaustivo.
 */
sealed interface ResultadoInteraccion {

    /**
     * La interacción quedó en el historial.
     *
     * @property sincronizacion qué pasó con la copia del servidor. En cualquier
     *   caso la copia local sigue visible y la aplicación continúa operativa.
     */
    data class Guardada(
        val interaccion: Interaccion,
        val sincronizacion: Sincronizacion
    ) : ResultadoInteraccion

    /** La interacción no pasó la validación local y no se guardó en ninguna parte. */
    data class Rechazada(val motivo: String) : ResultadoInteraccion

    val fueExitoso: Boolean get() = this is Guardada

    val mensaje: String
        get() = when (this) {
            is Guardada -> when (sincronizacion) {
                Sincronizacion.CONFIRMADA -> "Guardado en tu historial."
                Sincronizacion.PENDIENTE ->
                    "Guardado en este dispositivo. Se enviará al servidor cuando vuelva la conexión."
                Sincronizacion.RECHAZADA ->
                    "Guardado solo en este dispositivo: el servidor no aceptó la escritura."
            }

            is Rechazada -> motivo
        }
}

/**
 * Desenlace de una escritura en el servidor.
 *
 * Se distinguen los dos fallos porque Firestore los trata distinto: sin
 * conexión, la escritura queda en cola y se envía después; si el servidor la
 * rechaza, por ejemplo por las reglas de seguridad, se descarta y no se
 * reintenta. Prometer al usuario una sincronización que no ocurrirá sería
 * engañarlo.
 */
enum class Sincronizacion {
    /** El servidor confirmó la escritura. */
    CONFIRMADA,

    /** No hubo respuesta a tiempo; Firestore la envía al recuperar la conexión. */
    PENDIENTE,

    /** El servidor la rechazó y no se reintentará. */
    RECHAZADA
}
