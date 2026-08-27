import RepoSearchKit
import SwiftUI

/// Receives search results through `RepoSearchBridgeDelegate`.
final class SearchResultsStore: ObservableObject, RepoSearchBridgeDelegate {
  @Published var keyword: String = "expo"
  @Published private(set) var lastKeyword: String?
  @Published private(set) var repositories: [SearchedRepository] = []
  @Published private(set) var errorMessage: String?

  /// The keyword handed to React Native through `initialProps`.
  var effectiveKeyword: String {
    let trimmed = keyword.trimmingCharacters(in: .whitespacesAndNewlines)
    return trimmed.isEmpty ? "expo" : trimmed
  }

  private lazy var bridge = RepoSearchBridge(delegate: self)

  func startListening() {
    bridge.start()
  }

  func stopListening() {
    bridge.stop()
  }

  func repoSearchBridge(_ bridge: RepoSearchBridge, didReceive event: RepoSearchEvent) {
    switch event {
    case .succeeded(let keyword, let repositories):
      self.lastKeyword = keyword
      self.repositories = repositories
      self.errorMessage = nil
    case .failed(let keyword, let message):
      self.lastKeyword = keyword
      self.repositories = []
      self.errorMessage = message
    }
  }
}

/// The native part of the app. The keyword is typed here and passed into
/// React Native, and the results come back through `RepoSearchBridge`.
struct ContentView: View {
  @StateObject private var store = SearchResultsStore()

  var body: some View {
    NavigationStack {
      List {
        Section("検索ワード") {
          TextField("keyword", text: $store.keyword)
            .autocorrectionDisabled()
            .textInputAutocapitalization(.never)
        }

        Section("React Native") {
          NavigationLink {
            RepoSearchScreen(keyword: store.effectiveKeyword)
          } label: {
            Label("SwiftUI から開く", systemImage: "magnifyingglass")
          }

          NavigationLink {
            RepoSearchUIKitScreen(keyword: store.effectiveKeyword)
          } label: {
            Label("UIKit から開く", systemImage: "rectangle.stack")
          }
        }

        Section("React Native から受け取った結果") {
          if let errorMessage = store.errorMessage {
            Text(errorMessage).foregroundStyle(.red)
          } else if let lastKeyword = store.lastKeyword {
            LabeledContent("keyword", value: lastKeyword)
            LabeledContent("件数", value: "\(store.repositories.count)")
            ForEach(store.repositories.prefix(3)) { repository in
              LabeledContent(repository.fullName, value: "★ \(repository.stars)")
            }
          } else {
            Text("まだ結果を受け取っていません").foregroundStyle(.secondary)
          }
        }
      }
      .navigationTitle("Host App")
    }
    // Attached to the NavigationStack, not the List: pushing a link makes the
    // List disappear, and the RN screen reports its results while it is on top.
    .onAppear { store.startListening() }
    .onDisappear { store.stopListening() }
  }
}

/// SwiftUI entry point: `ReactNativeView` is vended by the generated brownfield framework.
private struct RepoSearchScreen: View {
  let keyword: String

  var body: some View {
    ReactNativeView(moduleName: "main", initialProps: ["keyword": keyword])
      .ignoresSafeArea(edges: .bottom)
      .navigationTitle("Repo Search (RN)")
      .navigationBarTitleDisplayMode(.inline)
  }
}

/// Receives search results through the closure form of `RepoSearchBridge`.
final class SearchStatusModel: ObservableObject {
  @Published private(set) var status: String = "結果待ち"

  private lazy var bridge: RepoSearchBridge = {
    RepoSearchBridge { [weak self] event in
      switch event {
      case .succeeded(let keyword, let repositories):
        self?.status = "\(keyword): \(repositories.count) 件受信"
      case .failed(let keyword, _):
        self?.status = "\(keyword): 取得失敗"
      }
    }
  }()

  func startListening() {
    bridge.start()
  }

  func stopListening() {
    bridge.stop()
  }
}

/// UIKit entry point: the same screen through `ReactNativeViewController`,
/// which is what an existing UIKit navigation stack would push.
private struct RepoSearchUIKitScreen: View {
  let keyword: String

  @StateObject private var model = SearchStatusModel()

  var body: some View {
    ReactNativeViewControllerRepresentable(keyword: keyword)
      .ignoresSafeArea(edges: .bottom)
      .navigationTitle(model.status)
      .navigationBarTitleDisplayMode(.inline)
      .onAppear { model.startListening() }
      .onDisappear { model.stopListening() }
  }
}

private struct ReactNativeViewControllerRepresentable: UIViewControllerRepresentable {
  let keyword: String

  func makeUIViewController(context: Context) -> UIViewController {
    ReactNativeViewController(moduleName: "main", initialProps: ["keyword": keyword])
  }

  func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

#Preview {
  ContentView()
}
