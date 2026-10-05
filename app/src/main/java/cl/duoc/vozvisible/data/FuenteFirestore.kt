package cl.duoc.vozvisible.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Implementación de [FuenteUsuarios] sobre Cloud Firestore.
 *
 * Cada usuario es un documento de la colección `usuarios`, identificado por su
 * correo normalizado. Usar el correo como identificador del documento hace que
 * la búsqueda por correo sea una lectura directa y que el propio motor impida
 * duplicados, sin necesidad de recorrer la colección.
 *
 * La contraseña no se guarda aquí. De las credenciales se encarga Firebase
 * Authentication, que almacena solo un hash en la infraestructura de Google:
 * replicarla en Firestore sería exponerla sin ninguna ganancia.
 */
class FuenteFirestore(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : FuenteUsuarios {

    private val coleccion get() = db.collection(COLECCION)

    override suspend fun cargarTodos(): List<Usuario> =
        coleccion.get().await()
            .documents
            .mapNotNull { documento -> documento.data?.aUsuario() }
            .sortedBy { usuario -> usuario.nombre }

    override suspend fun guardar(usuario: Usuario) {
        coleccion.document(usuario.correoNormalizado).set(usuario.aMapa()).await()
    }

    override suspend fun actualizar(correoOriginal: String, usuario: Usuario) {
        val claveOriginal = correoOriginal.lowercase().trim()

        // El identificador del documento es inmutable: si el correo cambió hay
        // que crear el documento nuevo y borrar el anterior.
        if (claveOriginal != usuario.correoNormalizado) {
            coleccion.document(usuario.correoNormalizado).set(usuario.aMapa()).await()
            coleccion.document(claveOriginal).delete().await()
        } else {
            coleccion.document(claveOriginal).set(usuario.aMapa()).await()
        }
    }

    override suspend fun eliminar(correo: String) {
        coleccion.document(correo.lowercase().trim()).delete().await()
    }

    private companion object {
        const val COLECCION = "usuarios"
    }
}

/**
 * Convierte el modelo a la forma que entiende Firestore.
 *
 * Los enums viajan por su nombre interno y no por su etiqueta visible: así un
 * cambio de redacción en la interfaz no invalida los datos ya almacenados.
 */
private fun Usuario.aMapa(): Map<String, Any> = mapOf(
    "nombre" to nombre,
    "correo" to correoNormalizado,
    "region" to region.name,
    "modoPreferido" to modoPreferido.name,
    "preferencias" to preferencias.map { apoyo -> apoyo.name }
)

/**
 * Reconstruye el modelo desde un documento.
 *
 * Cada campo se resuelve con un valor de respaldo, de modo que un documento
 * incompleto o escrito por una versión anterior no haga caer la aplicación.
 * El campo de contraseña queda vacío porque las credenciales viven en
 * Authentication, no aquí.
 */
private fun Map<String, Any?>.aUsuario(): Usuario? {
    val correo = this["correo"] as? String ?: return null

    val apoyos = (this["preferencias"] as? List<*>)
        .orEmpty()
        .mapNotNull { nombre -> nombre as? String }
        .mapNotNull { nombre -> runCatching { ApoyoAccesibilidad.valueOf(nombre) }.getOrNull() }
        .toSet()

    return Usuario(
        nombre = this["nombre"] as? String ?: correo,
        correo = correo,
        password = "",
        region = (this["region"] as? String)
            ?.let { nombre -> runCatching { Region.valueOf(nombre) }.getOrNull() }
            ?: Region.METROPOLITANA,
        modoPreferido = (this["modoPreferido"] as? String)
            ?.let { nombre -> runCatching { ModoComunicacion.valueOf(nombre) }.getOrNull() }
            ?: ModoComunicacion.AMBOS,
        preferencias = apoyos
    )
}
