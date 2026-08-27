# sample-expo-brownfield

Expo アプリを **既存のネイティブアプリに組み込む（Brownfield）** ためのサンプルです。

React Native 側は「ボタンを押すと GitHub API を `expo` というキーワードで検索し、
リポジトリ一覧を表示する」だけのシンプルな画面で、これを
[`expo-brownfield`](https://docs.expo.dev/versions/latest/sdk/brownfield/) で
XCFramework（Swift Package）に固めて、ネイティブの iOS ホストアプリから表示します。

```
sample-expo-brownfield/
├── expo-app/     # Expo アプリ本体（ここから XCFramework / AAR を生成する）
│   ├── App.tsx            # 画面を組み立てるだけのエントリポイント
│   ├── src/
│   │   ├── api/github.ts              # GitHub Search API クライアント
│   │   ├── components/ActionButton.tsx
│   │   ├── components/RepositoryRow.tsx
│   │   ├── native/bridge.ts           # initialProps の型 / ネイティブへの通知
│   │   └── screens/RepoSearchScreen.tsx   # 検索画面の本体
│   ├── app.json           # expo-brownfield / expo-build-properties プラグイン設定
│   └── ios/               # `expo prebuild` の生成物（RepoSearchKit ターゲットを含む）
├── ios-host/     # 生成物を取り込む素の SwiftUI アプリ（XcodeGen でプロジェクト生成）
│   ├── project.yml
│   └── HostApp/
│       ├── ContentView.swift       # 検索ワード入力 + 受信結果の表示
│       └── RepoSearchBridge.swift  # RN → ネイティブの delegate / クロージャ
└── android-host/ # AAR を取り込む素の Compose アプリ
    ├── settings.gradle.kts         # local-repo/ を Maven リポジトリとして参照
    └── app/src/main/java/.../host/
        ├── MainActivity.kt         # 検索ワード入力 + 受信結果の表示
        ├── RepoSearchActivity.kt   # RN 画面を表示する BrownfieldActivity
        └── RepoSearchBridge.kt     # RN → ネイティブの listener / ラムダ
```

## 必要環境

| ツール | バージョン |
| --- | --- |
| Node.js | 20 以上（動作確認: 24.14.1） |
| Xcode | 16 以上（動作確認: 26.3） |
| CocoaPods | 1.16 以上 |
| XcodeGen | iOS ホストアプリの `.xcodeproj` 生成に使用（`brew install xcodegen`） |
| Android Studio | Android SDK 36 / JDK 17 が必要（Android を扱う場合のみ） |

> CocoaPods が `Unicode Normalization not appropriate for ASCII-8BIT` で落ちる場合は
> ロケールが未設定です。`export LANG=en_US.UTF-8` を設定してから実行してください。

## 1. Expo アプリ単体で動かす

```bash
cd expo-app && npm install && npm run ios
```

`src/screens/RepoSearchScreen.tsx` の「リポジトリを検索」ボタンで
`https://api.github.com/search/repositories?q=expo&sort=stars` を叩き、
スター順のリポジトリ 20 件を `FlatList` で表示します
（未認証の GitHub Search API は 10 リクエスト/分の制限があります）。

「ネイティブに戻る」ボタンは `expo-brownfield` の `popToNative()` を呼びます。
ホストアプリに組み込んだときだけ効き、単体起動時は何も起きません。

## 2. ネイティブ用のターゲットを生成する（prebuild）

```bash
cd expo-app && npm run prebuild:ios
```

`app.json` の `expo-brownfield` プラグインが、通常の `expoapp` ターゲットに加えて
**`RepoSearchKit`（フレームワーク）ターゲット**を `ios/` に追加します。
このターゲットが `ReactNativeHostManager` / `ReactNativeViewController` /
`ReactNativeView` を公開し、ホストアプリから使う API になります。

```json
[
  "expo-brownfield",
  {
    "ios": { "targetName": "RepoSearchKit", "bundleIdentifier": "com.example.sampleexpobrownfield.reposearchkit" },
    "android": { "library": "reposearchkit" }
  }
]
```

あわせて `expo-build-properties` の `ios.usePrecompiledModules: true` を有効にしており、
`pod install` が各 Expo モジュールをソースからビルドせずに
ビルド済み `.xcframework` として取得します（ビルドが速く、配布物にもそのまま同梱されます）。

## 3. XCFramework を生成する

```bash
cd expo-app
npm run brownfield:ios          # Release / Swift Package 形式
npm run brownfield:ios:debug    # Debug（Metro に接続する開発用）
```

`expo-app/artifacts/RepoSearchKitPackage-release/` に、
`RepoSearchKit.xcframework` と React / Hermes / 各 Expo モジュールの `.xcframework` 群、
それらをまとめた `Package.swift` が出力されます。

素の XCFramework だけが欲しい場合（Swift Package にしない場合）は次を使います。

```bash
npm run brownfield:ios:xcframeworks
```

### コマンドの実体

```bash
npx expo-brownfield build:ios --release --package RepoSearchKitPackage
```

主なオプション:

| オプション | 説明 |
| --- | --- |
| `-r, --release` / `-d, --debug` | ビルド構成 |
| `-p, --package [name]` | Swift Package として出力（省略すると XCFramework のみ） |
| `-a, --artifacts <path>` | 出力先（既定: `./artifacts`） |
| `--host-provided <names...>` | ホストアプリ側が既に持つフレームワークを除外する |
| `--verbose` | xcodebuild の出力をそのまま流す |

> `.binaryTarget` は構成ごとの切り替えができないため、Swift Package は
> ビルド構成ごとに `-release` / `-debug` のサフィックス付きで出力されます。
> Debug / Release 両方を使いたい場合は 2 回ビルドして使い分けてください。

## 4. ホストアプリから使う

```bash
cd ios-host && xcodegen generate && open HostApp.xcodeproj
```

`project.yml` は `../expo-app/artifacts/RepoSearchKitPackage-release` を
ローカル Swift Package として参照しているので、**先に手順 3 を実行しておく必要があります**。
Xcode で手動で追加する場合は **File → Add Package Dependencies → Add Local** から
同じディレクトリを選びます。

ホストアプリ側のコードはこれだけです。

```swift
import RepoSearchKit

@main
struct HostApp: App {
  init() {
    ReactNativeHostManager.shared.initialize()   // RN ランタイムの起動は一度だけ
  }
  var body: some Scene { WindowGroup { ContentView() } }
}
```

```swift
// SwiftUI から
NavigationLink("RN 画面を開く") {
  ReactNativeView(moduleName: "main")
}

// UIKit から
navigationController?.pushViewController(
  ReactNativeViewController(moduleName: "main", initialProps: ["from": "UIKit"]),
  animated: true
)
```

Debug 構成の成果物を組み込んだ場合は、`expo-app` で `npm start` を実行して
Metro を立ち上げてからホストアプリを起動してください。
Release 構成では JS バンドルが `RepoSearchKit.xcframework` に同梱されるため、Metro は不要です。

## 5. ネイティブ ⇄ React Native の連携

### 5-1. 検索ワードをネイティブから渡す（`initialProps`）

検索ワードは JS 側にハードコードせず、ホストアプリから `initialProps` で渡します。

```swift
// ios-host/HostApp/ContentView.swift
ReactNativeView(moduleName: "main", initialProps: ["keyword": store.effectiveKeyword])

// UIKit の場合
ReactNativeViewController(moduleName: "main", initialProps: ["keyword": keyword])
```

`initialProps` はルートコンポーネントの props としてそのまま届きます。

```tsx
// expo-app/App.tsx
export default function App({ keyword }: RootProps) {
  return <RepoSearchScreen keyword={keyword ?? DEFAULT_KEYWORD} />;
}
```

単体起動時は `initialProps` が無いので、`DEFAULT_KEYWORD`（`expo`）にフォールバックします。
ホストアプリ側では `TextField` に入力した値がそのまま次の RN 画面に渡ります。

### 5-2. 検索結果をネイティブに返す（メッセージ + delegate / クロージャ）

RN 側は検索完了時に `sendMessage()` で結果を投げます。

```ts
// expo-app/src/native/bridge.ts
export function notifySearchSucceeded(keyword: string, repositories: Repository[]) {
  sendMessage({ type: 'searchSucceeded', keyword, repositories });
}
```

ネイティブ側は `BrownfieldMessaging.addListener` で受け取ります。生のペイロードは
`[String: Any?]` なので、[`RepoSearchBridge`](ios-host/HostApp/RepoSearchBridge.swift) で
型付きの `RepoSearchEvent` に変換し、メインキューで **delegate とクロージャの両方**に流しています。

```swift
enum RepoSearchEvent {
  case succeeded(keyword: String, repositories: [SearchedRepository])
  case failed(keyword: String, message: String)
}

// delegate で受ける
final class SearchResultsStore: ObservableObject, RepoSearchBridgeDelegate {
  private lazy var bridge = RepoSearchBridge(delegate: self)

  func repoSearchBridge(_ bridge: RepoSearchBridge, didReceive event: RepoSearchEvent) { ... }
}

// クロージャで受ける
let bridge = RepoSearchBridge { event in
  if case let .succeeded(keyword, repositories) = event {
    print("\(keyword): \(repositories.count) 件")
  }
}
bridge.start()
```

サンプルでは SwiftUI 画面が delegate 版、UIKit 画面がクロージャ版を使っています。
リスナーは `start()` / `stop()` で明示的に解除してください（`deinit` でも解除されます）。

> **ペイロードの型に注意**
> - JS の数値は Swift 側で必ず `Double` になります。`as? Int` は常に失敗するので変換が必要です。
> - 配列の要素は `Any?` として渡り、`null` を含むオブジェクトは `[String: Any?]` になります。
> - `NavigationStack` でリンクを push すると root View の `onDisappear` が発火します。
>   リスナーの解除をそこに書くと、RN 画面からのメッセージを取りこぼします
>   （サンプルでは `NavigationStack` 自体に付けています）。

### API 一覧

`expo-brownfield` が用意している API:

| JS | Swift | 用途 |
| --- | --- | --- |
| `popToNative(animated)` | — | RN 画面を閉じてネイティブに戻る |
| `setNativeBackEnabled(enabled)` | — | ネイティブの戻る操作の有効/無効 |
| `sendMessage` / `addMessageListener` | `BrownfieldMessaging` | 双方向メッセージ |
| `useSharedState(key)` | `BrownfieldState` | ネイティブと共有する状態 |

## 6. Android（AAR）

### 6-1. AAR を生成する

```bash
cd expo-app
npm run prebuild:android      # expo prebuild --platform android
npm run brownfield:android    # expo-brownfield build:android --release
```

`app.json` の `expo-brownfield` プラグインが `android/reposearchkit/` に
ライブラリモジュールを生成し、`android.publishing` の設定に従って
`android-host/local-repo/` へ Maven 形式で publish します。

```json
"android": {
  "libraryName": "reposearchkit",
  "package": "com.example.sampleexpobrownfield.reposearchkit",
  "group": "com.example.sampleexpobrownfield",
  "version": "1.0.0",
  "publishing": [
    { "type": "localDirectory", "name": "hostAppRepo", "path": "../android-host/local-repo" }
  ]
}
```

`publishing` を省略すると `mavenLocal()`（`~/.m2/repository`）に publish されます。
`path` は **Expo プロジェクトのルート（`expo-app/`）からの相対パス**で、
prebuild 時に絶対 URL として `android/build.gradle` に書き込まれます。

利用可能な publish タスクとリポジトリは次で確認できます。

```bash
npx expo-brownfield tasks:android
```

### 6-2. ホストアプリから使う

`android-host/settings.gradle.kts` が `local-repo/` を Maven リポジトリとして参照し、
`app/build.gradle.kts` が AAR を依存に追加しています。

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven { url = uri("${rootDir}/local-repo") }
  }
}

