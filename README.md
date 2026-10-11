# VozVisible

Aplicación móvil Android de accesibilidad para personas con discapacidad sensorial auditiva.
Facilita la comunicación cotidiana (escribir y hablar) convirtiendo el dispositivo móvil en
una herramienta que integra al usuario con su entorno real y digital.

## Contexto académico

| | |
|---|---|
| **Asignatura** | Desarrollo de Aplicaciones Móviles (DSY2204) |
| **Institución** | Duoc UC |
| **Entrega** | Evaluación final transversal (Semana 9) |
| **Alcance** | Aplicación completa: escribir y hablar, historial, ayuda, Firebase y APK firmado |

## Estado de la entrega

La primera entrega construyó la interfaz y la navegación. La segunda adaptó esa base al
lenguaje Kotlin, trasladando la lógica de negocio desde los composables hacia un modelo de
dominio propio. La tercera incorporó el back end: Firestore, Authentication, sesión con
SharedPreferences, CRUD de usuarios y el APK firmado.

Esta entrega final completa el flujo de comunicación que da sentido a la aplicación:

1. El usuario ingresa con sus credenciales y la aplicación lo saluda por su nombre.
2. En el menú principal elige **escribir** (Texto a voz) o **hablar** (Voz a texto).
3. La aplicación responde: reproduce en voz alta lo escrito, o transcribe lo que dice el
   interlocutor, guiando cada paso con indicaciones visuales.
4. Cada solicitud y su respuesta se guardan en el historial, en Cloud Firestore.
5. El usuario vuelve al menú principal.

- **Texto a voz** con `TextToSpeech`, en español de Chile, con avance visual por fragmentos.
- **Voz a texto** con `SpeechRecognizer`, con subtítulos en vivo e intensidad de la voz.
- **Historial** con su propio CRUD: crear, consultar, marcar como favorita y eliminar.
- **Frases rápidas**: las favoritas del historial se suman a frases predeterminadas.
- **Ayuda**: tutorial de seis pasos, ofrecido en Inicio hasta que se recorre una vez.

### Configuración necesaria para compilar

El repositorio incluye `app/google-services.json`, así que el proyecto compila al clonarlo.
La firma del APK requiere un `keystore.properties` en la raíz, que **no se versiona**: sin
él, `assembleRelease` produce un paquete sin firmar en lugar de fallar.

El historial vive en la colección `interacciones`. Las reglas de Firestore deben cubrirla
con el mismo criterio que la colección `usuarios`, es decir, acceso solo con sesión iniciada:

```
match /interacciones/{id} {
  allow read, write: if request.auth != null;
}
```

### Views implementadas

| View | Descripción |
|---|---|
| **Login** | Acceso contra Firebase Authentication |
| **Registro** | Alta de la credencial y del perfil, con validación y tabla de registrados |
| **Recuperar contraseña** | Envío del correo de restablecimiento por Firebase |
| **Inicio** | Saludo, resumen de la comunidad y menú de funciones priorizado por preferencia |
| **Texto a voz** | El usuario escribe o elige una frase rápida y la aplicación la dice en voz alta |
| **Voz a texto** | La aplicación escucha al interlocutor y muestra la transcripción en pantalla |
| **Historial** | Solicitudes y respuestas guardadas, con filtros, favoritas y borrado |
| **Ayuda** | Tutorial de uso en páginas deslizables |
| **Perfil** | Modificación y eliminación de la cuenta |

### Componentes de Material Design y Jetpack Compose

