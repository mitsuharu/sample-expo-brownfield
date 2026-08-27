import { StatusBar } from 'expo-status-bar'

import { DEFAULT_KEYWORD, type RootProps } from './src/native/bridge'
import { RepoSearchScreen } from './src/screens/RepoSearchScreen'

/**
 * `keyword` comes from the native host app via `initialProps`, e.g.
 * `ReactNativeView(moduleName: "main", initialProps: ["keyword": "swift"])`.
 * It falls back to a default so the app still runs standalone.
 */
export default function App({ keyword }: RootProps) {
  return (
    <>
      <StatusBar style="dark" />
      <RepoSearchScreen keyword={keyword ?? DEFAULT_KEYWORD} />
    </>
  )
}
