package cl.duoc.vozvisible.data

import androidx.compose.runtime.mutableStateListOf
import cl.duoc.vozvisible.util.normalizado

/**
 * Almacena en memoria el arreglo de usuarios de la aplicación.
 *
 * Es un `object`, es decir un singleton creado por el propio lenguaje: todas las
 * views acceden a la misma instancia sin necesidad de inyectarla ni de pasarla
 * como parámetro entre destinos de navegación.
 *
 * El arreglo se construye con `mutableStateListOf` y no con una lista común para
 * que Compose observe los cambios: al registrar un usuario, las views que leen la
 * colección se recomponen solas, sin ningún aviso explícito.
 */
object RepositorioUsuarios {

    /** Cupo máximo de usuarios que admite el arreglo. */
    const val MAX_USUARIOS = 10

    /**
     * Arreglo con los cinco usuarios exigidos por el enunciado, cargados con los
     * mismos campos que produce el formulario de la view de Registro.
     *
     * Se declara con `arrayOf` para dejar explícito que es un array de Kotlin y
     * se vuelca sobre la lista observable mediante el operador de propagación.
     */
    private val USUARIOS_INICIALES: Array<Usuario> = arrayOf(
        Usuario(
            nombre = "Camila Reyes",
            correo = "camila.reyes@duocuc.cl",
            password = "Camila2024",
            region = Region.METROPOLITANA,
            modoPreferido = ModoComunicacion.VOZ_A_TEXTO,
            preferencias = setOf(
                ApoyoAccesibilidad.SUBTITULOS_AUTOMATICOS,
                ApoyoAccesibilidad.ALERTAS_VIBRATORIAS
            )
        ),
        Usuario(
            nombre = "Matías Fuentes",
            correo = "matias.fuentes@duocuc.cl",
            password = "Matias2024",
            region = Region.VALPARAISO,
            modoPreferido = ModoComunicacion.TEXTO_A_VOZ,
            preferencias = setOf(ApoyoAccesibilidad.TEXTO_AMPLIADO)
        ),
        Usuario(
            nombre = "Valentina Soto",
            correo = "valentina.soto@duocuc.cl",
            password = "Valentina2024",
            region = Region.BIOBIO,
            modoPreferido = ModoComunicacion.AMBOS,
            preferencias = setOf(
                ApoyoAccesibilidad.ALTO_CONTRASTE,
                ApoyoAccesibilidad.TEXTO_AMPLIADO,
                ApoyoAccesibilidad.SUBTITULOS_AUTOMATICOS
            )
        ),
        Usuario(
            nombre = "Ignacio Márquez",
            correo = "ignacio.marquez@duocuc.cl",
            password = "Ignacio2024",
            region = Region.LA_ARAUCANIA,
            modoPreferido = ModoComunicacion.VOZ_A_TEXTO,
            preferencias = setOf(ApoyoAccesibilidad.ALERTAS_VIBRATORIAS)
        ),
        Usuario(
            nombre = "Fernanda Torres",
            correo = "fernanda.torres@duocuc.cl",
            password = "Fernanda2024",
            region = Region.METROPOLITANA,
            modoPreferido = ModoComunicacion.AMBOS,
            preferencias = setOf(
                ApoyoAccesibilidad.SUBTITULOS_AUTOMATICOS,
                ApoyoAccesibilidad.ALTO_CONTRASTE,
                ApoyoAccesibilidad.ALERTAS_VIBRATORIAS,
                ApoyoAccesibilidad.TEXTO_AMPLIADO
            )
        )
    )

    /** Cantidad de usuarios que trae el arreglo antes de cualquier registro nuevo. */
    val usuariosPrecargados: Int get() = USUARIOS_INICIALES.size

    private val usuarios = mutableStateListOf(*USUARIOS_INICIALES)

    /** Vista de solo lectura del arreglo: las views leen, pero no modifican. */
    val lista: List<Usuario> get() = usuarios

    /** Cantidad de usuarios almacenados. */
    val total: Int get() = usuarios.size

    /** Cupos que restan antes de alcanzar el máximo. */
    val cuposDisponibles: Int get() = MAX_USUARIOS - total

    fun hayCupo(): Boolean = cuposDisponibles > 0

    /** Determina si el correo ya pertenece a alguna cuenta del arreglo. */
    fun correoRegistrado(correo: String): Boolean =
        usuarios.any { usuario -> usuario.correspondeA(correo) }

