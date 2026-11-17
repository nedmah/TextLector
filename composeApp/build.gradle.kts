import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}


val localProps = Properties().apply {
    rootProject.file("local.properties").inputStream().use { load(it) }
}

// sherpa-onnx libs for the OS that builds and runs the desktop app
val sherpaNativeClassifier: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    when {
        os.contains("mac") && (arch == "aarch64" || arch == "arm64") -> "osx-aarch64"
        os.contains("win") -> "win-x64"
        else -> error("Desktop build is not supported on $os/$arch")
    }
}

kotlin {
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }


    compilerOptions{
        apiVersion.set(KotlinVersion.KOTLIN_2_3)
        languageVersion.set(KotlinVersion.KOTLIN_2_3)
    }

    androidTarget{
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    
    jvm()
    jvmToolchain(21)

    // explicit call is required: a manual dependsOn would otherwise disable the default hierarchy
    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.commons.compress)
            }
        }
        androidMain.get().dependsOn(jvmAndAndroidMain)
        jvmMain.get().dependsOn(jvmAndAndroidMain)

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            // Date
            implementation(libs.kotlinx.datetime)
            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.composeViewModel)
            // Navigation
            implementation(libs.navigation.compose)
            // SQLDelight
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            // Settings
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            // Okio
            implementation(libs.okio)
            // uuid
            implementation(libs.uuid)
            // pdf
            implementation(libs.pdfbox.android)

            implementation(libs.kotlinx.serialization.json)

            // xml parsing
            implementation(libs.ksoup.core)
            implementation(libs.ksoup.network)

            implementation(libs.synth.kmp.zip)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.koin.android)
            implementation(libs.sqldelight.androidDriver)
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.13.0"))
            implementation(libs.firebase.crashlytics)
            implementation(libs.firebase.analytics)
            implementation(libs.tesseract4android)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.nativeDriver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            val sherpaVersion = libs.versions.sherpaOnnx.get()
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.sqldelight.jvmDriver)
            implementation(libs.pdfbox.jvm)
            implementation(files("libs/desktop/sherpa-onnx-jvm-$sherpaVersion.jar"))
            implementation(files("libs/desktop/sherpa-onnx-native-lib-$sherpaNativeClassifier-$sherpaVersion.jar"))
        }
    }

    sqldelight {
        databases {
            create("LectorDatabase") {
                packageName.set("com.nedmah.textlector.db")
            }
        }
    }
}

android {
    namespace = "com.nedmah.textlector"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.nedmah.textlector"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 140
        versionName = "1.4.0"
    }
    flavorDimensions += "store"
    productFlavors {
        create("rustore") { dimension = "store" }
        create("play") { dimension = "store" }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("textlector.jks")
            storePassword = localProps["KEYSTORE_PASSWORD"] as String
            keyAlias = "textlector"
            keyPassword = localProps["KEY_PASSWORD"] as String
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
    add("rustoreImplementation", libs.rustore.review)
    add("androidMainImplementation", fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}

compose.desktop {
    application {
        mainClass = "com.nedmah.textlector.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.nedmah.textlector"
            packageVersion = "1.0.0"
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            if (output is com.android.build.api.variant.impl.VariantOutputImpl) {
                val versionName = output.versionName.get() ?: "1.0"
                output.outputFileName = "TextLector-${variant.flavorName}-v${versionName}.apk"
            }
        }
    }
}
