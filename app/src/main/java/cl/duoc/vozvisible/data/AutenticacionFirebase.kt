package cl.duoc.vozvisible.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.tasks.await

/**
 * Envoltura de Firebase Authentication.
 *
 * Aísla al resto de la aplicación del SDK de Firebase: las views y el
 * repositorio hablan con esta clase, que traduce las excepciones del servicio a
 * los desenlaces propios del dominio. Si mañana se cambia de proveedor, el
 * cambio queda contenido aquí.
 *
 * Las operaciones de Firebase devuelven un Task, que es su propio mecanismo
 * asíncrono. La extensión `await` de kotlinx-coroutines-play-services lo
 * convierte en una función de suspensión, de modo que el código se lee
 * secuencial en vez de encadenar callbacks.
 */
class AutenticacionFirebase(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    /** Identificador del usuario con sesión abierta en Firebase, o null. */
    val uidActual: String? get() = auth.currentUser?.uid

    /** Correo de la cuenta autenticada, o null si no hay ninguna. */
    val correoActual: String? get() = auth.currentUser?.email

    /** Indica si Firebase mantiene una sesión viva. */
    val haySesion: Boolean get() = auth.currentUser != null

    /**
     * Crea la cuenta en el servicio de autenticación.
     *
     * @return el uid asignado por Firebase, o el desenlace del fallo.
     */
    suspend fun registrar(correo: String, password: String): ResultadoAuth =
        ejecutar {
            val credencial = auth.createUserWithEmailAndPassword(correo.trim(), password).await()
            ResultadoAuth.Exitoso(
                uid = credencial.user?.uid.orEmpty(),
                correo = credencial.user?.email.orEmpty()
            )
        }

    /** Valida las credenciales contra el servicio y abre la sesión. */
    suspend fun iniciarSesion(correo: String, password: String): ResultadoAuth =
        ejecutar {
            val credencial = auth.signInWithEmailAndPassword(correo.trim(), password).await()
            ResultadoAuth.Exitoso(
                uid = credencial.user?.uid.orEmpty(),
                correo = credencial.user?.email.orEmpty()
            )
        }

    /**
     * Envía el correo de restablecimiento de contraseña.
     *
     * Reemplaza la simulación de la entrega anterior: ahora el mensaje lo envía
     * Firebase de verdad, con un enlace de un solo uso.
     */
    suspend fun enviarCorreoRecuperacion(correo: String): ResultadoAuth =
        ejecutar {
            auth.sendPasswordResetEmail(correo.trim()).await()
            ResultadoAuth.CorreoEnviado(correo.trim().lowercase())
        }

    /** Cierra la sesión en el servicio. No borra la cuenta. */
    fun cerrarSesion() = auth.signOut()

    /** Elimina del servicio la cuenta que tiene la sesión abierta. */
    suspend fun eliminarCuenta(): ResultadoAuth =
        ejecutar {
            val usuario = auth.currentUser
                ?: return@ejecutar ResultadoAuth.Fallo("No hay ninguna sesión abierta.")
            val correo = usuario.email.orEmpty()
            usuario.delete().await()
            ResultadoAuth.CuentaEliminada(correo)
        }

    /**
     * Ejecuta una operación contra Firebase traduciendo sus excepciones.
     *
     * Centralizar el catch evita repetir el mismo bloque en cada método y
     * garantiza que ningún fallo del servicio llegue crudo a la interfaz.
     */
    private suspend fun ejecutar(operacion: suspend () -> ResultadoAuth): ResultadoAuth =
        try {
            operacion()
        } catch (e: FirebaseAuthWeakPasswordException) {
            ResultadoAuth.Fallo("La contraseña es demasiado débil para el servicio.")
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            ResultadoAuth.Fallo("Correo o contraseña incorrectos.")
        } catch (e: FirebaseAuthInvalidUserException) {
            ResultadoAuth.Fallo("No existe ninguna cuenta con ese correo.")
        } catch (e: FirebaseAuthUserCollisionException) {
            ResultadoAuth.Fallo("Ese correo ya tiene una cuenta en el servicio.")
        } catch (e: Exception) {
            // Último recurso: sin conexión, servicio caído o error no previsto.
            ResultadoAuth.Fallo(
                e.localizedMessage ?: "No pudimos contactar al servicio de autenticación."
            )
        }
}

/**
 * Desenlace de una operación contra el servicio de autenticación.
 *
 * Igual que el resto de los resultados del proyecto, se modela con una interfaz
 * sellada para que el `when` de la view sea exhaustivo.
 */
sealed interface ResultadoAuth {

    /** La cuenta quedó creada o la sesión abierta. */
    data class Exitoso(val uid: String, val correo: String) : ResultadoAuth

    /** Firebase envió el correo de restablecimiento. */
    data class CorreoEnviado(val correo: String) : ResultadoAuth

    /** La cuenta se eliminó del servicio. */
    data class CuentaEliminada(val correo: String) : ResultadoAuth

    /** La operación no se completó; el mensaje ya viene traducido al dominio. */
    data class Fallo(val motivo: String) : ResultadoAuth

    /** Verdadero mientras no se trate de un fallo. */
    val fueExitoso: Boolean get() = this !is Fallo
}
