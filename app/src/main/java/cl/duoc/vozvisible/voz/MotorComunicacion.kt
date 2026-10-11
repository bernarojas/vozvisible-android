package cl.duoc.vozvisible.voz

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Base común de los dos motores de comunicación de la aplicación.
 *
 * Texto a voz y Voz a texto envuelven servicios distintos de Android, pero
 * comparten el mismo ciclo de vida: se preparan al crearse, pueden estar listos
 * o no disponibles en el dispositivo, trabajan, se detienen y al final liberan
 * los recursos del sistema. Esa parte común vive aquí; cada subclase completa
 * lo que es propio de su servicio.
 *
 * Es abstracta porque no tiene sentido crear un motor genérico: solo existen
 * motores concretos. El estado es observable por Compose, de modo que la view
 * se redibuja sola cuando el motor cambia de estado.
 *
 * @param contexto contexto de la aplicación. Es `protected`: las subclases lo
 *   necesitan para crear sus servicios, pero nadie de fuera debe tocarlo.
 */
abstract class MotorComunicacion(protected val contexto: Context) {

    /** Nombre del motor para mostrar en pantalla. */
    abstract val nombre: String

    /** Estado actual. Solo el propio motor y sus subclases pueden cambiarlo. */
    var estado: EstadoMotor by mutableStateOf<EstadoMotor>(EstadoMotor.Preparando)
        protected set

    /**
     * Verdadero cuando el motor puede recibir una orden nueva. Tras un fallo
     * también lo está: el error fue de esa operación, no del servicio.
     */
    val estaListo: Boolean
        get() = estado == EstadoMotor.Listo || estado is EstadoMotor.Fallo

    /** Verdadero mientras el motor está hablando o escuchando. */
    val estaTrabajando: Boolean
        get() = estado is EstadoMotor.Hablando || estado is EstadoMotor.Escuchando

    /**
     * Los servicios de voz avisan de sus eventos desde hilos propios. Todo
     * cambio de estado se reenvía al hilo principal, que es donde Compose
     * espera que ocurran.
     */
    private val hiloPrincipal = Handler(Looper.getMainLooper())

    protected fun enHiloPrincipal(accion: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) accion()
        else hiloPrincipal.post(accion)
    }

    /** Interrumpe lo que el motor esté haciendo, sin liberarlo. */
    abstract fun detener()

    /**
     * Libera los recursos del sistema. Después de llamarla el motor no se puede
     * volver a usar.
     *
     * Es `open`: las subclases la extienden para cerrar su servicio y luego
     * invocan esta versión con `super.liberar()`.
     */
    open fun liberar() {
        hiloPrincipal.removeCallbacksAndMessages(null)
        estado = EstadoMotor.Liberado
    }

    /** Descripción del estado para mostrar al usuario. */
    open fun describirEstado(): String = when (val actual = estado) {
        EstadoMotor.Preparando -> "Preparando $nombre…"
        EstadoMotor.Listo -> "$nombre listo"
        is EstadoMotor.Hablando -> "Reproduciendo fragmento ${actual.fragmento} de ${actual.total}"
        is EstadoMotor.Escuchando -> "Escuchando…"
        is EstadoMotor.NoDisponible -> actual.motivo
        is EstadoMotor.Fallo -> actual.motivo
        EstadoMotor.Liberado -> "$nombre detenido"
    }
}

/**
 * Estados por los que pasa un motor de comunicación.
 *
 * Con una interfaz sellada el `when` de la view cubre todos los casos sin rama
 * `else`, y cada estado transporta solo los datos que le corresponden.
 */
sealed interface EstadoMotor {

    /** El servicio del sistema todavía se está inicializando. */
    data object Preparando : EstadoMotor

    /** Listo para recibir una orden. */
    data object Listo : EstadoMotor

    /** Reproduciendo un texto; el avance se muestra en pantalla. */
    data class Hablando(val fragmento: Int, val total: Int) : EstadoMotor {
        val progreso: Float get() = fragmento.toFloat() / total
    }

    /**
     * Escuchando al interlocutor.
     *
     * @property parcial transcripción provisional mientras la persona habla.
     * @property nivel intensidad del sonido entre 0 y 1, para la barra visual.
     */
    data class Escuchando(val parcial: String = "", val nivel: Float = 0f) : EstadoMotor

    /** El dispositivo no ofrece el servicio. */
    data class NoDisponible(val motivo: String) : EstadoMotor

    /** La última operación falló; el motor puede volver a intentarse. */
    data class Fallo(val motivo: String) : EstadoMotor

    /** El motor liberó sus recursos y ya no se puede usar. */
    data object Liberado : EstadoMotor
}
