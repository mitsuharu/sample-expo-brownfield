import { StyleSheet, Text, View } from 'react-native'

import type { Repository } from '../api/github'

type Props = {
  repository: Repository
}

export function RepositoryRow({ repository }: Props) {
  return (
    <View style={styles.row}>
      <Text style={styles.title}>{repository.fullName}</Text>
      {repository.description !== null && (
        <Text numberOfLines={2} style={styles.description}>
          {repository.description}
        </Text>
      )}
      <Text style={styles.meta}>
        ★ {repository.stars.toLocaleString()}
        {repository.language !== null ? ` · ${repository.language}` : ''}
      </Text>
    </View>
  )
}

const styles = StyleSheet.create({
  row: {
    borderTopColor: '#e5e7eb',
    borderTopWidth: StyleSheet.hairlineWidth,
    paddingVertical: 12,
  },
  title: {
    fontSize: 16,
    fontWeight: '600',
  },
  description: {
    color: '#4b5563',
    fontSize: 13,
    marginTop: 4,
  },
  meta: {
    color: '#6b7280',
    fontSize: 12,
    marginTop: 6,
  },
})
