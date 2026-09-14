# VozVisible

Aplicación móvil Android de accesibilidad para personas con discapacidad sensorial auditiva.
Facilita la comunicación cotidiana —escribir y hablar— convirtiendo el dispositivo móvil en
una herramienta que integra al usuario con su entorno real y digital.

## Contexto académico

| | |
|---|---|
| **Asignatura** | Desarrollo de Aplicaciones Móviles (DSY2204) |
| **Institución** | Duoc UC |
| **Entrega** | Sumativa 2 — Semana 5 |
| **Alcance** | Integración del lenguaje Kotlin: funciones, colecciones y arreglo de usuarios |

## Estado de la entrega

La primera entrega implementó la capa de interfaz y la navegación. Esta segunda entrega
adapta esa base al lenguaje Kotlin: la lógica de negocio sale de los composables hacia un
modelo de dominio propio, los datos de selección pasan de cadenas de texto a enums, los
desenlaces se modelan con `sealed interface` y el arreglo de usuarios llega precargado.

Las funciones de transcripción y síntesis de voz corresponden a iteraciones posteriores;
la pantalla de inicio las presenta como accesos aún no operativos.

### Views implementadas

| View | Descripción |
|---|---|
| **Login** | Acceso con validación de credenciales contra el arreglo de usuarios |
| **Registro** | Alta de usuarios con validación de formulario y tabla de registrados |
| **Recuperar contraseña** | Búsqueda de la cuenta y simulación de envío de instrucciones |
| **Inicio** | Resumen de la comunidad y grilla de funciones priorizada por preferencia |

### Componentes de Material Design y Jetpack Compose

- `OutlinedTextField` — campos de entrada con validación e indicación de error
- `ExposedDropdownMenuBox` — combo box de selección de región
- `RadioButton` — selección excluyente del modo de comunicación preferido
- `Checkbox` — check list de apoyos de accesibilidad y aceptación de términos
- `LazyColumn` — tabla de usuarios registrados
- `LazyVerticalGrid` — grilla de funciones en la pantalla de inicio
- `LinearProgressIndicator` — barras del resumen de apoyos más solicitados
- `Button`, `TextButton`, `Card`, `TopAppBar`, `Snackbar`, `Surface`, `Icon`

## Arquitectura

Se aplica el patrón *single-activity*: una única `Activity` que aloja todas las views como
composables gestionados por un `NavHost`.

```
app/src/main/java/cl/duoc/vozvisible/
├── MainActivity.kt                Activity única; monta el tema y el grafo
├── data/
│   ├── Usuario.kt                 Modelo de usuario con propiedades calculadas
│   ├── Region.kt                  Enum de regiones, con su zona geográfica
│   ├── ModoComunicacion.kt        Enum del modo preferido de comunicación
│   ├── ApoyoAccesibilidad.kt      Enum de los apoyos de la check list
│   ├── ResultadoRegistro.kt       Sealed interface con el desenlace del registro
│   ├── RepositorioUsuarios.kt     Arreglo observable de usuarios y sus consultas
│   └── EstadisticasUsuarios.kt    Resumen agregado del arreglo
├── util/
│   └── ExtensionesTexto.kt        Funciones de extensión de validación y formato
├── navigation/
│   ├── Rutas.kt                   Constantes y constructores de rutas
│   └── NavegacionApp.kt           Grafo de navegación
└── ui/
    ├── screens/                   Una view por archivo
    └── theme/                     Tema Material 3
```

## Integración del lenguaje Kotlin

### Arreglo de usuarios

`RepositorioUsuarios` declara con `arrayOf` un arreglo con los cinco usuarios exigidos por
la actividad, con los mismos campos que produce el formulario de Registro, y lo vuelca sobre
una lista observable con el operador de propagación. El cupo total es de diez, de modo que
la view de Registro sigue siendo operativa sobre el arreglo precargado.

Credenciales de los usuarios precargados:

| Correo | Contraseña |
|---|---|
| `camila.reyes@duocuc.cl` | `Camila2024` |
| `matias.fuentes@duocuc.cl` | `Matias2024` |
| `valentina.soto@duocuc.cl` | `Valentina2024` |
| `ignacio.marquez@duocuc.cl` | `Ignacio2024` |
| `fernanda.torres@duocuc.cl` | `Fernanda2024` |

### Elementos del lenguaje aplicados

