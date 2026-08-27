import RepoSearchKit
import XCTest

/// Covers the conversion from the raw messaging payload to typed events —
/// the part of the embedding that is easy to get wrong and hard to see fail.
final class RepoSearchBridgeTests: XCTestCase {
  private func succeeded(_ repositories: [Any?]) -> [String: Any?] {
    ["type": "searchSucceeded", "keyword": "expo", "repositories": repositories]
  }

  private func repository(
    id: Any? = 65_750_241.0,
    fullName: Any? = "expo/expo",
    stars: Any? = 51_842.0,
    language: Any? = "TypeScript"
  ) -> [String: Any] {
    ["id": id as Any, "fullName": fullName as Any, "stars": stars as Any, "language": language as Any]
  }

  private func event(
    for message: [String: Any?],
    timeout: TimeInterval = 1
  ) -> RepoSearchEvent? {
    let received = expectation(description: "event")
    var result: RepoSearchEvent?

    let bridge = RepoSearchBridge { event in
      result = event
      received.fulfill()
    }
    bridge.receive(message)
    wait(for: [received], timeout: timeout)

    return result
  }

  func testDecodesRepositories() throws {
    let event = try XCTUnwrap(event(for: succeeded([repository()])))

    guard case let .succeeded(keyword, repositories) = event else {
      return XCTFail("expected .succeeded, got \(event)")
    }
    XCTAssertEqual(keyword, "expo")
    XCTAssertEqual(repositories.count, 1)
    XCTAssertEqual(repositories[0].id, 65_750_241)
    XCTAssertEqual(repositories[0].fullName, "expo/expo")
    XCTAssertEqual(repositories[0].stars, 51_842)
    XCTAssertEqual(repositories[0].language, "TypeScript")
  }

  func testAcceptsIntegersAsWellAsDoubles() throws {
    // Numbers cross the bridge as Double, but an Int must not break decoding.
    let event = try XCTUnwrap(event(for: succeeded([repository(id: 7, stars: 3)])))

    guard case let .succeeded(_, repositories) = event else {
      return XCTFail("expected .succeeded, got \(event)")
    }
    XCTAssertEqual(repositories[0].id, 7)
    XCTAssertEqual(repositories[0].stars, 3)
  }

  func testTreatsEmptyLanguageAsMissing() throws {
    // The JS side sends "" rather than null, which Android cannot bridge.
    let event = try XCTUnwrap(event(for: succeeded([repository(language: "")])))

    guard case let .succeeded(_, repositories) = event else {
      return XCTFail("expected .succeeded, got \(event)")
    }
    XCTAssertNil(repositories[0].language)
  }

  func testSkipsRepositoriesMissingRequiredFields() throws {
    let event = try XCTUnwrap(
      event(for: succeeded([repository(), repository(id: nil), repository(fullName: nil), "junk"]))
    )

    guard case let .succeeded(_, repositories) = event else {
      return XCTFail("expected .succeeded, got \(event)")
    }
    XCTAssertEqual(repositories.count, 1)
  }

  func testDefaultsMissingStarsToZero() throws {
    var json = repository()
    json.removeValue(forKey: "stars")
    let event = try XCTUnwrap(event(for: succeeded([json])))

    guard case let .succeeded(_, repositories) = event else {
      return XCTFail("expected .succeeded, got \(event)")
    }
    XCTAssertEqual(repositories[0].stars, 0)
  }

  func testDecodesFailure() throws {
    let event = try XCTUnwrap(
      event(for: ["type": "searchFailed", "keyword": "expo", "message": "API rate limit exceeded"])
    )

    guard case let .failed(keyword, message) = event else {
      return XCTFail("expected .failed, got \(event)")
    }
    XCTAssertEqual(keyword, "expo")
    XCTAssertEqual(message, "API rate limit exceeded")
  }

  func testIgnoresUnrelatedMessages() {
    let ignored: [[String: Any?]] = [
      ["type": "somethingElse", "keyword": "expo"],
      ["keyword": "expo"],
      ["type": "searchSucceeded"],
    ]

    for message in ignored {
      let notified = expectation(description: "not notified")
      notified.isInverted = true
      let bridge = RepoSearchBridge { _ in notified.fulfill() }
      bridge.receive(message)
      wait(for: [notified], timeout: 0.2)
    }
  }

  func testNotifiesDelegateAndClosure() {
    final class SpyDelegate: RepoSearchBridgeDelegate {
      var events: [RepoSearchEvent] = []
      let received: XCTestExpectation
      init(received: XCTestExpectation) { self.received = received }
      func repoSearchBridge(_ bridge: RepoSearchBridge, didReceive event: RepoSearchEvent) {
        XCTAssertTrue(Thread.isMainThread, "events must arrive on the main queue")
        events.append(event)
        received.fulfill()
      }
    }

    let delegateCalled = expectation(description: "delegate")
    let closureCalled = expectation(description: "closure")
    let delegate = SpyDelegate(received: delegateCalled)
    let bridge = RepoSearchBridge(delegate: delegate) { _ in closureCalled.fulfill() }

    bridge.receive(succeeded([repository()]))

    wait(for: [delegateCalled, closureCalled], timeout: 1)
    XCTAssertEqual(delegate.events.count, 1)
  }

  func testStopIsIdempotent() {
    let bridge = RepoSearchBridge()
    bridge.stop()
    bridge.start()
    bridge.start()
    bridge.stop()
    bridge.stop()
  }
}
