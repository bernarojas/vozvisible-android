# VozVisible

Aplicación móvil Android de accesibilidad para personas con discapacidad sensorial auditiva.
Facilita la comunicación cotidiana —escribir y hablar— convirtiendo el dispositivo móvil en
una herramienta que integra al usuario con su entorno real y digital.

## Contexto académico

| | |
|---|---|
| **Asignatura** | Desarrollo de Aplicaciones Móviles (DSY2204) |
| **Institución** | Duoc UC |
| **Entrega** | Sumativa 1 — Semana 2 |
| **Alcance** | Interfaz de usuario y navegación entre views |

## Estado de la entrega

Esta primera entrega implementa la capa de interfaz y la navegación. Las funciones de
transcripción y síntesis de voz corresponden a iteraciones posteriores; en esta versión
la pantalla de inicio las presenta como accesos aún no operativos.

### Views implementadas

| View | Descripción |
|---|---|
| **Login** | Acceso con validación de credenciales contra el arreglo de usuarios |
| **Registro** | Alta de usuarios con validación de formulario y tabla de registrados |
| **Recuperar contraseña** | Búsqueda de la cuenta y simulación de envío de instrucciones |
| **Inicio** | Grilla con las funciones de accesibilidad de la aplicación |

### Componentes de Material Design y Jetpack Compose

- `OutlinedTextField` — campos de entrada con validación e indicación de error
- `ExposedDropdownMenuBox` — combo box de selección de región
- `RadioButton` — selección excluyente del modo de comunicación preferido
- `Checkbox` — check list de apoyos de accesibilidad y aceptación de términos
- `LazyColumn` — tabla de usuarios registrados
- `LazyVerticalGrid` — grilla de funciones en la pantalla de inicio
- `Button`, `TextButton`, `Card`, `TopAppBar`, `Snackbar`, `Icon`

## Arquitectura

Se aplica el patrón *single-activity*: una única `Activity` que aloja todas las views como
composables gestionados por un `NavHost`.

```
app/src/main/java/cl/duoc/vozvisible/
├── MainActivity.kt              Activity única; monta el tema y el grafo
├── data/
│   ├── Usuario.kt               Modelo de datos del usuario
│   └── RepositorioUsuarios.kt   Arreglo observable de usuarios (máx. 5)
├── navigation/
│   ├── Rutas.kt                 Constantes y constructores de rutas
│   └── NavegacionApp.kt         Grafo de navegación
└── ui/
    ├── screens/                 Una view por archivo
    └── theme/                   Tema Material 3
```

### Decisiones de diseño

**State hoisting.** Las views no reciben el `NavController`: exponen funciones lambda y
notifican eventos hacia arriba. Esto las mantiene reutilizables y permite previsualizarlas
de forma aislada con `@Preview`.

**Estado observable.** El repositorio usa `mutableStateListOf` en lugar de una lista común,
de modo que Compose recompone automáticamente las views que leen el arreglo cuando se
registra un usuario nuevo.

**Navegación con argumentos.** El correo autenticado viaja como argumento de ruta desde
Login hacia Inicio, codificado con `Uri.encode` por contener caracteres reservados.

**Accesibilidad.** Siendo una aplicación dirigida a personas con discapacidad sensorial,
la interfaz aplica objetivos táctiles de al menos 56dp, descripciones de contenido en los
iconos para lectores de pantalla, roles semánticos en los controles de selección y áreas
seleccionables que abarcan la fila completa.

**Seguridad.** La view de recuperación nunca muestra la contraseña en pantalla, aunque
técnicamente esté disponible en memoria: confirma la existencia de la cuenta y simula el
envío de instrucciones por el canal elegido.

## Requisitos

- Android Studio (versión reciente con soporte de Kotlin DSL)
- JDK 11 o superior
- SDK de Android con plataforma de compilación 37
- Dispositivo o emulador con Android 7.0 (API 24) o superior

## Compilación y ejecución

Clonar el repositorio:

```
git clone https://github.com/bernarojas/vozvisible-android.git
```

Abrir la carpeta desde Android Studio y esperar la sincronización de Gradle. Luego ejecutar
con el botón *Run*, o desde la línea de comandos:

```
gradlew assembleDebug
```

Para instalar en un dispositivo o emulador conectado:

```
gradlew installDebug
```

## Configuración técnica

| | |
|---|---|
| Lenguaje | Kotlin |
| Interfaz | Jetpack Compose + Material 3 |
| `minSdk` | 24 (Android 7.0) |
| `targetSdk` / `compileSdk` | 37 |
| Navegación | `androidx.navigation:navigation-compose` |
| Build | Gradle con Kotlin DSL y catálogo de versiones |

## Limitaciones conocidas

Los usuarios se almacenan en un arreglo en memoria, según lo solicitado para esta entrega.
Los datos se pierden al cerrar la aplicación. La persistencia mediante Room o DataStore
está contemplada para una iteración posterior.

El cupo de registro está limitado a cinco usuarios.
