package cl.duoc.vozvisible.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.vozvisible.data.SesionUsuario
import cl.duoc.vozvisible.ui.screens.InicioScreen
import cl.duoc.vozvisible.ui.screens.LoginScreen
import cl.duoc.vozvisible.ui.screens.RecuperarPasswordScreen
import cl.duoc.vozvisible.ui.screens.RegistroScreen

/**
 * Grafo de navegación de la aplicación.
 *
 * Cada view se declara como un destino del NavHost. Las views no conocen el
 * NavController: reciben funciones lambda y solo avisan "ocurrió tal evento".
 * Esto las mantiene reutilizables y permite previsualizarlas de forma aislada.
 *
 * El destino inicial depende de la sesión guardada en SharedPreferences: si
 * quedó una abierta, la aplicación entra directamente a Inicio y se salta el
 * formulario de acceso.
 */
@Composable
fun NavegacionApp() {
    val navController = rememberNavController()
    val contexto = LocalContext.current

    // remember con el contexto como clave: la sesión se abre una sola vez y no
    // se vuelve a leer de disco en cada recomposición.
    val sesion = remember(contexto) { SesionUsuario(contexto) }

    val destinoInicial = sesion.correo
        ?.takeIf { correo -> correo.isNotBlank() }
        ?.let { correo -> Rutas.inicioDe(correo) }
        ?: Rutas.LOGIN

    NavHost(
        navController = navController,
        startDestination = destinoInicial
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = { usuario ->
                    // La sesión se guarda antes de navegar: si la aplicación se
                    // cierra en Inicio, al reabrirla vuelve ahí directamente.
                    sesion.abrir(usuario)
                    navController.navigate(Rutas.inicioDe(usuario.correoNormalizado)) {
                        // Elimina Login del historial para que el botón Atrás
                        // no devuelva al usuario a la pantalla de acceso.
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                },
                onIrARegistro = { navController.navigate(Rutas.REGISTRO) },
                onIrARecuperar = { navController.navigate(Rutas.RECUPERAR) }
            )
        }

        composable(Rutas.REGISTRO) {
            RegistroScreen(onVolver = { navController.popBackStack() })
        }

        composable(Rutas.RECUPERAR) {
            RecuperarPasswordScreen(onVolver = { navController.popBackStack() })
        }

        composable(
            route = Rutas.INICIO,
            // Se declara el tipo del argumento para que Navigation lo valide.
            arguments = listOf(navArgument(Rutas.ARG_CORREO) { type = NavType.StringType })
        ) { backStackEntry ->
            val correo = backStackEntry.arguments?.getString(Rutas.ARG_CORREO).orEmpty()
            InicioScreen(
                correoUsuario = correo,
                onCerrarSesion = {
                    sesion.cerrar()
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.INICIO) { inclusive = true }
                    }
                }
            )
        }
    }
}
