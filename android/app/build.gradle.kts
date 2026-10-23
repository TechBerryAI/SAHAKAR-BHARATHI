import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun loadProperties(vararg candidates: File): Properties {
    val properties = Properties()
    candidates.firstOrNull { it.exists() }?.inputStream()?.use { properties.load(it) }
    return properties
}

// Gitignored. local.properties may live in android/ or the repository root.
val localProperties = loadProperties(rootProject.file("local.properties"), rootProject.file("../local.properties"))
val keystoreProperties = loadProperties(rootProject.file("keystore.properties"))

// Resolution order: -P Gradle property, environment variable, local.properties.
fun setting(gradleProperty: String, envVar: String, localKey: String? = null, source: Properties = localProperties): String? {
    val fromGradle = providers.gradleProperty(gradleProperty).orNull
    val fromEnv = providers.environmentVariable(envVar).orNull
    val fromFile = localKey?.let { source.getProperty(it) }
    return listOf(fromGradle, fromEnv, fromFile).firstOrNull { !it.isNullOrBlank() }?.trim()
}

val releaseApiBaseUrl = setting("sahakar.apiBaseUrl", "SAHAKAR_API_BASE_URL", "api.base.url")
// Debug only: emulator default, or e.g. a LAN address for a physical phone.
val debugApiBaseUrl = setting("sahakar.debugApiBaseUrl", "SAHAKAR_DEBUG_API_BASE_URL", "debug.api.base.url")
    ?: "http://10.0.2.2:8000/"
// Compiled into release BuildConfig only when it passes validation; the
// validateReleaseConfig task below fails the build before anything is packaged otherwise.
val releaseApiBaseUrlOrPlaceholder = releaseApiBaseUrl ?: "https://api.invalid/"

val versionCodeSetting = setting("sahakar.versionCode", "SAHAKAR_VERSION_CODE")
val versionNameSetting = setting("sahakar.versionName", "SAHAKAR_VERSION_NAME")

val signingStoreFile = setting("sahakar.keystoreFile", "SAHAKAR_KEYSTORE_FILE", "storeFile", keystoreProperties)
val signingStorePassword = setting("sahakar.keystorePassword", "SAHAKAR_KEYSTORE_PASSWORD", "storePassword", keystoreProperties)
val signingKeyAlias = setting("sahakar.keyAlias", "SAHAKAR_KEY_ALIAS", "keyAlias", keystoreProperties)
val signingKeyPassword = setting("sahakar.keyPassword", "SAHAKAR_KEY_PASSWORD", "keyPassword", keystoreProperties)
val signingValues = listOf(signingStoreFile, signingStorePassword, signingKeyAlias, signingKeyPassword)
val releaseSigningConfigured = signingValues.all { it != null }
val releaseSigningPartial = !releaseSigningConfigured && signingValues.any { it != null }

android {
    namespace = "com.example.sahakarbharati"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.sahakarbharati"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeSetting?.toIntOrNull() ?: 1
        versionName = versionNameSetting ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile = rootProject.file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrlOrPlaceholder\"")
            if (releaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Returns a problem description, or null when the URL is safe to ship.
fun releaseApiUrlProblem(value: String?): String? {
    if (value.isNullOrBlank()) {
        return "is not set"
    }
    val uri = runCatching { URI(value) }.getOrNull() ?: return "is not a valid URL"
    val host = uri.host?.lowercase() ?: return "has no host"
    return when {
        uri.scheme != "https" -> "must use https://"
        host == "localhost" || host == "10.0.2.2" || host.startsWith("127.") || host == "0.0.0.0" ->
            "points at a local development server"
        host.endsWith(".invalid") || host.endsWith(".example") || host.endsWith(".test") ||
            host == "example.com" || host.endsWith(".example.com") -> "is a placeholder host"
        !value.endsWith("/") -> "must end with '/' (Retrofit base URLs require it)"
        !uri.query.isNullOrEmpty() || !uri.fragment.isNullOrEmpty() -> "must not contain a query or fragment"
        else -> null
    }
}

val releaseApiProblem = releaseApiUrlProblem(releaseApiBaseUrl)
val releaseApiShown = releaseApiBaseUrl.orEmpty()

val validateReleaseConfig = tasks.register("validateReleaseConfig") {
    group = "verification"
    description = "Fails release builds that would ship an invalid API URL or partial signing config."
    val apiProblem = releaseApiProblem
    val apiShown = releaseApiShown
    val partialSigning = releaseSigningPartial
    doLast {
        if (apiProblem != null) {
            throw GradleException(
                "Release API base URL $apiProblem (value: '$apiShown'). Set it with " +
                    "-Psahakar.apiBaseUrl=https://<api-host>/, the SAHAKAR_API_BASE_URL environment variable, " +
                    "or api.base.url in gitignored local.properties. See android/README.md.",
            )
        }
        if (partialSigning) {
            throw GradleException(
                "Release signing is partially configured. Provide all of SAHAKAR_KEYSTORE_FILE, " +
                    "SAHAKAR_KEYSTORE_PASSWORD, SAHAKAR_KEY_ALIAS and SAHAKAR_KEY_PASSWORD (or keystore.properties), " +
                    "or none of them to build an unsigned release.",
            )
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseConfig)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.androidx.security.crypto)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
