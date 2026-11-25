import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun resolveConfigValue(gradlePropKey: String, envKey: String): String? {
    val fromGradle = project.findProperty(gradlePropKey)?.toString()?.takeIf { it.isNotBlank() }
    if (fromGradle != null) return fromGradle
    val fromLocal = localProps.getProperty(gradlePropKey)?.takeIf { it.isNotBlank() }
    if (fromLocal != null) return fromLocal
    return System.getenv(envKey)?.takeIf { it.isNotBlank() }
}

val configuredVersionCode = resolveConfigValue("nothing.versionCode", "NOTHING_VERSION_CODE")?.toIntOrNull() ?: 1
val configuredVersionName = resolveConfigValue("nothing.versionName", "NOTHING_VERSION_NAME") ?: "1.0.0"

val releaseStoreFilePath = resolveConfigValue("nothing.release.storeFile", "KEYSTORE_PATH")
val releaseStorePassword = resolveConfigValue("nothing.release.storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = resolveConfigValue("nothing.release.keyAlias", "KEY_ALIAS")
val releaseKeyPassword = resolveConfigValue("nothing.release.keyPassword", "KEY_PASSWORD")
val releaseKeystoreFile = releaseStoreFilePath?.let { rootProject.file(it) }
val hasReleaseSigning = !releaseStoreFilePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    releaseKeystoreFile?.exists() == true

android {
    namespace = "com.nothing.island.ai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nothing.island.ai"
        minSdk = 29
        targetSdk = 36
        versionCode = configuredVersionCode
        versionName = configuredVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseKeystoreFile
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.lifecycle("Nothing Island: Release signing credentials/keystore not configured; release APK will be built unsigned.")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
            isDebuggable = true
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.media:media:1.7.0")

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Android Instrumentation Testing
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
