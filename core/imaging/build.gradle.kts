// :core:imaging — PURE Kotlin/JVM. Dithering, scaling to 384 px, 1bpp packing. No Android imports.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

dependencies {
    testImplementation(libs.bundles.junit5)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Alias so the documented `./gradlew testDebugUnitTest` also covers this JVM module.
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Runs this module's JVM tests (alias of `test` for parity with Android modules)."
    dependsOn(tasks.test)
}
