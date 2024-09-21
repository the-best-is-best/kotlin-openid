import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
    alias(libs.plugins.android.library)
    alias(libs.plugins.native.cocoapods)
    id("maven-publish")
    id("signing")
    alias(libs.plugins.maven.publish)
}




apply(plugin = "maven-publish")
apply(plugin = "signing")


tasks.withType<PublishToMavenRepository> {
    val isMac = getCurrentOperatingSystem().isMacOsX
    onlyIf {
        isMac.also {
            if (!isMac) logger.error(
                """
                        Publishing the library requires macOS to be able to generate iOS artifacts.
                        Run the task on a mac or use the project GitHub workflows for publication and release.
                    """
            )
        }
    }
}



mavenPublishing {
    coordinates("x", "xx", "xxx")

    publishToMavenCentral(SonatypeHost.S01, automaticRelease = true)
    signAllPublications()

    pom {
        name.set("x")
        description.set("x")
        url.set("x")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        issueManagement {
            system.set("x")
            url.set("x")
        }
        scm {
            connection.set("x")
            url.set("x")
        }
        developers {
            developer {
                id.set("x")
                name.set("x")
                email.set("x")
            }
        }
    }

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
        //https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-test.html
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
            dependencies {
                debugImplementation(libs.androidx.testManifest)
                implementation(libs.androidx.junit4)
            }
        }
    }

    //    jvm()
    //
    //    js {
    //        browser()
    //        binaries.executable()
    //    }
    //
    //    @OptIn(ExperimentalWasmDsl::class)
    //    wasmJs {
    //        browser()
    //        binaries.executable()
    //    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "KMMOpenId"
            isStatic = true
        }
    }



    cocoapods {
        version = "1.0"
        summary = "Some description for a Kotlin/Native module"
        homepage = "Link to a Kotlin/Native module homepage"

        // Optional properties
        // Configure the Pod name here instead of changing the Gradle project name
        name = "KMMOpenId"

        framework {
            baseName = "KMMOpenId"
        }
        noPodspec()
        ios.deploymentTarget = "12.0"  // Update this to the required version

        pod("AppAuth") {
            version = "1.7.5"
            extraOpts += listOf("-compiler-option", "-fmodules")

        }
//            pod("KServices") {
//                version = "0.1.7" // Ensure this is the correct version
//                extraOpts += listOf("-compiler-option", "-fmodules")
//            }


    }



    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.kmm.crypto)



        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        androidMain.dependencies {
            implementation(compose.uiTooling)
            implementation(libs.androidx.activityCompose)
            implementation(libs.appauth)
            implementation("com.google.code.gson:gson:2.11.0")

        }

        //        jvmMain.dependencies {
        //            implementation(compose.desktop.currentOs)
        //        }
        //
        //        jsMain.dependencies {
        //            implementation(compose.html.core)
        //        }

        iosMain.dependencies {
        }

    }
}

android {
    namespace = "io.github.lib"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        buildFeatures {
            //enables a Compose tooling support in the AndroidStudio
            compose = true
        }
    }
}
compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.github.lib.desktopApp"
            packageVersion = "1.0.0"
        }
    }
}