| Elemento | Dónde se usa |
|---|---|
| `enum class` con propiedades y `companion object` | `Region`, `ModoComunicacion`, `ApoyoAccesibilidad` |
| `sealed interface` y `when` exhaustivo | `ResultadoRegistro`, `ResultadoAcceso`, `ResultadoRecuperacion` |
| `data class` con `copy` y desestructuración | `Usuario`, `EstadisticasUsuarios`, `FuncionApp` |
| Propiedades calculadas (`get()`) | `Usuario.iniciales`, `Region.zona`, `RepositorioUsuarios.cuposDisponibles` |
| Funciones de extensión | `String.esCorreoValido()`, `Iterable<Usuario>.buscarCorreo()` |
| Funciones de orden superior y genéricas | `RepositorioUsuarios.filtrar()`, `ordenadosPor()` |
| Parámetros con nombre y valor por defecto | `Usuario.desdeFormulario()`, `apoyosComoTexto()` |
| Null-safety: `?.`, `?:`, `let`, smart cast | Validación de acceso y resolución del usuario en Inicio |
| Colecciones: `filter`, `map`, `flatMap`, `groupingBy`, `associateBy`, `sortedWith`, `average`, `take` | `EstadisticasUsuarios.calcular()`, `RepositorioUsuarios` |

### Decisiones de diseño

**State hoisting.** Las views no reciben el `NavController`: exponen funciones lambda y
notifican eventos hacia arriba. Esto las mantiene reutilizables y permite previsualizarlas
de forma aislada con `@Preview`.

**Estado observable.** El repositorio usa `mutableStateListOf` en lugar de una lista común,
de modo que Compose recompone automáticamente las views que leen el arreglo cuando se
registra un usuario nuevo.

**Selecciones como enums.** Región, modo de comunicación y apoyos dejaron de ser cadenas
sueltas. El compilador impide guardar un valor que la interfaz no ofrece, el `when` sobre
un enum no necesita rama `else`, y las opciones de cada selector se recorren desde
`entries` en lugar de mantener una lista paralela que pueda desincronizarse.

**Desenlaces tipados.** Registrar, acceder y recuperar la contraseña devuelven un
`sealed interface` en vez de un `String?`. Cada desenlace transporta sus propios datos y el
compilador avisa si se agrega un caso nuevo sin tratarlo.

**Lógica fuera de la interfaz.** Validaciones, consultas y estadísticas viven en `data/` y
`util/`, no dentro de los composables. Por eso pueden probarse en la JVM sin emulador.

**Navegación con argumentos.** El correo autenticado viaja como argumento de ruta desde
Login hacia Inicio, codificado con `Uri.encode` por contener caracteres reservados.

**Accesibilidad.** Siendo una aplicación dirigida a personas con discapacidad sensorial,
la interfaz aplica objetivos táctiles de al menos 56dp, descripciones de contenido en los
iconos para lectores de pantalla, roles semánticos en los controles de selección y áreas
seleccionables que abarcan la fila completa.

**Seguridad.** La view de recuperación nunca muestra la contraseña en pantalla, aunque
técnicamente esté disponible en memoria: confirma la existencia de la cuenta y simula el
envío de instrucciones por el canal elegido. La tabla de registrados y el mensaje de
recuperación muestran el correo enmascarado.

## Pruebas

La lógica de dominio está cubierta por 36 tests unitarios que se ejecutan en la JVM:

```
app/src/test/java/cl/duoc/vozvisible/
├── data/
│   ├── UsuarioTest.kt                Propiedades calculadas y autenticación
│   ├── RepositorioUsuariosTest.kt    Arreglo, cupo, duplicados y consultas
│   └── EstadisticasUsuariosTest.kt   Agrupaciones, promedios y porcentajes
└── util/
    └── ExtensionesTextoTest.kt       Validación y formato de texto
```

Ejecutarlas con:

```
gradlew testDebugUnitTest
```

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
| Pruebas | JUnit 4 |
| Build | Gradle con Kotlin DSL y catálogo de versiones |

## Limitaciones conocidas

Los usuarios se almacenan en un arreglo en memoria, según lo solicitado para esta entrega.
Los datos se pierden al cerrar la aplicación y las contraseñas se guardan en texto plano.
La persistencia mediante Room o DataStore, y el cifrado de credenciales, están
contemplados para una iteración posterior.

El cupo del arreglo está limitado a diez usuarios.