// app/build.gradle.kts — group : libraryName : version
implementation("com.example.sampleexpobrownfield:reposearchkit:1.0.0")

// BrownfieldMessaging / BrownfieldState を使うために必要
implementation("expo.modules.brownfield:expo.modules.brownfield:57.0.14")
```

> `expo-brownfield` 本体は AAR の Gradle module metadata 上 **runtime スコープ**でしか
> 依存に含まれないため、`BrownfieldMessaging` を import するには上記のように
> 明示的に依存を足す必要があります（`local-repo/` に一緒に publish されています）。

RN 画面は `BrownfieldActivity` を継承した Activity で表示します。
最短の書き方は `showReactNativeFragment()` の 1 行ですが、
**この関数は launch options を受け取れません**。検索ワードを initialProps として
渡したいので、サンプルでは同関数の中身を展開して
`ReactNativeViewFactory.createFrameLayout()` に `Bundle` を渡しています。

```kotlin
class RepoSearchActivity : BrownfieldActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val keyword = intent.getStringExtra(EXTRA_KEYWORD) ?: DEFAULT_KEYWORD

    ReactNativeHostManager.shared.initialize(application)
    setContentView(
      ReactNativeViewFactory.createFrameLayout(this, this, "main", bundleOf("keyword" to keyword))
    )
    setUpNativeBackHandling()
  }
}
```

`AndroidManifest.xml` では AppCompat の NoActionBar テーマと `configChanges` が必要です。

```xml
<activity
    android:name=".RepoSearchActivity"
    android:theme="@style/Theme.AppCompat.Light.NoActionBar"
    android:configChanges="keyboard|keyboardHidden|orientation|screenLayout|screenSize|smallestScreenSize|uiMode" />
