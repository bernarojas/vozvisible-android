package cl.duoc.vozvisible.data

/**
 * Contrato del almacén de usuarios.
 *
 * El repositorio no habla con Firestore directamente, sino contra esta
 * interfaz. Eso permite dos cosas: sustituir el motor de persistencia sin
 * tocar las views, y ejecutar las pruebas unitarias contra una implementación
 * en memoria, sin red ni emulador.
 *
 * Las cuatro operaciones son de suspensión porque el acceso a un servicio
 * remoto no es inmediato y no debe bloquear el hilo de la interfaz.
 */
interface FuenteUsuarios {

    /** Lee todos los perfiles almacenados. */
    suspend fun cargarTodos(): List<Usuario>

    /** Inserta un perfil nuevo. */
    suspend fun guardar(usuario: Usuario)

    /**
     * Reemplaza un perfil existente.
     *
     * @param correoOriginal clave con la que el perfil está almacenado, que
     *   puede diferir del correo nuevo si la edición lo cambió.
     */
    suspend fun actualizar(correoOriginal: String, usuario: Usuario)

    /** Borra el perfil asociado al correo. */
    suspend fun eliminar(correo: String)
}
