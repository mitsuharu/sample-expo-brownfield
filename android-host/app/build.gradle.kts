plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.example.sample.expo.brownfield.host"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.example.sample.expo.brownfield.host"
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

  // Robolectric provides Looper/Context, so the bridge and the Activity glue
  // can be covered by plain unit tests instead of instrumentation tests.
  testOptions { unitTests.isReturnDefaultValues = true }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  // The brownfield library. Coordinates come from `android` in the
  // expo-brownfield plugin config (group : libraryName : version).
  implementation("com.example.sample.expo.brownfield:reposearchkit:1.0.1")

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

  testImplementation("junit:junit:4.13.2")
  testImplementation("org.robolectric:robolectric:4.14.1")
  testImplementation("androidx.test:core:1.6.1")
}
