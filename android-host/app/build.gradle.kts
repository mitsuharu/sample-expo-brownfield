plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.sampleexpobrownfield.host"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.example.sampleexpobrownfield.host"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  buildFeatures { compose = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  // The brownfield library. Coordinates come from `android` in the
  // expo-brownfield plugin config (group : libraryName : version).
  implementation("com.example.sampleexpobrownfield:reposearchkit:1.0.0")

  // expo-brownfield itself (BrownfieldMessaging / BrownfieldState) reaches the
  // app only as a *runtime* dependency of the library above, so it has to be
  // declared explicitly to be usable at compile time.
  implementation("expo.modules.brownfield:expo.modules.brownfield:57.0.14")

  // BrownfieldActivity extends AppCompatActivity, and the React Native view is
  // hosted through a Fragment, so the host app provides both.
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("androidx.fragment:fragment-ktx:1.8.5")

  implementation("androidx.core:core-ktx:1.15.0")
  implementation("androidx.activity:activity-compose:1.9.3")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation(platform("androidx.compose:compose-bom:2024.10.01"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.material3:material3")
  debugImplementation("androidx.compose.ui:ui-tooling")
}
