import Foundation

/// One repository as it arrives from the React Native screen.
public struct SearchedRepository: Identifiable, Hashable {
  public let id: Int
  public let fullName: String
  public let stars: Int
  public let language: String?

  init?(json: [String: Any]) {
    guard let id = Self.int(json["id"]), let fullName = json["fullName"] as? String else {
      return nil
    }
    self.id = id
    self.fullName = fullName
    self.stars = Self.int(json["stars"]) ?? 0
    // The JS side sends "" instead of null (see src/native/bridge.ts).
    self.language = (json["language"] as? String).flatMap { $0.isEmpty ? nil : $0 }
  }

  /// Every JS number crosses the bridge as a `Double`, so `as? Int` alone never matches.
  private static func int(_ value: Any?) -> Int? {
    if let int = value as? Int { return int }
    if let double = value as? Double { return Int(double) }
    return nil
  }
}

/// What the React Native screen reports back. Mirrors `src/native/bridge.ts`.
public enum RepoSearchEvent {
  case succeeded(keyword: String, repositories: [SearchedRepository])
  case failed(keyword: String, message: String)
}

public protocol RepoSearchBridgeDelegate: AnyObject {
  func repoSearchBridge(_ bridge: RepoSearchBridge, didReceive event: RepoSearchEvent)
}

/// Turns the raw `[String: Any?]` messaging channel into typed events, delivered
/// on the main queue through either a delegate or a closure.
///
/// This lives inside the brownfield framework so host apps never have to know
/// the wire format: they import the framework and consume `RepoSearchEvent`.
public final class RepoSearchBridge {
  public weak var delegate: RepoSearchBridgeDelegate?
  public var onEvent: ((RepoSearchEvent) -> Void)?

  private var listenerID: String?

  public init(
    delegate: RepoSearchBridgeDelegate? = nil,
    onEvent: ((RepoSearchEvent) -> Void)? = nil
  ) {
    self.delegate = delegate
    self.onEvent = onEvent
  }

  deinit {
    stop()
  }

  public func start() {
    guard listenerID == nil else { return }
    listenerID = BrownfieldMessaging.addListener { [weak self] message in
      self?.receive(message)
    }
  }

  public func stop() {
    guard let listenerID else { return }
    BrownfieldMessaging.removeListener(id: listenerID)
    self.listenerID = nil
  }

  /// Converts one raw message and delivers the event on the main queue.
  ///
  /// The messaging channel calls this; it is public so host apps can unit test
  /// the conversion without standing up a React Native runtime.
  public func receive(_ message: [String: Any?]) {
    guard let event = Self.makeEvent(from: message) else { return }
    DispatchQueue.main.async { [weak self] in
      guard let self else { return }
      self.delegate?.repoSearchBridge(self, didReceive: event)
      self.onEvent?(event)
    }
  }

  private static func makeEvent(from message: [String: Any?]) -> RepoSearchEvent? {
    guard let type = message["type"] as? String, let keyword = message["keyword"] as? String else {
      return nil
    }

    switch type {
    case "searchSucceeded":
      // Elements cross the bridge as `Any?`, and a JS `null` field makes the
      // dictionary a `[String: Any?]`, so normalize both before decoding.
      let raw = message["repositories"] as? [Any?] ?? []
      let repositories = raw.compactMap { element -> SearchedRepository? in
        if let json = element as? [String: Any] {
          return SearchedRepository(json: json)
        }
        if let json = element as? [String: Any?] {
          return SearchedRepository(json: json.compactMapValues { $0 })
        }
        return nil
      }
      return .succeeded(keyword: keyword, repositories: repositories)
    case "searchFailed":
      return .failed(keyword: keyword, message: message["message"] as? String ?? "unknown error")
    default:
      // Messages this bridge doesn't care about.
      return nil
    }
  }
}
