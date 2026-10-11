package cl.duoc.vozvisible.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
 * Antepone la cantidad a la palabra, en singular o plural según corresponda.
 *
 * Ejemplo: `1.enPlural("fragmento")` produce `"1 fragmento"`; con 3, `"3 fragmentos"`.
 */
fun Int.enPlural(singular: String, plural: String = singular + "s"): String =
    "$this ${if (this == 1) singular else plural}"

/** Formato de fecha y hora del historial, con el orden día-mes-año usado en Chile. */
private const val PATRON_FECHA_HORA = "dd-MM-yyyy HH:mm"

/**
 * Convierte una marca de tiempo en milisegundos a texto legible.
 *
 * Se crea un formateador por llamada porque SimpleDateFormat no es seguro entre
 * hilos: compartir una sola instancia podría mezclar fechas.
 */
fun Long.comoFechaHora(): String =
    SimpleDateFormat(PATRON_FECHA_HORA, Locale.forLanguageTag("es-CL")).format(Date(this))

/** Largo máximo de cada fragmento que se envía al motor de síntesis de voz. */
const val LARGO_FRAGMENTO_VOZ = 200

/**
 * Divide un texto en fragmentos para reproducirlo en voz alta.
 *
 * El motor de síntesis admite textos largos, pero entregarlos por partes tiene
 * una ventaja para quien no oye: la aplicación sabe qué fragmento está sonando
 * y puede mostrar en pantalla el avance ("fragmento 2 de 3"), que de otro modo
 * el usuario no tendría cómo percibir.
 *
 * Se corta preferentemente después de un signo de puntuación y, si no hay
 * ninguno dentro del límite, en el último espacio, para no partir palabras.
 *
 * @param largoMaximo tamaño máximo de cada fragmento.
 */
fun String.fragmentosParaVoz(largoMaximo: Int = LARGO_FRAGMENTO_VOZ): List<String> {
    require(largoMaximo > 0) { "El largo máximo debe ser positivo" }

    val fragmentos = mutableListOf<String>()
    var restante = trim()

    while (restante.length > largoMaximo) {
        val tramo = restante.substring(0, largoMaximo)

        // Se busca el último corte natural dentro del tramo. lastIndexOfAny
        // devuelve -1 cuando no encuentra ninguno de los caracteres.
        val finDeFrase = tramo.lastIndexOfAny(charArrayOf('.', '?', '!', ';', ':', ','))
        val ultimoEspacio = tramo.lastIndexOf(' ')

        val corte = when {
            finDeFrase > 0 -> finDeFrase + 1
            ultimoEspacio > 0 -> ultimoEspacio
            else -> largoMaximo // una sola palabra más larga que el límite
        }

        fragmentos += restante.substring(0, corte).trim()
        restante = restante.substring(corte).trim()
    }

    if (restante.isNotEmpty()) fragmentos += restante
    return fragmentos
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
