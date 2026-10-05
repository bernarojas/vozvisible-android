import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// Credenciales de firma. Viven fuera del control de versiones, de modo que la
// clave privada no viaja en el repositorio. Si el archivo no está, la
// compilación de release sigue funcionando y produce un APK sin firmar.
val archivoFirma = rootProject.file("keystore.properties")
val datosFirma = Properties().apply {
    if (archivoFirma.exists()) archivoFirma.inputStream().use { load(it) }
}
val hayFirma = archivoFirma.exists()

android {
    namespace = "cl.duoc.vozvisible"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cl.duoc.vozvisible"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hayFirma) {
            create("release") {
                storeFile = rootProject.file(datosFirma.getProperty("storeFile"))
                storePassword = datosFirma.getProperty("storePassword")
                keyAlias = datosFirma.getProperty("keyAlias")
                keyPassword = datosFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Sin las credenciales, Gradle deja el APK sin firmar en lugar de
            // fallar: así el proyecto compila en cualquier equipo.
            if (hayFirma) signingConfig = signingConfigs.getByName("release")

            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}