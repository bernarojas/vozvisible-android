package cl.duoc.vozvisible.data

/**
 * Resumen agregado del arreglo de usuarios.
 *
 * Concentra en un solo lugar las operaciones de colección que antes habrían
 * quedado dispersas dentro de los composables. La view recibe el resultado ya
 * calculado y se limita a dibujarlo, lo que mantiene la interfaz libre de
 * lógica y permite probar el cálculo con tests unitarios.
 *
 * @property total cantidad de usuarios en el arreglo.
 * @property porRegion usuarios agrupados por región, de mayor a menor.
 * @property porZona usuarios agrupados por zona geográfica.
 * @property porModo cantidad de usuarios por modo de comunicación preferido.
 * @property apoyosMasSolicitados apoyos ordenados por cantidad de solicitudes.
 * @property promedioApoyos media de apoyos marcados por usuario.
 * @property dominiosCorreo dominios distintos presentes en los correos.
 * @property regionMayoritaria región con más usuarios, o null si el arreglo está vacío.
 */
data class EstadisticasUsuarios(
    val total: Int,
    val porRegion: Map<Region, Int>,
    val porZona: Map<String, Int>,
    val porModo: Map<ModoComunicacion, Int>,
    val apoyosMasSolicitados: List<Pair<ApoyoAccesibilidad, Int>>,
    val promedioApoyos: Double,
    val dominiosCorreo: Set<String>,
    val regionMayoritaria: Region?
) {

    /** Verdadero cuando no hay nada que resumir. */
    val estaVacio: Boolean get() = total == 0

    /** Promedio con un decimal, listo para mostrar en pantalla. */
    val promedioApoyosFormateado: String get() = "%.1f".format(promedioApoyos)

    /**
     * Porcentaje de usuarios que solicitó un apoyo determinado.
     *
     * Devuelve 0 cuando el arreglo está vacío, para no dividir por cero.
     */
    fun porcentajeDe(apoyo: ApoyoAccesibilidad): Int {
        if (estaVacio) return 0
        val solicitudes = apoyosMasSolicitados
            .firstOrNull { (candidato, _) -> candidato == apoyo }
            ?.second
            ?: 0
        return solicitudes * 100 / total
    }

    companion object {

        /**
         * Calcula el resumen recorriendo la colección con las operaciones
         * estándar de Kotlin.
         *
         * @param usuarios arreglo de usuarios a resumir.
         */
        fun calcular(usuarios: List<Usuario>): EstadisticasUsuarios {

            // groupingBy + eachCount cuenta en una sola pasada, sin construir
            // las listas intermedias que sí crearía groupBy.
            val porRegion = usuarios
                .groupingBy { usuario -> usuario.region }
                .eachCount()
                .toList()
                .sortedByDescending { (_, cantidad) -> cantidad }
                .toMap()

            val porZona = usuarios
                .groupingBy { usuario -> usuario.region.zona }
                .eachCount()

            // El mapa se inicializa con todos los modos en cero para que la
            // interfaz muestre siempre las tres barras, incluso las vacías.
            val porModo = ModoComunicacion.entries.associateWith { modo ->
                usuarios.count { usuario -> usuario.modoPreferido == modo }
            }

            // flatMap aplana los Set de preferencias de todos los usuarios en
            // una sola secuencia de apoyos, que luego se cuenta.
            val conteoApoyos = usuarios
                .flatMap { usuario -> usuario.preferencias }
                .groupingBy { apoyo -> apoyo }
                .eachCount()

            val apoyosMasSolicitados = ApoyoAccesibilidad.entries
                .map { apoyo -> apoyo to conteoApoyos.getOrDefault(apoyo, 0) }
                .sortedWith(
                    // A igual cantidad de solicitudes, se ordena por título para
                    // que el listado no cambie de orden entre recomposiciones.
                    compareByDescending<Pair<ApoyoAccesibilidad, Int>> { (_, cantidad) -> cantidad }
                        .thenBy { (apoyo, _) -> apoyo.titulo }
                )

            val promedioApoyos = usuarios
                .map { usuario -> usuario.preferencias.size }
                .average()
                .takeIf { promedio -> !promedio.isNaN() }
                ?: 0.0

            return EstadisticasUsuarios(
                total = usuarios.size,
                porRegion = porRegion,
                porZona = porZona,
                porModo = porModo,
                apoyosMasSolicitados = apoyosMasSolicitados,
                promedioApoyos = promedioApoyos,
                dominiosCorreo = usuarios.mapTo(mutableSetOf()) { it.dominioCorreo },
                regionMayoritaria = porRegion.keys.firstOrNull()
            )
        }
    }
}
