package cl.duoc.vozvisible.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [FuenteInteracciones] sobre Cloud Firestore.
 *
 * Cada interacción es un documento de la colección `interacciones`, con su
 * propio identificador y un campo `correo` que indica a quién pertenece. Se
 * eligió una colección de primer nivel y no una subcolección dentro de cada
 * usuario porque el documento del usuario cambia de identificador cuando se
 * edita el correo, y una subcolección quedaría huérfana bajo la clave antigua.
 *
 * La consulta filtra solo por correo y el orden se aplica en el dispositivo:
 * combinar un filtro con un orden sobre otro campo obligaría a crear un índice
 * compuesto en la consola de Firebase.
 */
class FirestoreInteracciones(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FuenteInteracciones {

    private val coleccion get() = db.collection(COLECCION)

    override suspend fun cargarDe(correo: String): List<Interaccion> =
        coleccion.whereEqualTo(CAMPO_CORREO, correo).get().await()
            .documents
            .mapNotNull { documento -> documento.aInteraccion() }
            .sortedByDescending { interaccion -> interaccion.fecha }

    override suspend fun guardar(interaccion: Interaccion) {
        coleccion.document(interaccion.id).set(interaccion.aMapa()).await()
    }

    override suspend fun marcarFavorita(id: String, favorita: Boolean) {
        coleccion.document(id).update(CAMPO_FAVORITA, favorita).await()
    }

    override suspend fun eliminar(id: String) {
        coleccion.document(id).delete().await()
    }

    override suspend fun vaciar(correo: String) {
        val documentos = coleccion.whereEqualTo(CAMPO_CORREO, correo).get().await().documents

        // Un lote aplica todos los borrados en una sola escritura atómica: o se
        // eliminan todos o ninguno, sin dejar el historial a medias.
        db.runBatch { lote ->
            for (documento in documentos) lote.delete(documento.reference)
        }.await()
    }

    override suspend fun reasignar(correoAnterior: String, correoNuevo: String) {
        val documentos = coleccion.whereEqualTo(CAMPO_CORREO, correoAnterior).get().await().documents

        db.runBatch { lote ->
            for (documento in documentos) lote.update(documento.reference, CAMPO_CORREO, correoNuevo)
        }.await()
    }

    private companion object {
        const val COLECCION = "interacciones"
        const val CAMPO_CORREO = "correo"
        const val CAMPO_FAVORITA = "favorita"
    }
}

/** Convierte el modelo al mapa que guarda Firestore. El enum viaja por su nombre interno. */
private fun Interaccion.aMapa(): Map<String, Any> = mapOf(
    "correo" to correoUsuario,
    "tipo" to tipo.name,
    "solicitud" to solicitud,
    "respuesta" to respuesta,
    "fecha" to fecha,
    "favorita" to favorita
)

/**
 * Reconstruye el modelo desde un documento.
 *
 * Un documento sin correo o con un tipo desconocido se descarta en lugar de
 * hacer caer la carga completa del historial.
 */
private fun DocumentSnapshot.aInteraccion(): Interaccion? {
    val correo = getString("correo") ?: return null
    val tipo = getString("tipo")
        ?.let { nombre -> runCatching { TipoInteraccion.valueOf(nombre) }.getOrNull() }
        ?: return null

    return Interaccion(
        id = id,
        correoUsuario = correo,
        tipo = tipo,
        solicitud = getString("solicitud").orEmpty(),
        respuesta = getString("respuesta").orEmpty(),
        fecha = getLong("fecha") ?: 0L,
        favorita = getBoolean("favorita") ?: false
    )
}
