package cl.duoc.vozvisible.data

import cl.duoc.vozvisible.util.enPlural

/**
 * Cifras del historial de un usuario, para la cabecera de la view Historial.
 *
 * @property total cantidad de interacciones.
 * @property mensajesEscritos interacciones de Texto a voz.
 * @property transcripciones interacciones de Voz a texto.
 * @property favoritas interacciones marcadas como frase rápida.
 * @property palabrasComunicadas suma de las palabras de cada texto reutilizable.
 */
data class ResumenHistorial(
    val total: Int,
    val mensajesEscritos: Int,
    val transcripciones: Int,
    val favoritas: Int,
    val palabrasComunicadas: Int
) {

    val estaVacio: Boolean get() = total == 0

    /** Texto de una línea con las cifras principales. */
    val descripcion: String
        get() = if (estaVacio) "Todavía no tienes interacciones guardadas"
        else "$total en total · ${mensajesEscritos.enPlural("escrita")} · " +
            transcripciones.enPlural("escuchada")

    companion object {

        /**
         * Recorre el historial una sola vez acumulando todos los contadores.
         *
         * Se usa un ciclo `for` explícito y no varias llamadas a `count`, porque
         * cada una de ellas recorrería la lista completa de nuevo: con un solo
         * recorrido se obtienen las cinco cifras.
         */
        fun calcular(interacciones: List<Interaccion>): ResumenHistorial {
            var escritos = 0
            var transcritos = 0
            var favoritas = 0
            var palabras = 0

            for (interaccion in interacciones) {
                when (interaccion.tipo) {
                    TipoInteraccion.TEXTO_A_VOZ -> escritos++
                    TipoInteraccion.VOZ_A_TEXTO -> transcritos++
                }
                if (interaccion.favorita) favoritas++
                palabras += contarPalabras(interaccion.textoReutilizable)
            }

            return ResumenHistorial(
                total = interacciones.size,
                mensajesEscritos = escritos,
                transcripciones = transcritos,
                favoritas = favoritas,
                palabrasComunicadas = palabras
            )
        }

        /**
         * Cuenta las palabras recorriendo el texto carácter por carácter.
         *
         * Una palabra empieza cada vez que aparece una letra o dígito después de
         * un separador. Evita construir la lista intermedia que crearía `split`.
         */
        fun contarPalabras(texto: String): Int {
            var palabras = 0
            var dentroDePalabra = false
            var indice = 0

            while (indice < texto.length) {
                val esParteDePalabra = texto[indice].isLetterOrDigit()
                if (esParteDePalabra && !dentroDePalabra) palabras++
                dentroDePalabra = esParteDePalabra
                indice++
            }
            return palabras
        }
    }
}
