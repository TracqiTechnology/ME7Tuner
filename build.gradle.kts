import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

repositories {
    mavenCentral()
    google()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

@Suppress("DEPRECATION")
dependencies {
    // Compose Desktop
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    // Kept from original project
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-csv:1.8")
    implementation("org.graalvm.js:js:22.0.0")
    implementation("org.graalvm.js:js-scriptengine:22.0.0")
    implementation("org.jdom:jdom2:2.0.6.1")

    // Removed: jfreechart, rxjava, flatlaf, flatlaf-intellij-themes
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ME7Tuner"
            packageVersion = "2.0.0"
            description = "ME7 M-box ECU Calibration Tool"

            macOS {
                bundleID = "com.tracqi.me7tuner"
            }

            windows {
                upgradeUuid = "e4a5b6c7-d8e9-4f0a-b1c2-d3e4f5a6b7c8"
            }
        }
    }
}
