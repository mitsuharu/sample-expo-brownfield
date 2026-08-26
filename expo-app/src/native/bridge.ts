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

export function notifySearchSucceeded(keyword: string, repositories: Repository[]) {
  sendMessage({
    type: MessageType.searchSucceeded,
    keyword,
    repositories,
  });
}

export function notifySearchFailed(keyword: string, message: string) {
  sendMessage({
    type: MessageType.searchFailed,
    keyword,
    message,
  });
}
