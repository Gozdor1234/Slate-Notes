plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.gozdor.slatenotes"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gozdor.slatenotes"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "0.6.1"
    }

    // One fixed key for every build, so a new APK installs over the old one
    // without uninstalling. This is a personal sideload key, not a Play Store key.
    signingConfigs {
        create("shared") {
            storeFile = file("signing/slate.keystore")
            storePassword = "slatenotes"
            keyAlias = "slate"
            keyPassword = "slatenotes"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("shared")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    // UI code shared with the Windows app (compiled here against Jetpack Compose).
    sourceSets["main"].java.srcDir("../shared-ui/src")
}

dependencies {
    implementation(project(":core"))

    val composeBom = platform("androidx.compose:compose-bom:2025.05.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // Markdown rendering for reading view
    val markwon = "4.6.2"
    implementation("io.noties.markwon:core:$markwon")
    implementation("io.noties.markwon:ext-strikethrough:$markwon")
    implementation("io.noties.markwon:ext-tables:$markwon")
    implementation("io.noties.markwon:ext-tasklist:$markwon")
    implementation("io.noties.markwon:image:$markwon")
}
