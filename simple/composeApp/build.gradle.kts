import com.android.build.api.dsl.ManagedVirtualDevice
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.application)

}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                    freeCompilerArgs.add("-Xjdk-release=${JavaVersion.VERSION_1_8}")
                }
            }
        }

    }

//    jvm()
//
//    wasmJs {
//        browser()
//        binaries.executable()
//    }


    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries {
            framework {
                baseName = "ComposeApp"
                isStatic = true

            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.jetbrains.material3)
            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)

            implementation(project(":kmmOpenId"))
//            implementation(libs.kmm.crypto)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.auth)

            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.koin.androidx.compose)



        }

//        commonTest.dependencies {
//            implementation(kotlin("test"))
//            @OptIn(ExperimentalComposeLibrary::class)
//            implementation(compose.uiTest)
//        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.ktor.client.okhttp)

        }

//        jvmMain.dependencies {
//            implementation(compose.desktop.currentOs)
//        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)

        }

    }
}


android {
    signingConfigs {
        getByName("debug") {
            storeFile =
                file("/Users/michelleraouf/Desktop/kmm/kotlin-openid/simple/composeApp/src/androidMain/key")
            storePassword = "key-pass"
            keyPassword = "key-pass"
            keyAlias = "key0"
        }
    }
    namespace = "io.github.sample"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36

        applicationId = "io.github.sample.androidApp"
        versionCode = 1
        versionName = "1.0.0"
        addManifestPlaceholders(
            mapOf("appAuthRedirectScheme" to "com.duendesoftware.demo")
        )
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        res.srcDirs("src/androidMain/res")
    }
    //https://developer.android.com/studio/test/gradle-managed-devices
    @Suppress("UnstableApiUsage")
    testOptions {
        managedDevices.devices {
            maybeCreate<ManagedVirtualDevice>("pixel5").apply {
                device = "Pixel 5"
                apiLevel = 34
                systemImageSource = "aosp"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        //enables a Compose tooling support in the AndroidStudio
        compose = true
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.sample.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}
