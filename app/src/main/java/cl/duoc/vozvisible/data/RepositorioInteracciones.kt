package cl.duoc.vozvisible.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cl.duoc.vozvisible.util.normalizado
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

/**
 * Historial de solicitudes y respuestas de la aplicación.
 *
 * Sigue el mismo diseño que [RepositorioUsuarios]: una lista observable en
 * memoria, que Compose vigila para recomponer las views, y un almacén remoto
 * opcional detrás. Las reglas de negocio se aplican siempre sobre la lista local
 * y solo lo que las supera se propaga al servidor.
 *
 * Completa el segundo conjunto de operaciones CRUD de la aplicación: se crea una
 * interacción por cada uso de Texto a voz o Voz a texto, se consulta en la view
 * Historial, se modifica al marcarla como favorita y se elimina de a una o
 * vaciando el historial completo.
 */
object RepositorioInteracciones {

    /**
     * Tiempo máximo que se espera la confirmación del servidor.
     *
     * Firestore conserva en el dispositivo las escrituras hechas sin conexión y
     * las envía cuando la red vuelve, pero la tarea no termina hasta recibir la
     * confirmación. Sin este límite, la view quedaría esperando indefinidamente
     * mientras el teléfono no tenga señal.
     */
    internal const val ESPERA_MAXIMA_MS = 5_000L

    /** Frases que se ofrecen aunque el usuario aún no haya marcado ninguna favorita. */
    val FRASES_PREDETERMINADAS: List<String> = listOf(
        "Hola, soy una persona sorda. ¿Puede hablarme despacio y de frente?",
        "¿Me lo puede escribir, por favor?",
        "Gracias por su paciencia.",
        "Necesito ayuda, por favor.",
        "¿Cuánto cuesta?",
        "No escuché bien, ¿puede repetirlo?"
    )

    private val interacciones = mutableStateListOf<Interaccion>()

    private var fuente: FuenteInteracciones? = null

    /**
     * Indica si hay una carga del servidor en curso.
     *
     * El setter es privado: las views lo leen para mostrar un indicador, pero
     * solo el repositorio decide cuándo cambia.
     */
    var cargando: Boolean by mutableStateOf(false)
        private set

    /** Asocia el almacén remoto. Se invoca una sola vez al arrancar la app. */
    fun conectar(almacen: FuenteInteracciones) {
        fuente = almacen
    }

    /** Interacciones de un usuario, de la más reciente a la más antigua. */
    fun delUsuario(correo: String): List<Interaccion> {
        val objetivo = correo.normalizado()
        return interacciones
            .filter { interaccion -> interaccion.correoUsuario == objetivo }
            .sortedByDescending { interaccion -> interaccion.fecha }
    }

    /** Busca una interacción por su identificador. */
    fun buscar(id: String): Interaccion? = interacciones.firstOrNull { it.id == id }

    /** Resumen del historial de un usuario. */
    fun resumenDe(correo: String): ResumenHistorial = ResumenHistorial.calcular(delUsuario(correo))

    /**
     * Frases rápidas del usuario: primero sus favoritas y luego las
     * predeterminadas, sin repetir ninguna.
     *
     * `distinctBy` sobre el texto en minúsculas evita que una favorita idéntica a
     * una frase predeterminada aparezca dos veces.
     */
    fun frasesRapidasDe(correo: String): List<String> =
        (delUsuario(correo)
            .filter { interaccion -> interaccion.favorita }
            .map { interaccion -> interaccion.textoReutilizable } + FRASES_PREDETERMINADAS)
            .filter { frase -> frase.isNotBlank() }
            .distinctBy { frase -> frase.normalizado() }

    /**
     * Trae del servidor el historial del usuario y reemplaza con él la copia local.
     *
     * El bloque `finally` garantiza que el indicador de carga se apague siempre,
     * tanto si la lectura termina bien como si lanza una excepción: sin él, un
     * fallo de red dejaría la view mostrando "Cargando" para siempre.
     *
     * @return true si la lectura se completó.
     */
    suspend fun cargar(correo: String): Boolean {
        val almacen = fuente ?: return false
        val objetivo = correo.normalizado()

        cargando = true
        try {
            val remotas = almacen.cargarDe(objetivo)
            interacciones.removeAll { interaccion -> interaccion.correoUsuario == objetivo }
            interacciones.addAll(remotas)
            return true
        } catch (e: CancellationException) {
            // La cancelación no es un error: se deja pasar para que la corrutina
            // que la pidió termine como corresponde.
            throw e
        } catch (e: Exception) {
            return false
        } finally {
            cargando = false
        }
    }

    /**
     * Valida y guarda una interacción, primero en memoria y luego en el servidor.
     *
     * La validación va antes que cualquier escritura: una solicitud vacía no
     * llega ni a la lista ni a Firestore.
     */
    suspend fun registrar(interaccion: Interaccion): ResultadoInteraccion {
        validar(interaccion)?.let { rechazo -> return rechazo }

        interacciones.add(interaccion)

        // Sin almacén conectado, como en las pruebas, no hay servidor que
        // confirme ni que rechace: la escritura queda solo en memoria.
        val almacen = fuente
            ?: return ResultadoInteraccion.Guardada(interaccion, Sincronizacion.PENDIENTE)

        val sincronizacion = escribirEnServidor { almacen.guardar(interaccion) }
        return ResultadoInteraccion.Guardada(interaccion, sincronizacion)
    }

