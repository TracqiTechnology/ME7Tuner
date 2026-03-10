import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

val appVersion = project.property("app.version") as String
val pkgVersion = project.findProperty("pkg.version") as String?
    ?: appVersion.replace(Regex("-.*"), "")

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
    // Compose Desktop — use explicit platform dep on CI so uber JARs bundle skiko natives correctly
    if (System.getenv("CI") != null) {
        val osName = System.getProperty("os.name").lowercase()
        val osArch = System.getProperty("os.arch").lowercase()
        when {
            osName.contains("win") -> implementation(compose.desktop.windows_x64)
            osName.contains("mac") && osArch.contains("aarch64") -> implementation(compose.desktop.macos_arm64)
            osName.contains("mac") -> implementation(compose.desktop.macos_x64)
            else -> implementation(compose.desktop.linux_x64)
        }
    } else {
        implementation(compose.desktop.currentOs)
    }
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
    implementation("org.graalvm.js:js:22.3.4")
    implementation("org.graalvm.js:js-scriptengine:22.3.4")
    implementation("org.jdom:jdom2:2.0.6.1")

    // Removed: jfreechart, rxjava, flatlaf, flatlaf-intellij-themes
}

tasks.register<JavaExec>("screenshots") {
    mainClass.set("ScreenshotHarnessKt")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = projectDir
}

compose.desktop {
    application {
        mainClass = "MainKt"

        // GraalVM Truffle needs module opens on stock Temurin JDK
        jvmArgs += listOf(
            "--add-opens", "java.base/java.lang=ALL-UNNAMED",
            "--add-opens", "java.base/java.nio=ALL-UNNAMED",
            "--add-opens", "java.base/java.io=ALL-UNNAMED",
            "--add-opens", "java.base/java.util=ALL-UNNAMED",
        )

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ME7Tuner"
            packageVersion = pkgVersion
            description = "ME7 M-box ECU Calibration Tool"

            includeAllModules = true

            macOS {
                bundleID = "com.tracqi.me7tuner"
                iconFile.set(project.file("src/main/resources/icons/icon.icns"))
            }

            windows {
                upgradeUuid = "e4a5b6c7-d8e9-4f0a-b1c2-d3e4f5a6b7c8"
                iconFile.set(project.file("src/main/resources/icons/icon.ico"))
            }

            linux {
                iconFile.set(project.file("src/main/resources/icons/icon.png"))
            }
        }
    }
}
