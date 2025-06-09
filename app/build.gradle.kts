import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.secrets.gradle.plugin)
    alias(libs.plugins.kotlin.parcelize)

}
fun getApiKey(propertyName: String): String {
    val secretsFile = rootProject.file("secrets.properties")
    val fallbackFile = rootProject.file("local.defaults.properties")

    val properties = Properties()
    val usedFile = if (secretsFile.exists()) {
        properties.load(secretsFile.inputStream())
        secretsFile
    } else {
        properties.load(fallbackFile.inputStream())
        fallbackFile
    }

    val value = properties.getProperty(propertyName)
    println("🔍 API KEY READ: $propertyName = ${value ?: "NOT FOUND"} (from ${usedFile.name})")
    return value ?: ""
}
android {
    namespace = "com.example.tripcue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tripcue"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "NAVER_CLIENT_ID", "\"${getApiKey("NAVER_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_D_CLIENT_ID", "\"${getApiKey("NAVER_D_CLIENT_ID")}\"")
        buildConfigField("String", "NAVER_D_CLIENT_SECRET", "\"${getApiKey("NAVER_D_CLIENT_SECRET")}\"")
        manifestPlaceholders["NAVER_CLIENT_ID"] = getApiKey("NAVER_CLIENT_ID")


    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.maps)
    implementation(libs.naver.map.compose)
    implementation(libs.firebase.database)
    implementation(libs.composeMaterial3)
    implementation(libs.composeWindowSizeClass)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(platform("com.google.firebase:firebase-bom:33.14.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Firebase BOM (Bill of Materials) – 버전 관리 자동화
//    implementation (platform("com.google.firebase:firebase-bom:32.7.2")) // 최신 안정 버전 기준

// Firebase 인증
    implementation ("com.google.firebase:firebase-auth-ktx")

// Firebase 실시간 데이터베이스 (사용 시)
    implementation ("com.google.firebase:firebase-database-ktx")

// Firebase Firestore (Cloud Firestore를 사용할 경우)
    implementation ("com.google.firebase:firebase-firestore-ktx")
// Jetpack Compose UI
    implementation ("androidx.compose.ui:ui:1.5.4")
    implementation ("androidx.compose.material3:material3:1.2.1")
    implementation ("androidx.activity:activity-compose:1.8.2")

// Lifecycle (ViewModel, State 관리용)
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

// Navigation (Compose Navigation 사용 시)
    implementation ("androidx.navigation:navigation-compose:2.7.7")

    implementation("com.naver.maps:map-sdk:3.21.0")

    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

}