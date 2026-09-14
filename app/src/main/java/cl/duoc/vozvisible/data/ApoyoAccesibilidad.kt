package cl.duoc.vozvisible.data

/**
 * Apoyos de accesibilidad que el usuario marca en la check list de Registro.
 *
 * Al ser selección múltiple se guardan en un `Set`: la estructura descarta
 * duplicados por definición y la consulta de pertenencia es directa.
 *
 * @property titulo etiqueta del checkbox.
 * @property beneficio explicación breve del apoyo, usada en la vista de perfil.
 */
enum class ApoyoAccesibilidad(val titulo: String, val beneficio: String) {
    ALERTAS_VIBRATORIAS(
        "Alertas vibratorias",
        "Avisa con vibración cuando se detecta un sonido relevante"
    ),
    SUBTITULOS_AUTOMATICOS(
        "Subtítulos automáticos",
        "Muestra en pantalla lo que dice el interlocutor"
    ),
    ALTO_CONTRASTE(
        "Alto contraste",
        "Aumenta la diferencia de color entre texto y fondo"
    ),
    TEXTO_AMPLIADO(
        "Texto ampliado",
        "Incrementa el tamaño de la tipografía en toda la aplicación"
    );

    override fun toString(): String = titulo

    companion object {
        fun desdeTitulo(titulo: String): ApoyoAccesibilidad? =
            entries.firstOrNull { it.titulo.equals(titulo.trim(), ignoreCase = true) }
    }
}
