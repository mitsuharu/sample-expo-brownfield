import { popToNative } from 'expo-brownfield'
import { useCallback, useEffect, useState } from 'react'
import {
  ActivityIndicator,
  FlatList,
  Platform,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native'

import { type Repository, searchRepositories } from '../api/github'
import { ActionButton } from '../components/ActionButton'
import { RepositoryRow } from '../components/RepositoryRow'
import {
  addKeywordListener,
  notifySearchFailed,
  notifySearchSucceeded,
} from '../native/bridge'

type Props = {
  /** Handed over by the host app through `initialProps`. */
  initialKeyword: string
}

export function RepoSearchScreen({ initialKeyword }: Props) {
  // initialProps only arrives while the host app is creating this screen, so
  // replacing the keyword on an open screen comes over the message channel.
  const [keyword, setKeyword] = useState(initialKeyword)
  const [repositories, setRepositories] = useState<Repository[]>([])
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(
    () =>
      addKeywordListener((next) => {
        setKeyword(next)
        setRepositories([])
        setError(null)
      }),
    [],
  )

  const onPressSearch = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const results = await searchRepositories(keyword)
      setRepositories(results)
      // Hand the results back to the native host app.
      notifySearchSucceeded(keyword, results)
    } catch (e) {
      const message = e instanceof Error ? e.message : String(e)
      setRepositories([])
      setError(message)
      notifySearchFailed(keyword, message)
    } finally {
      setIsLoading(false)
    }
  }, [keyword])

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>GitHub Repositories</Text>
        <Text style={styles.subtitle}>keyword: {keyword}</Text>
      </View>

      <View style={styles.actions}>
        <ActionButton
          disabled={isLoading}
          label={isLoading ? '検索中...' : 'リポジトリを検索'}
          onPress={onPressSearch}
        />
        {/* Only has an effect when this screen is embedded in a native host app. */}
        <ActionButton
          label="ネイティブに戻る"
          onPress={() => popToNative(true)}
          variant="secondary"
        />
      </View>

      {error !== null && <Text style={styles.error}>{error}</Text>}

      {isLoading && repositories.length === 0 ? (
        <ActivityIndicator style={styles.loading} size="large" />
      ) : (
        <FlatList
          contentContainerStyle={styles.listContent}
          data={repositories}
          keyExtractor={(item) => String(item.id)}
          ListEmptyComponent={
            error === null ? (
              <Text style={styles.empty}>
                ボタンを押すと検索結果が表示されます。
              </Text>
            ) : null
          }
          renderItem={({ item }) => <RepositoryRow repository={item} />}
        />
      )}
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: '#fff',
    flex: 1,
    // SafeAreaView only insets on iOS, so keep the header clear of the
    // Android status bar as well.
    paddingTop: Platform.OS === 'android' ? (StatusBar.currentHeight ?? 0) : 0,
  },
  header: {
    paddingHorizontal: 20,
    paddingTop: 24,
  },
  title: {
    fontSize: 24,
    fontWeight: '700',
  },
  subtitle: {
    color: '#6b7280',
    fontSize: 14,
    marginTop: 4,
  },
  actions: {
    flexDirection: 'row',
    gap: 8,
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  error: {
    color: '#b91c1c',
    paddingBottom: 8,
    paddingHorizontal: 20,
  },
  loading: {
    marginTop: 32,
  },
  listContent: {
    paddingBottom: 32,
    paddingHorizontal: 20,
  },
  empty: {
    color: '#9ca3af',
    marginTop: 32,
    textAlign: 'center',
  },
})
