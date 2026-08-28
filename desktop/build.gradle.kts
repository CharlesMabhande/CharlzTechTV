import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
}

group = "com.charlztech"
version = "1.0.11"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.animation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.8.2")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.9.0")
    implementation("uk.co.caprica:vlcj:4.8.3")
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "com.charlztech.tv.MainKt"
        jvmArgs += listOf(
            "--add-opens=java.base/java.nio=ALL-UNNAMED",
            "-Dfile.encoding=UTF-8"
        )
        nativeDistributions {
            // Full JRE so target PCs do not need Java installed.
            includeAllModules = true
            // Ships VLC natives under app/resources (windows/) — no system VLC required.
            appResourcesRootDir.set(project.layout.projectDirectory.dir("packaging/resources"))
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "CharlzTechTV"
            packageVersion = "1.0.11"
            description = "CharlzTechTV - Live Sports Streaming for Windows"
            vendor = "CharlzTech Software Developers"
            windows {
                menuGroup = "CharlzTechTV"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                dirChooser = true
                // Machine install keeps runtime + VLC natives under a stable Program Files path.
                perUserInstall = false
            }
        }
    }
}
