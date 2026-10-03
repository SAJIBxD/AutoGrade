import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.autograde"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autograde"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { stream ->
                localProperties.load(stream)
            }
        }

        // Emulator -> host Django: 10.0.2.2. Override with -PbaseUrl or in local.properties.
        val baseUrl = providers.gradleProperty("baseUrl")
            .getOrElse(localProperties.getProperty("baseUrl", "http://10.0.2.2:8000/"))
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")

        // Must match backend GOOGLE_CLIENT_ID
        // Priority: -PgoogleWebClientId flag, else local.properties
        // Pass with: ./gradlew :app:installDebug -PgoogleWebClientId=xxx.apps.googleusercontent.com
        val googleWebClientId = providers.gradleProperty("googleWebClientId")
            .getOrElse(localProperties.getProperty("googleWebClientId", ""))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.datastore.prefs)
    implementation(libs.credentials)
    implementation(libs.credentials.play)
    implementation(libs.googleid)
    implementation(libs.play.auth)
    implementation(libs.coil)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
