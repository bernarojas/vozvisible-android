package cl.duoc.vozvisible.data

/**
 * Modos de comunicación excluyentes que el usuario elige con radio buttons.
 *
 * Cada constante lleva su propio texto y descripción, de modo que la view
 * recorre `entries` y no mantiene una lista paralela que pueda desincronizarse.
 *
 * @property titulo etiqueta del radio button.
 * @property descripcion texto de apoyo bajo la etiqueta.
 */
enum class ModoComunicacion(val titulo: String, val descripcion: String) {
    VOZ_A_TEXTO("Voz a texto", "Transcribe lo que otros dicen"),
    TEXTO_A_VOZ("Texto a voz", "Reproduce en voz alta lo que escribes"),
    AMBOS("Ambos modos", "Comunicación bidireccional completa");

    /** Indica si el modo contempla transcribir la voz del interlocutor. */
    val incluyeEscucha: Boolean get() = this != TEXTO_A_VOZ

    /** Indica si el modo contempla sintetizar voz a partir de texto escrito. */
    val incluyeHabla: Boolean get() = this != VOZ_A_TEXTO

    override fun toString(): String = titulo

    companion object {
        fun desdeTitulo(titulo: String): ModoComunicacion? =
            entries.firstOrNull { it.titulo.equals(titulo.trim(), ignoreCase = true) }
    }
}