    /**
     * Intenta agregar un usuario al arreglo.
     *
     * Devuelve un [ResultadoRegistro] en vez de un booleano o un mensaje suelto,
     * para que la view distinga el motivo exacto del rechazo.
     */
    fun registrar(usuario: Usuario): ResultadoRegistro = when {
        !hayCupo() -> ResultadoRegistro.SinCupo(MAX_USUARIOS)

        correoRegistrado(usuario.correo) ->
            ResultadoRegistro.CorreoDuplicado(usuario.correoNormalizado)

        else -> {
            usuarios.add(usuario)
            ResultadoRegistro.Exitoso(usuario = usuario, totalRegistrados = total)
        }
    }

    /**
     * Devuelve el usuario cuyas credenciales coinciden, o null si no hay ninguno.
     *
     * Entregar el objeto y no un booleano permite que la view de Login reutilice
     * el resultado, por ejemplo el nombre, sin volver a recorrer el arreglo.
     */
    fun autenticar(correo: String, password: String): Usuario? =
        usuarios.firstOrNull { usuario -> usuario.autenticaCon(correo, password) }

    /** Busca un usuario por correo; lo usan Recuperar contraseña e Inicio. */
    fun buscarPorCorreo(correo: String): Usuario? =
        usuarios.firstOrNull { usuario -> usuario.correspondeA(correo) }

    /**
     * Filtra el arreglo con el criterio que entregue quien llama.
     *
     * Es una función de orden superior: recibe otra función como parámetro, lo
     * que evita escribir un método distinto por cada filtro posible.
     *
     * Se declara inline para que el compilador copie el cuerpo de la lambda en
     * el lugar de la llamada, en vez de crear un objeto de función por cada
     * invocación. Por eso opera sobre la vista pública y no sobre el arreglo
     * privado: una función inline pública no puede acceder a miembros privados,
     * porque su cuerpo termina incrustado en código externo.
     */
    inline fun filtrar(criterio: (Usuario) -> Boolean): List<Usuario> = lista.filter(criterio)

    /**
     * Ordena el arreglo según la clave que devuelva el selector.
     *
     * Es genérica en el tipo de la clave, con la restricción de que sea
     * comparable: sirve igual para ordenar por nombre, por región o por la
     * cantidad de apoyos marcados. También es inline, por el mismo motivo.
     *
     * El selector lleva crossinline porque no se invoca directamente aquí, sino
     * que se entrega a sortedBy: eso impide que la lambda use un return que
     * salga de la función que la escribió.
     */
    inline fun <C : Comparable<C>> ordenadosPor(
        descendente: Boolean = false,
        crossinline selector: (Usuario) -> C
    ): List<Usuario> =
        if (descendente) lista.sortedByDescending(selector)
        else lista.sortedBy(selector)

    /** Usuarios de una región, ordenados alfabéticamente. */
    fun deRegion(region: Region): List<Usuario> =
        filtrar { usuario -> usuario.region == region }
            .sortedBy { usuario -> usuario.nombre }

    /** Usuarios que marcaron un apoyo determinado en la check list. */
    fun conApoyo(apoyo: ApoyoAccesibilidad): List<Usuario> =
        filtrar { usuario -> usuario.tieneApoyo(apoyo) }

    /**
     * Índice de correo a usuario.
     *
     * `associateBy` recorre la colección una sola vez y construye el mapa, en
     * lugar de repetir una búsqueda lineal por cada consulta.
     */
    fun indicePorCorreo(): Map<String, Usuario> =
        usuarios.associateBy { usuario -> usuario.correoNormalizado }

    /** Resumen agregado del arreglo, calculado con operaciones de colección. */
    fun estadisticas(): EstadisticasUsuarios = EstadisticasUsuarios.calcular(usuarios)

    /**
     * Restaura el arreglo a los cinco usuarios iniciales.
     *
     * Existe para dejar el estado en un punto conocido al probar la aplicación
     * y al ejecutar los tests unitarios.
     */
    fun restaurar() {
        usuarios.clear()
        usuarios.addAll(USUARIOS_INICIALES)
    }
}

/**
 * Extensión sobre cualquier colección de usuarios que busca por correo.
 *
 * Al declararla fuera del repositorio queda disponible también para listas ya
 * filtradas, sin obligar a volver al arreglo completo.
 */
fun Iterable<Usuario>.buscarCorreo(correo: String): Usuario? {
    val objetivo = correo.normalizado()
    return firstOrNull { usuario -> usuario.correoNormalizado == objetivo }
}
