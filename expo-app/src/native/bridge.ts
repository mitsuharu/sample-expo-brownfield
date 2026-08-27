/**
 * Messages this React Native screen sends back to the native host app.
 *
 * Native listens with `BrownfieldMessaging.addListener { message in ... }`
 * (see ios-host/HostApp/RepoSearchBridge.swift).
 */
import { sendMessage } from 'expo-brownfield';

import type { Repository } from '../api/github';

/** Props the native host passes in through `initialProps`. */
export type RootProps = {
  keyword?: string;
};

export const DEFAULT_KEYWORD = 'expo';

export const MessageType = {
  searchSucceeded: 'searchSucceeded',
  searchFailed: 'searchFailed',
} as const;

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
  };
}

export function notifySearchSucceeded(keyword: string, repositories: Repository[]) {
  sendMessage({
    type: MessageType.searchSucceeded,
    keyword,
    repositories: repositories.map(toPayload),
  });
}

export function notifySearchFailed(keyword: string, message: string) {
  sendMessage({
    type: MessageType.searchFailed,
    keyword,
    message,
  });
}
