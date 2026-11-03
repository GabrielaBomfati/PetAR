
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ifpr.androidapptemplatear"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ifpr.androidapptemplatear"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    sourceSets["main"].assets.srcDirs("src/main/assets")
}

configurations.all {
    exclude(
        group = "com.android.support",
        module = "support-core-ui"
    )

    exclude(
        group = "com.android.support",
        module = "support-compat"
    )
}

dependencies {

    // Android e interface
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)

    // Bibliotecas do projeto original
    implementation(libs.okhttp)
    implementation(libs.glide)
    implementation(libs.firebase.database.ktx)

    // Localização dos animais no mapa
    implementation(libs.play.services.location)

    // SceneView 2.2.1
    // Realidade aumentada e visualização 3D
    implementation("io.github.sceneview:arsceneview:2.2.1")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(libs.androidx.junit)
}
