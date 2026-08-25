package cl.duoc.vozvisible.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
 */
@Composable
fun NavegacionApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                onLoginExitoso = { correo ->
                    navController.navigate(Rutas.inicioDe(correo)) {
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
                    navController.navigate(Rutas.LOGIN) {
                        popUpTo(Rutas.INICIO) { inclusive = true }
                    }
                }
            )
        }
    }
}
