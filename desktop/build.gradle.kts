import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The Windows app. Shares core/ and shared-ui/ with Android; only the desktop layout lives here.
plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    // Same shared UI sources the Android app compiles, built here against Compose for Desktop.
    sourceSets["main"].kotlin.srcDir("../shared-ui/src")
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("org.json:json:20240303")
}

// CI passes -PbuildNumber so every installer has a higher version and upgrades the last one.
val buildNumber = (findProperty("buildNumber") as String?) ?: "0"

compose.desktop {
    application {
        mainClass = "com.gozdor.slatenotes.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Slate Notes"
            packageVersion = "1.0.$buildNumber"
            description = "Slate Notes"
            vendor = "Nate Gozdor"
            includeAllModules = true
            windows {
                // Installs for the current user only, so no administrator rights are needed.
                perUserInstall = true
                menu = true
                menuGroup = "Slate Notes"
                shortcut = true
                // Fixed id: a newer installer replaces the old version instead of installing alongside it.
                upgradeUuid = "458f1bdb-5ba2-43d1-bfc8-1ac10aab1581"
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
