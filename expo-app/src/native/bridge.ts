/**
 * The message channel between this screen and the native host app. It carries
 * traffic in both directions: results out, commands in.
 *
 * Both ends are typed by RepoSearchBridge, which lives in the brownfield
 * artifact (see expo-app/native/).
 */
import { addMessageListener, sendMessage } from 'expo-brownfield'

import type { Repository } from '../api/github'

/** Props the native host passes in through `initialProps`. */
export type RootProps = {
  keyword?: string
}

export const DEFAULT_KEYWORD = 'expo'

/** Sent to native. */
export const MessageType = {
  searchSucceeded: 'searchSucceeded',
  searchFailed: 'searchFailed',
} as const

/** Received from native. */
export const CommandType = {
  setKeyword: 'setKeyword',
} as const

/**
 * Android の `sendMessage` は入れ子になった `null` を Kotlin の型に変換できず
 * `Cannot convert '[object Object]' to a Kotlin type. Value is null` で失敗する。
 * ネイティブが使うフィールドだけに絞り、`null` になりうる値は空文字に落として送る。
 */
function toPayload(repository: Repository) {
  return {
    id: repository.id,
    fullName: repository.fullName,
    stars: repository.stars,
    language: repository.language ?? '',
  }
}

export function notifySearchSucceeded(
  keyword: string,
  repositories: Repository[],
) {
  sendMessage({
    type: MessageType.searchSucceeded,
    keyword,
    repositories: repositories.map(toPayload),
  })
}

export function notifySearchFailed(keyword: string, message: string) {
  sendMessage({
    type: MessageType.searchFailed,
    keyword,
    message,
  })
}

/**
 * Listens for the host app changing the keyword.
 *
 * `initialProps` can only deliver a value while the screen is being created,
 * so replacing it on a screen that is already open goes through the message
 * channel instead.
 */
export function addKeywordListener(onKeyword: (keyword: string) => void) {
  const subscription = addMessageListener((event) => {
    if (event?.type !== CommandType.setKeyword) {
      return
    }
    if (typeof event.keyword === 'string' && event.keyword.length > 0) {
      onKeyword(event.keyword)
    }
  })

  return () => subscription.remove()
}
