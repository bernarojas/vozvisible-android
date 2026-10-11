package cl.duoc.vozvisible.data

/**
 * Contrato del almacén del historial de interacciones.
 *
 * Cumple el mismo papel que [FuenteUsuarios]: el repositorio depende de esta
 * interfaz y no de Firestore, de modo que las pruebas unitarias trabajan con un
 * objeto simulado, sin red ni emulador.
 */
interface FuenteInteracciones {

    /** Lee las interacciones de un usuario. */
    suspend fun cargarDe(correo: String): List<Interaccion>

    /** Inserta una interacción nueva. */
    suspend fun guardar(interaccion: Interaccion)

    /** Cambia la marca de favorita de una interacción existente. */
    suspend fun marcarFavorita(id: String, favorita: Boolean)

    /** Borra una interacción. */
    suspend fun eliminar(id: String)

    /** Borra todo el historial de un usuario. */
    suspend fun vaciar(correo: String)

    /** Traspasa el historial a un correo nuevo, cuando el usuario lo cambia en su perfil. */
    suspend fun reasignar(correoAnterior: String, correoNuevo: String)
}