```

### 6-3. 検索結果を受け取る

iOS と同じ形で、`BrownfieldMessaging` を
[`RepoSearchBridge`](android-host/app/src/main/java/com/example/sampleexpobrownfield/host/RepoSearchBridge.kt)
が型付きイベントに変換し、**listener（delegate 相当）とラムダの両方**に流します。
Compose 画面が listener 版、`RepoSearchActivity` の Toast がラムダ版です。

```kotlin
sealed interface RepoSearchEvent {
  data class Succeeded(val keyword: String, val repositories: List<SearchedRepository>) : RepoSearchEvent
  data class Failed(val keyword: String, val message: String) : RepoSearchEvent
}

val bridge = RepoSearchBridge(onEvent = { event -> ... })
bridge.start()
```

> **Android 固有の注意点**
> - `sendMessage` のペイロードに **入れ子の `null` を含められません**。
>   `Cannot convert '[object Object]' to a Kotlin type. Value is null, expected an Object`
>   で失敗するため、`src/native/bridge.ts` では `null` になりうる値を空文字に落として送り、
>   ネイティブ側で空文字を `null` に戻しています。
> - `BrownfieldMessaging` のリスナーは Activity のライフサイクルに紐づかないグローバルなものです。
>   RN 画面は別 Activity なので、リスナーは `onCreate` で登録して `onDestroy` で解除し、
>   結果は ViewModel に保持しています（iOS の `NavigationStack` の話と同じ理由です）。
> - `SafeAreaView` は iOS でしか inset が効かないので、
>   Android ではステータスバーぶんの `paddingTop` を自前で入れています。

### 6-4. ビルドと実行

```bash
cd android-host
./gradlew installDebug
```

`local.properties` に SDK の場所を書くか、`ANDROID_HOME` を通しておいてください。

```
sdk.dir=/Users/<you>/Library/Android/sdk
```

> Android Studio 同梱の JBR は JDK 25 で、AGP 8.12 の対応範囲外です。
> `JAVA_HOME` に JDK 17 を指定してください（`brew install openjdk@17`）。

## 動作確認の状況

iOS / Android とも、**ネイティブ → RN への検索ワード受け渡し**、**RN → ネイティブへの結果通知（20 件）**、
**ネイティブ画面への復帰**まで通しで確認済みです。

| | 環境 | 生成物 |
| --- | --- | --- |
| iOS | Xcode 26.3 / iPhone 17 シミュレータ | `artifacts/RepoSearchKitPackage-release/`（約 400MB、10 個の xcframework） |
| Android | Android Studio (SDK 36 / NDK 30 / JDK 17) / Pixel 10 エミュレータ | `android-host/local-repo/`（約 6.4MB、`reposearchkit-1.0.0.aar` は 578KB） |

共通の土台は Expo SDK 57.0.16 / React Native 0.86.2 です。
どちらも Release 構成なので JS バンドルは成果物に同梱され、Metro なしで動作します。

## 既知の問題

### 1. `expo-modules-jsi` のビルドエラー（パッチ適用済み）

Expo SDK 57 (`expo-modules-jsi@57.0.5`) の `RuntimeScheduler.h` は、コンストラクタに
`SWIFT_RETURNS_RETAINED` を付けているため Swift の C++ interop がエラーになり、
`ExpoModulesJSI.xcframework` のビルドに失敗します。

```
error: 'RuntimeScheduler' cannot be annotated with either SWIFT_RETURNS_RETAINED
       or SWIFT_RETURNS_UNRETAINED because it is not returning a SWIFT_SHARED_REFERENCE type
