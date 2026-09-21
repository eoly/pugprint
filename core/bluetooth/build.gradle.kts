// :core:bluetooth — Android. The only module that touches Bluetooth APIs: Kable `BleTransport`,
// Companion Device Manager pairing, permission helpers. No protocol bytes (those stay in :core:printer).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.example.pugprint.bluetooth"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    // Built-in Kotlin (AGP 9) derives jvmTarget from targetCompatibility.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.useJUnitPlatform() }
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

dependencies {
    api(project(":core:printer"))

    implementation(libs.kable.core)
    implementation(libs.coroutines.android)
    implementation(libs.androidx.core.ktx)

    // JVM unit tests: JUnit 5 for the transport, JUnit 4 (via vintage) for Robolectric.
    testImplementation(libs.bundles.junit5)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit5.vintage.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit4)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.core)
}
