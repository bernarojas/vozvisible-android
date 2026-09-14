package cl.duoc.vozvisible.data

/**
 * Regiones disponibles en el combo box de la view de Registro.
 *
 * Se modelan como enum y no como cadenas sueltas por tres razones:
 * el compilador impide asignar un valor inexistente, el `when` sobre un enum
 * es exhaustivo y no necesita rama `else`, y la lista de opciones del combo
 * box se obtiene directamente de `entries` sin duplicar los textos.
 *
 * @property nombre etiqueta que se muestra en la interfaz.
 */
enum class Region(val nombre: String) {
    ARICA_Y_PARINACOTA("Arica y Parinacota"),
    ANTOFAGASTA("Antofagasta"),
    COQUIMBO("Coquimbo"),
    VALPARAISO("Valparaíso"),
    METROPOLITANA("Metropolitana"),
    MAULE("Maule"),
    BIOBIO("Biobío"),
    LA_ARAUCANIA("La Araucanía"),
    LOS_LAGOS("Los Lagos"),
    MAGALLANES("Magallanes");

    /** Zona geográfica, derivada con una expresión `when` sobre el propio enum. */
    val zona: String
        get() = when (this) {
            ARICA_Y_PARINACOTA, ANTOFAGASTA, COQUIMBO -> "Norte"
            VALPARAISO, METROPOLITANA, MAULE -> "Centro"
            BIOBIO, LA_ARAUCANIA, LOS_LAGOS, MAGALLANES -> "Sur"
        }

    override fun toString(): String = nombre

    companion object {
        /**
         * Recupera una región a partir de su etiqueta visible.
         *
         * @return la región coincidente, o null si el texto no corresponde a ninguna.
         */
        fun desdeNombre(nombre: String): Region? =
            entries.firstOrNull { it.nombre.equals(nombre.trim(), ignoreCase = true) }
    }
}