```

本サンプルでは [patch-package](https://github.com/ds300/patch-package) で該当行を取り除いています
（`patches/expo-modules-jsi+57.0.5.patch`、`npm install` の postinstall で自動適用）。
上流の SDK 58 系では同じ修正が入っているため、SDK を上げる際にこのパッチは削除してください。

### 2. Xcode の Build Location が Custom の場合

`expo-brownfield build:ios` は `-derivedDataPath ios/build` を渡し、成果物が
`ios/build/Build/Products/<Configuration>-iphoneos/` にある前提でフレームワークを探します。
Xcode の **Settings → Locations → Advanced… → Build Location** が
`Custom (relative to workspace)` になっていると、成果物は `ios/Build/Products/...` に出力され、
ビルド自体は成功するのに次のエラーで失敗します。

```
Error: Could not find the compiled brownfield framework in the Xcode build products directory
Missing framework: .../ios/build/Build/Products/release-iphoneos/RepoSearchKit.framework
```

対処はどちらか:

1. Xcode の Build Location を `Unique`（既定）に戻す。
2. `ios/` を生成したあとにブリッジ用のシンボリックリンクを張る（`expo prebuild --clean` のたびに必要）。

```bash
mkdir -p expo-app/ios/build/Build && ln -sfn ../Products expo-app/ios/build/Build/Products
```

## 参考

- [Expo Brownfield SDK](https://docs.expo.dev/versions/latest/sdk/brownfield/)
- [How to add Expo to an existing native app](https://docs.expo.dev/brownfield/get-started/)
