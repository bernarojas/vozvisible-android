package cl.duoc.vozvisible.data

import cl.duoc.vozvisible.util.aNombrePropio
import cl.duoc.vozvisible.util.correoEnmascarado
import cl.duoc.vozvisible.util.normalizado

/**
 * Representa a un usuario registrado en la aplicación.
 *
 * Es una `data class`: el compilador genera `equals`, `hashCode`, `toString`,
 * `copy` y los operadores de desestructuración, de modo que comparar usuarios o
 * derivar una copia con un campo distinto no requiere código manual.
 *
 * Los campos de selección usan enums en lugar de String para que el compilador
 * impida guardar un valor que la interfaz no ofrece.
 *
 * @property nombre nombre completo ingresado en la view de Registro.
 * @property correo correo electrónico; actúa como identificador único de acceso.
 * @property password contraseña de acceso asociada al correo.
 * @property region región de residencia, seleccionada desde el combo box.
 * @property modoPreferido modo de comunicación preferido, elegido con radio buttons.
 * @property preferencias apoyos de accesibilidad marcados en la check list.
 */
data class Usuario(
    val nombre: String,
    val correo: String,
    val password: String,
    val region: Region,
    val modoPreferido: ModoComunicacion,
    val preferencias: Set<ApoyoAccesibilidad> = emptySet()
) {

    /** Forma canónica del correo, usada en toda comparación de identidad. */
    val correoNormalizado: String get() = correo.normalizado()

    /** Dominio del correo, sin la parte de usuario. */
    val dominioCorreo: String get() = correoNormalizado.substringAfterLast('@')

    /** Correo parcialmente oculto, para mostrarlo sin exponerlo completo. */
    val correoVisible: String get() = correo.correoEnmascarado()

    /** Primer nombre, para saludar en la view de Inicio sin ocupar toda la línea. */
    val primerNombre: String get() = nombre.trim().substringBefore(' ')

    /** Iniciales del nombre y del primer apellido, para el avatar de la cabecera. */
    val iniciales: String
        get() = nombre.trim()
            .split(Regex("""\s+"""))
            .filter { palabra -> palabra.isNotBlank() }
            .take(2)
            .map { palabra -> palabra.first().uppercaseChar() }
            .joinToString(separator = "")
            .ifEmpty { "?" }

    /** Describe cuánto ha personalizado el usuario sus apoyos de accesibilidad. */
    val nivelPersonalizacion: String
        get() = when (preferencias.size) {
            0 -> "Sin apoyos configurados"
            1 -> "Personalización básica"
            in 2..3 -> "Personalización intermedia"
            else -> "Personalización completa"
        }

    /** Comprueba si el correo indicado identifica a este usuario. */
    fun correspondeA(correo: String): Boolean = correoNormalizado == correo.normalizado()

    /**
     * Valida las credenciales de acceso.
     *
     * El correo se compara sin distinguir mayúsculas; la contraseña, de forma
     * exacta, porque distinguir mayúsculas es parte de su seguridad.
     */
    fun autenticaCon(correo: String, password: String): Boolean =
        correspondeA(correo) && this.password == password

    /** Indica si el usuario marcó un apoyo determinado en la check list. */
    fun tieneApoyo(apoyo: ApoyoAccesibilidad): Boolean = apoyo in preferencias

    /** Lista los apoyos marcados en un solo texto, para la tabla de registrados. */
    fun apoyosComoTexto(vacio: String = "Sin apoyos"): String =
        preferencias
            .sortedBy { apoyo -> apoyo.ordinal }
            .joinToString(separator = ", ") { apoyo -> apoyo.titulo }
            .ifEmpty { vacio }

    companion object {

        /**
         * Construye un usuario a partir de los datos crudos del formulario,
         * aplicando la normalización antes de guardarlo en el arreglo.
         *
         * Concentrar la limpieza aquí evita que cada view decida por su cuenta
         * cómo recortar espacios o capitalizar el nombre.
         */
        fun desdeFormulario(
            nombre: String,
            correo: String,
            password: String,
            region: Region,
            modoPreferido: ModoComunicacion,
            preferencias: Collection<ApoyoAccesibilidad> = emptyList()
        ): Usuario = Usuario(
            nombre = nombre.aNombrePropio(),
            correo = correo.normalizado(),
            password = password,
            region = region,
            modoPreferido = modoPreferido,
            preferencias = preferencias.toSet()
        )
    }
}

/**
 * Resumen en una línea de las preferencias declaradas por el usuario.
 *
 * Es una propiedad de extensión: se declara fuera de la clase y el compilador
 * la resuelve como si fuera un miembro más, pero sin ocupar espacio en el
 * objeto ni formar parte del constructor. Se elige esta forma porque el
 * formato del texto es una decisión de presentación, no un dato del modelo:
 * puede cambiar sin tocar la data class ni las pruebas que la cubren.
 */
val Usuario.resumen: String
    get() = "${modoPreferido.titulo} · ${apoyosComoTexto()}"
