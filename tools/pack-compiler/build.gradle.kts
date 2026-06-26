import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

java {
    toolchain {
        // Match the rest of the build (gradle-daemon-jvm.properties -> JDK 21).
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlin.test)
    testImplementation(kotlin("test-junit"))
}

application {
    mainClass = "com.github.nols1000.nutaru.packcompiler.MainKt"
    applicationName = "pack-compiler"
}
