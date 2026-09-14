package cl.duoc.vozvisible.data

/**
 * Desenlace de un intento de registro.
 *
 * Se modela con una `sealed interface` en lugar de devolver `String?`: el
 * conjunto de subtipos es cerrado y conocido en compilación, por lo que un
 * `when` sobre el resultado es exhaustivo y el compilador avisa si en el futuro
 * se agrega un caso nuevo sin tratarlo. Además cada desenlace transporta sus
 * propios datos en vez de esconderlos dentro de un mensaje de texto.
 */
sealed interface ResultadoRegistro {

    /** El usuario quedó almacenado en el arreglo. */
    data class Exitoso(val usuario: Usuario, val totalRegistrados: Int) : ResultadoRegistro

    /** El arreglo ya alcanzó su cupo máximo. */
    data class SinCupo(val maximo: Int) : ResultadoRegistro

    /** Ya existe una cuenta con ese correo. */
    data class CorreoDuplicado(val correo: String) : ResultadoRegistro

    /** Verdadero solo para el desenlace correcto; simplifica las condiciones en la view. */
    val fueExitoso: Boolean get() = this is Exitoso

    /** Mensaje listo para mostrar, derivado del propio desenlace. */
    val mensaje: String
        get() = when (this) {
            is Exitoso ->
                "Usuario ${usuario.primerNombre} registrado correctamente " +
                    "($totalRegistrados de ${RepositorioUsuarios.MAX_USUARIOS}). Ya puedes iniciar sesión."

            is SinCupo ->
                "Se alcanzó el máximo de $maximo usuarios registrados."

            is CorreoDuplicado ->
                "El correo $correo ya está registrado."
        }
}
