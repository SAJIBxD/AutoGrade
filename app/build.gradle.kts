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
        // Emulator -> host Django: 10.0.2.2 . Override with -PbaseUrl for device/prod.
        val baseUrl = providers.gradleProperty("baseUrl").getOrElse("http://10.0.2.2:8000/")
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        // Must match backend GOOGLE_CLIENT_ID in backend/.env.
        // Priority: -PgoogleWebClientId flag, else backend/.env (same repo).
        // Pass with: ./gradlew :app:installDebug -PgoogleWebClientId=xxx.apps.googleusercontent.com
        val googleWebClientId = providers.gradleProperty("googleWebClientId").getOrElse(
            rootProject.projectDir.resolve("../backend/.env").let { f ->
                if (f.exists()) {
                    f.readLines()
                        .map { it.trim() }
                        .firstOrNull { it.startsWith("GOOGLE_CLIENT_ID=") }
                        ?.substringAfter("=")
                        ?.trim()
                        ?.trim('"', '\'')
                        .orEmpty()
                } else {
                    ""
                }
            },
        )
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
