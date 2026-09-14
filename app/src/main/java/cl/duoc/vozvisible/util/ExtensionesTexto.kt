package cl.duoc.vozvisible.util

/**
 * Funciones de extensión sobre String usadas por las tres views de formulario.
 *
 * Antes cada view repetía su propia comprobación de correo; al declararlas como
 * extensiones se escriben una sola vez, se leen como si fueran métodos propios
 * del tipo (`correo.esCorreoValido()`) y quedan cubiertas por tests unitarios
 * sin necesidad de levantar la interfaz.
 */

/** Longitud mínima exigida a la contraseña en el formulario de Registro. */
const val LARGO_MINIMO_PASSWORD = 6

/** Quita espacios sobrantes y pasa a minúsculas: forma canónica de un correo. */
fun String.normalizado(): String = trim().lowercase()

/**
 * Comprueba que el texto tenga la forma `usuario@dominio.tld`.
 *
 * No pretende cubrir el estándar completo de direcciones de correo: valida lo
 * necesario para dar retroalimentación inmediata en el formulario.
 */
fun String.esCorreoValido(): Boolean {
    val texto = normalizado()
    if (texto.count { it == '@' } != 1) return false

    val (usuario, dominio) = texto.split("@")
    return usuario.isNotBlank() &&
        dominio.contains('.') &&
        !dominio.startsWith('.') &&
        !dominio.endsWith('.') &&
        dominio.substringAfterLast('.').length >= 2
}

/** Una contraseña es aceptable si alcanza el largo mínimo y no es solo espacios. */
fun String.esPasswordValida(): Boolean =
    isNotBlank() && length >= LARGO_MINIMO_PASSWORD

/**
 * Clasifica la robustez de la contraseña para mostrarla como texto de apoyo.
 *
 * Suma un punto por cada característica presente y traduce el total con un
 * `when` sobre rangos.
 */
fun String.fortalezaPassword(): String {
    if (isBlank()) return ""

    val puntos = listOf(
        length >= LARGO_MINIMO_PASSWORD,
        length >= 10,
        any { it.isDigit() },
        any { it.isUpperCase() },
        any { !it.isLetterOrDigit() }
    ).count { cumple -> cumple }

    return when (puntos) {
        in 0..1 -> "Contraseña débil"
        in 2..3 -> "Contraseña aceptable"
        else -> "Contraseña robusta"
    }
}

/**
 * Normaliza un nombre propio: recorta espacios repetidos y deja cada palabra
 * con la inicial en mayúscula.
 *
 * Ejemplo: `"  ana   maría SOTO "` produce `"Ana María Soto"`.
 */
fun String.aNombrePropio(): String =
    trim()
        .split(Regex("""\s+"""))
        .filter { palabra -> palabra.isNotEmpty() }
        .joinToString(" ") { palabra ->
            palabra.lowercase().replaceFirstChar { inicial -> inicial.uppercase() }
        }

/**
 * Oculta parcialmente un correo para mostrarlo sin exponerlo por completo.
 *
 * Ejemplo: `"camila.reyes@duocuc.cl"` produce `"ca••••••••••@duocuc.cl"`.
 */
fun String.correoEnmascarado(): String {
    if (!esCorreoValido()) return this

    val texto = normalizado()
    val usuario = texto.substringBefore('@')
    val dominio = texto.substringAfter('@')
    val visibles = usuario.take(2)

    return visibles + "•".repeat(maxOf(usuario.length - visibles.length, 1)) + "@" + dominio
}
