// Versions match React Native 0.86.2's version catalog
// (node_modules/react-native/gradle/libs.versions.toml), so the host app and the
// brownfield AAR are built by the same AGP / Kotlin toolchain.
plugins {
  id("com.android.application") version "9.3.2" apply false
  id("org.jetbrains.kotlin.android") version "2.1.20" apply false
  id("org.jetbrains.kotlin.plugin.compose") version "2.1.20" apply false
}
