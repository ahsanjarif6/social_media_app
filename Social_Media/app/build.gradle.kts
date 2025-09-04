import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("plugin.serialization") version "2.2.0"
}

android {
    namespace = "com.example.social_media"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.social_media"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildTypes {
        debug {
            buildConfigField("String", "SUPABASE_URL", "\"${getLocalProperty("SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getLocalProperty("SUPABASE_ANON_KEY")}\"")

        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "SUPABASE_URL", "\"${getLocalProperty("SUPABASE_URL")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${getLocalProperty("SUPABASE_ANON_KEY")}\"")

        }
    }

    buildFeatures {
        buildConfig = true
    }
}

fun getLocalProperty(key: String): String {
    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
        return properties.getProperty(key, "")
    }
    return ""
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.exoplayer)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.media3:media3-ui:1.7.1")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation(platform("io.github.jan-tennert.supabase:bom:3.2.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt:3.2.0")
    implementation("io.github.jan-tennert.supabase:auth-kt:3.2.0")
    implementation("io.github.jan-tennert.supabase:realtime-kt:3.2.0")
    implementation("io.github.jan-tennert.supabase:storage-kt:3.2.0")

    implementation("io.ktor:ktor-client-android:3.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-core:3.2.3")
    implementation("io.ktor:ktor-client-websockets:3.2.3")
    implementation("io.ktor:ktor-client-okhttp:3.2.3")
}