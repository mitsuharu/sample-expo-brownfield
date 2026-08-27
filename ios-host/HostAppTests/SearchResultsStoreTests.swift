import RepoSearchKit
import XCTest

@testable import HostApp

/// Covers the host app's own side of the embedding: what it does with the
/// events the brownfield framework hands it.
final class SearchResultsStoreTests: XCTestCase {
  private var store: SearchResultsStore!

  override func setUp() {
    super.setUp()
    store = SearchResultsStore()
  }

  private func send(_ event: RepoSearchEvent) {
    store.repoSearchBridge(RepoSearchBridge(), didReceive: event)
  }

  private func succeeded(_ repositories: [SearchedRepository]) -> RepoSearchEvent {
    .succeeded(keyword: "expo", repositories: repositories)
  }

  private func decodedRepository() -> SearchedRepository {
    let bridge = RepoSearchBridge()
    var decoded: [SearchedRepository] = []
    let received = expectation(description: "decoded")
    bridge.onEvent = { event in
      if case .succeeded(_, let repositories) = event { decoded = repositories }
      received.fulfill()
    }
    bridge.receive([
      "type": "searchSucceeded",
      "keyword": "expo",
      "repositories": [
        ["id": 1.0, "fullName": "expo/expo", "stars": 51_842.0, "language": "TypeScript"]
      ],
    ])
    wait(for: [received], timeout: 1)
    return decoded[0]
  }

  func testStartsEmpty() {
    XCTAssertNil(store.lastKeyword)
    XCTAssertNil(store.errorMessage)
    XCTAssertTrue(store.repositories.isEmpty)
  }

  func testKeepsResultsFromASuccessfulSearch() {
    let repository = decodedRepository()

    send(succeeded([repository]))

    XCTAssertEqual(store.lastKeyword, "expo")
    XCTAssertEqual(store.repositories.count, 1)
    XCTAssertEqual(store.repositories[0].fullName, "expo/expo")
    XCTAssertNil(store.errorMessage)
  }

  func testAFailureClearsPreviousResults() {
    send(succeeded([decodedRepository()]))

    send(.failed(keyword: "expo", message: "API rate limit exceeded"))

    XCTAssertEqual(store.errorMessage, "API rate limit exceeded")
    XCTAssertTrue(store.repositories.isEmpty)
    XCTAssertEqual(store.lastKeyword, "expo")
  }

  func testASuccessClearsAPreviousFailure() {
    send(.failed(keyword: "expo", message: "boom"))

    send(succeeded([decodedRepository()]))

    XCTAssertNil(store.errorMessage)
    XCTAssertEqual(store.repositories.count, 1)
  }

  func testEffectiveKeywordFallsBackWhenBlank() {
    store.keyword = "   "
    XCTAssertEqual(store.effectiveKeyword, "expo")

    store.keyword = ""
    XCTAssertEqual(store.effectiveKeyword, "expo")
  }

  func testEffectiveKeywordIsTrimmed() {
    store.keyword = "  expo-brownfield \n"
    XCTAssertEqual(store.effectiveKeyword, "expo-brownfield")
  }
}
