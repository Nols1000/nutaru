import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }

    js {
        outputModuleName = "sharedLogic"
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
            optIn.add("kotlin.js.ExperimentalJsExport")
        }
    }

    androidLibrary {
       namespace = "com.github.nols1000.nutaru.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()

       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    sourceSets {
        commonMain.dependencies {
            // SQLDelight runtime is added by the sqldelight gradle plugin.
            implementation(libs.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.androidDriver)
            implementation(libs.sqlcipher.android)
            implementation(libs.bouncycastle)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.nativeDriver)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqliteDriver)
                implementation(libs.bouncycastle)
            }
        }
    }
}

sqldelight {
    databases {
        create("NutaruDatabase") {
            packageName.set("com.github.nols1000.nutaru.db")
            version = 6
            dialect(libs.sqldelight.dialect)
        }
    }
}