- `OutlinedTextField`: campos de entrada con validación e indicación de error
- `ExposedDropdownMenuBox`: combo box de selección de región
- `RadioButton`: selección excluyente del modo de comunicación preferido
- `Checkbox`: check list de apoyos de accesibilidad y aceptación de términos
- `LazyColumn`: tabla de usuarios registrados
- `LazyVerticalGrid`: grilla de funciones en la pantalla de inicio
- `LinearProgressIndicator`: barras del resumen de apoyos más solicitados
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
│   ├── ResultadoRegistro.kt       Desenlace del alta
│   ├── ResultadoEdicion.kt        Desenlace de modificación y borrado
│   ├── AutenticacionFirebase.kt   Envoltura de Firebase Authentication
│   ├── FuenteUsuarios.kt          Contrato del almacén remoto
│   ├── FuenteFirestore.kt         Implementación sobre Cloud Firestore
│   ├── SesionUsuario.kt           Sesión en SharedPreferences
│   ├── RepositorioUsuarios.kt     Arreglo observable y escritura remota
│   ├── EstadisticasUsuarios.kt    Resumen agregado del arreglo
│   ├── TipoInteraccion.kt         Enum de las dos formas de comunicarse
│   ├── Interaccion.kt             Solicitud y respuesta guardadas en el historial
│   ├── ResumenHistorial.kt        Cifras del historial, en un solo recorrido
│   ├── FuenteInteracciones.kt     Contrato del almacén del historial
│   ├── FirestoreInteracciones.kt  Implementación sobre Cloud Firestore
│   └── RepositorioInteracciones.kt Historial observable y escritura remota
├── voz/
│   ├── MotorComunicacion.kt       Clase abstracta base y estados del motor
│   ├── MotorTextoAVoz.kt          Síntesis de voz con TextToSpeech
│   └── MotorVozATexto.kt          Reconocimiento de voz con SpeechRecognizer
├── util/
│   └── ExtensionesTexto.kt        Funciones de extensión de validación y formato
├── navigation/
│   ├── Rutas.kt                   Constantes y constructores de rutas
│   └── NavegacionApp.kt           Grafo de navegación
└── ui/
    ├── componentes/               Selectores compartidos por Registro y Perfil
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
| Propiedades de extensión | `Usuario.resumen`, declarada fuera de la data class |
| Funciones de orden superior, genéricas e `inline` | `RepositorioUsuarios.filtrar()`, `ordenadosPor()` con `crossinline` |
| Parámetros con nombre y valor por defecto | `Usuario.desdeFormulario()`, `apoyosComoTexto()` |
| Null-safety: `?.`, `?:`, `let`, smart cast | Validación de acceso y resolución del usuario en Inicio |
| Colecciones: `filter`, `map`, `flatMap`, `groupingBy`, `associateBy`, `sortedWith`, `average`, `take` | `EstadisticasUsuarios.calcular()`, `RepositorioUsuarios` |
| Clase abstracta, herencia, `open`, `override` y `super` | `MotorComunicacion` y sus subclases `MotorTextoAVoz`, `MotorVozATexto` |
| Implementación de interfaces del sistema | `TextToSpeech.OnInitListener`, `RecognitionListener` |
| Modificadores `private`, `protected` e `internal` | `MotorComunicacion.contexto`, `estado` con `protected set`, `RepositorioInteracciones.restaurar()` |
| Ciclos `for` y `while` | `ResumenHistorial.calcular()`, `contarPalabras()`, `String.fragmentosParaVoz()` |
| `try` / `catch` / `finally` | `RepositorioInteracciones.cargar()` y `escribirEnServidor()`, estados de espera en las views |
| Lambdas guardadas en un enum | `FiltroHistorial` en la view Historial |

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

**Retroalimentación solo visual.** Quien no oye no sabe si el teléfono terminó de hablar ni
si está captando la voz. Texto a voz entrega el mensaje por fragmentos y muestra el avance
en una barra; Voz a texto muestra la intensidad del sonido y los subtítulos parciales
mientras la persona habla.

**Escrituras que no cuelgan la interfaz.** Firestore no confirma una escritura hecha sin
conexión hasta que la red vuelve. El historial espera la confirmación como máximo cinco
segundos y distingue si la escritura quedó en cola o si el servidor la rechazó, para no
prometer una sincronización que no ocurrirá.

**Seguridad.** Las contraseñas las administra Firebase Authentication: la aplicación no
las conoce ni las almacena. La tabla de registrados y el mensaje de recuperación muestran
el correo enmascarado.

## Pruebas

La lógica de dominio está cubierta por 75 tests unitarios que se ejecutan en la JVM, con
JUnit y Mockito:

```
app/src/test/java/cl/duoc/vozvisible/
├── data/
│   ├── UsuarioTest.kt                   Propiedades calculadas y autenticación
│   ├── RepositorioUsuariosTest.kt       Arreglo, cupo, duplicados y consultas
│   ├── RepositorioRemotoTest.kt         Escritura remota verificada con Mockito
│   ├── EstadisticasUsuariosTest.kt      Agrupaciones, promedios y porcentajes
│   ├── RepositorioInteraccionesTest.kt  CRUD del historial, plazo y rechazo del servidor
│   └── ResumenHistorialTest.kt          Conteos del historial y de palabras
├── util/
│   └── ExtensionesTextoTest.kt          Validación, formato y fragmentación de texto
└── voz/
    └── MotoresTest.kt                   Identificadores, intensidad y mensajes de error
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
| Back end | Firebase Authentication y Cloud Firestore |
| Voz | `TextToSpeech` y `SpeechRecognizer` del sistema |
| Pruebas | JUnit 4 y Mockito |
| Build | Gradle con Kotlin DSL y catálogo de versiones |

## Limitaciones conocidas

El reconocimiento y la síntesis de voz usan los servicios instalados en el dispositivo. Voz
a texto necesita el servicio de reconocimiento de Google y conexión a internet; Texto a voz
necesita una voz en español instalada.

La detección de sonidos del entorno para generar alertas visuales queda fuera del alcance.

El cupo del arreglo de usuarios está limitado a diez.
