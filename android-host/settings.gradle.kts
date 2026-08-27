pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    // The brownfield AAR published by `npm run brownfield:android` in ../expo-app.
    // Matches `android.publishing` in expo-app/app.json.
    maven { url = uri("${rootDir}/local-repo") }
  }
}

rootProject.name = "HostApp"

include(":app")
