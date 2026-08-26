import RepoSearchKit
import SwiftUI

@main
struct HostApp: App {
  init() {
    // Boots the React Native runtime once, before any React Native view is created.
    ReactNativeHostManager.shared.initialize()
  }

  var body: some Scene {
    WindowGroup {
      ContentView()
    }
  }
}