    /**
     * Invierte la marca de favorita.
     *
     * Las data class son inmutables: en vez de cambiar el objeto se reemplaza
     * por una copia con el campo distinto, que es lo que Compose detecta.
     *
     * @return la interacción actualizada, o null si no existe.
     */
    suspend fun alternarFavorita(id: String): Interaccion? {
        val posicion = interacciones.indexOfFirst { it.id == id }
        if (posicion < 0) return null

        val actualizada = interacciones[posicion].let { it.copy(favorita = !it.favorita) }
        interacciones[posicion] = actualizada

        fuente?.let { almacen ->
            escribirEnServidor { almacen.marcarFavorita(id, actualizada.favorita) }
        }
        return actualizada
    }

    /** Elimina una interacción. Devuelve false si no existía. */
    suspend fun eliminar(id: String): Boolean {
        val eliminada = interacciones.removeAll { it.id == id }
        if (!eliminada) return false

        fuente?.let { almacen -> escribirEnServidor { almacen.eliminar(id) } }
        return true
    }

    /**
     * Borra todo el historial del usuario.
     *
     * Se invoca también al eliminar la cuenta, para no dejar en el servidor
     * interacciones de alguien que ya no existe.
     *
     * @return cantidad de interacciones eliminadas de la copia local.
     */
    suspend fun vaciar(correo: String): Int {
        val objetivo = correo.normalizado()
        val antes = interacciones.size
        interacciones.removeAll { interaccion -> interaccion.correoUsuario == objetivo }

        fuente?.let { almacen -> escribirEnServidor { almacen.vaciar(objetivo) } }
        return antes - interacciones.size
    }

    /**
     * Traspasa el historial al correo nuevo cuando el usuario lo cambia en su
     * perfil. Sin este paso, las interacciones quedarían asociadas a un correo
     * que ya no identifica a nadie.
     */
    suspend fun reasignar(correoAnterior: String, correoNuevo: String) {
        val anterior = correoAnterior.normalizado()
        val nuevo = correoNuevo.normalizado()
        if (anterior == nuevo) return

        // Se recorre por índice porque cada elemento se reemplaza en su misma
        // posición por una copia con el correo nuevo.
        for (indice in interacciones.indices) {
            val interaccion = interacciones[indice]
            if (interaccion.correoUsuario == anterior) {
                interacciones[indice] = interaccion.copy(correoUsuario = nuevo)
            }
        }

        fuente?.let { almacen -> escribirEnServidor { almacen.reasignar(anterior, nuevo) } }
    }

    /**
     * Reglas que debe cumplir una interacción antes de guardarse.
     *
     * Se expone como función pura para cubrirla con pruebas unitarias.
     */
    fun validar(interaccion: Interaccion): ResultadoInteraccion.Rechazada? = when {
        interaccion.correoUsuario.isBlank() ->
            ResultadoInteraccion.Rechazada("No hay una sesión asociada a la interacción.")

        interaccion.solicitud.isBlank() ->
            ResultadoInteraccion.Rechazada("Escribe un mensaje antes de reproducirlo.")

        interaccion.respuesta.isBlank() ->
            ResultadoInteraccion.Rechazada("No se obtuvo ninguna respuesta que guardar.")

        interaccion.solicitud.length > Interaccion.LARGO_MAXIMO ||
            interaccion.respuesta.length > Interaccion.LARGO_MAXIMO ->
            ResultadoInteraccion.Rechazada(
                "El mensaje supera los ${Interaccion.LARGO_MAXIMO} caracteres permitidos."
            )

        else -> null
    }

    /**
     * Ejecuta una escritura remota con un plazo máximo.
     *
     * Cada bloque catch atiende un desenlace distinto. Si el plazo vence,
     * Firestore ya dejó la escritura en su cola local y la enviará al recuperar
     * la conexión. Si la corrutina se cancela, la cancelación se propaga.
     * Cualquier otro error, como un rechazo de las reglas de seguridad, se
     * absorbe: la copia local ya refleja el cambio y la aplicación no se cae.
     *
     * El orden de los catch importa: TimeoutCancellationException es una
     * subclase de CancellationException y debe atraparse antes.
     */
    private suspend fun escribirEnServidor(escritura: suspend () -> Unit): Sincronizacion =
        try {
            withTimeout(ESPERA_MAXIMA_MS) { escritura() }
            Sincronizacion.CONFIRMADA
        } catch (e: TimeoutCancellationException) {
            Sincronizacion.PENDIENTE
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Sincronizacion.RECHAZADA
        }

    /**
     * Deja el repositorio vacío y sin almacén.
     *
     * Es `internal`: visible para las pruebas del mismo módulo, pero no forma
     * parte de lo que una view debería poder invocar.
     */
    internal fun restaurar() {
        interacciones.clear()
        fuente = null
        cargando = false
    }
}
