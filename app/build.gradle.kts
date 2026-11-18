import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}

// File .env locale (escluso dal versionamento): comodo per CI e per non scrivere
// i segreti su disco. Le stesse chiavi possono arrivare anche da variabili d'ambiente.
val envFile = rootProject.file(".env")
val envProperties = Properties().apply {
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            val separator = trimmed.indexOf('=')
            if (separator > 0) {
                val key = trimmed.substring(0, separator).trim()
                val value = trimmed.substring(separator + 1).trim().trim('"', '\'')
                setProperty(key, value)
            }
        }
    }
}

fun firstNonBlank(vararg values: String?): String =
    values.firstOrNull { !it.isNullOrBlank() }?.trim().orEmpty()

// Chiave Gemini personale per la build locale. Non va mai scritta nel codice sorgente:
// viene letta da secrets.properties, .env, variabile d'ambiente o -PGEMINI_API_KEY=...
// (in questo ordine) e inserita in BuildConfig per il primo avvio.
val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties().apply {
    if (secretsPropertiesFile.exists()) {
        FileInputStream(secretsPropertiesFile).use { load(it) }
    }
}
val geminiApiKey: String = firstNonBlank(
    secretsProperties.getProperty("GEMINI_API_KEY"),
    System.getenv("GEMINI_API_KEY"),
    envProperties.getProperty("GEMINI_API_KEY"),
    project.findProperty("GEMINI_API_KEY") as? String
)
val escapedGeminiApiKey = geminiApiKey.replace("\\", "\\\\").replace("\"", "\\\"")

// Firma release: keystore.properties resta la fonte principale, ma si possono usare
// anche le variabili d'ambiente o il .env per non committere keystore e password.
val releaseStoreFile = firstNonBlank(
    keystoreProperties.getProperty("storeFile"),
    System.getenv("KEYSTORE_FILE"),
    envProperties.getProperty("KEYSTORE_FILE")
)
val releaseStorePassword = firstNonBlank(
    keystoreProperties.getProperty("storePassword"),
    System.getenv("KEYSTORE_PASSWORD"),
    envProperties.getProperty("KEYSTORE_PASSWORD")
)
val releaseKeyAlias = firstNonBlank(
    keystoreProperties.getProperty("keyAlias"),
    System.getenv("KEYSTORE_KEY_ALIAS"),
    envProperties.getProperty("KEYSTORE_KEY_ALIAS")
)
val releaseKeyPassword = firstNonBlank(
    keystoreProperties.getProperty("keyPassword"),
    System.getenv("KEYSTORE_KEY_PASSWORD"),
    envProperties.getProperty("KEYSTORE_KEY_PASSWORD")
)
val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { it.isNotBlank() }

android {
    namespace = "com.sudokucat.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.sudokucat.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Vuota se non è stata fornita una chiave personale: in quel caso l'app
        // funziona comunque offline e la chiave si può inserire dalle Impostazioni.
        buildConfigField("String", "GEMINI_API_KEY", "\"$escapedGeminiApiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.9")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("androidx.work:work-runtime-ktx:2.10.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
