import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)

}

kotlin {
    jvmToolchain(17)

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
//            implementation(libs.components.resources)
            implementation(libs.ui.tooling.preview)

            implementation(project(":kappAuthCMP"))


            implementation(libs.koin.androidx.compose)

            implementation(project(":simple:simpleKMPPorject"))



        }

//        commonTest.dependencies {
//            implementation(kotlin("test"))
//            @OptIn(ExperimentalComposeLibrary::class)
//            implementation(compose.uiTest)
//        }

        androidMain.dependencies {
            implementation(libs.ui.tooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.ktor.client.okhttp)

        }

//        jvmMain.dependencies {
//            implementation(compose.desktop.currentOs)
//        }

        iosMain.dependencies {

        }

    }


    android {
//        signingConfigs {
//            getByName("debug") {
//                storeFile =
//                    file("/Users/michelleraouf/Desktop/kmm/kotlin-openid/simple/composeApp/src/androidMain/key")
//                storePassword = "key-pass"
//                keyPassword = "key-pass"
//                keyAlias = "key0"
//            }
//        }
        namespace = "io.github.sample"
        compileSdk = 36
        minSdk = 24
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
