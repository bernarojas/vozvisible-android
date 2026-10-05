package cl.duoc.vozvisible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import cl.duoc.vozvisible.data.FuenteFirestore
import cl.duoc.vozvisible.data.RepositorioUsuarios
import cl.duoc.vozvisible.navigation.NavegacionApp
import cl.duoc.vozvisible.ui.theme.VozVisibleTheme

/**
 * Unica Activity de la aplicacion.
 *
 * Se aplica el patron single-activity: todas las views son composables
 * gestionados por el NavHost, no Activities independientes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // El almacen remoto se asocia una sola vez, al crear la Activity. El
        // repositorio queda igual de utilizable sin el, que es como corren las
        // pruebas unitarias.
        RepositorioUsuarios.conectar(FuenteFirestore())

        enableEdgeToEdge()
        setContent {
            VozVisibleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavegacionApp()
                }
            }
        }
    }
}
