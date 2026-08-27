import { fireEvent, render, waitFor } from '@testing-library/react-native'
import { popToNative } from 'expo-brownfield'

import { searchRepositories } from '../../api/github'
import { notifySearchFailed, notifySearchSucceeded } from '../../native/bridge'
import { RepoSearchScreen } from '../RepoSearchScreen'

jest.mock('expo-brownfield', () => ({
  popToNative: jest.fn(),
  sendMessage: jest.fn(),
}))
jest.mock('../../api/github', () => ({ searchRepositories: jest.fn() }))
jest.mock('../../native/bridge', () => ({
  notifySearchSucceeded: jest.fn(),
  notifySearchFailed: jest.fn(),
}))

const searchMock = searchRepositories as jest.MockedFunction<
  typeof searchRepositories
>

const results = [
  {
    id: 65750241,
    fullName: 'expo/expo',
    description: 'An open-source framework',
    stars: 51842,
    language: 'TypeScript',
    htmlUrl: 'https://github.com/expo/expo',
  },
  {
    id: 1,
    fullName: 'a/b',
    description: null,
    stars: 0,
    language: null,
    htmlUrl: 'https://github.com/a/b',
  },
]

describe('RepoSearchScreen', () => {
  beforeEach(() => jest.clearAllMocks())

  it('shows the keyword handed over by the host app', async () => {
    const { getByText } = await render(
      <RepoSearchScreen keyword="expo-brownfield" />,
    )

    expect(getByText('keyword: expo-brownfield')).toBeTruthy()
  })

  it('does not search until the button is pressed', async () => {
    const { getByText } = await render(<RepoSearchScreen keyword="expo" />)

    expect(searchMock).not.toHaveBeenCalled()
    expect(getByText('ボタンを押すと検索結果が表示されます。')).toBeTruthy()
  })

  it('lists the repositories returned for the keyword', async () => {
    searchMock.mockResolvedValue(results)
    const { getByText } = await render(<RepoSearchScreen keyword="expo" />)

    await fireEvent.press(getByText('リポジトリを検索'))

    await waitFor(() => expect(getByText('expo/expo')).toBeTruthy())
    expect(searchMock).toHaveBeenCalledWith('expo')
    expect(getByText('★ 51,842 · TypeScript')).toBeTruthy()
    // A repository with no language shows the star count on its own.
    expect(getByText('★ 0')).toBeTruthy()
  })

  it('reports the results back to the host app', async () => {
    searchMock.mockResolvedValue(results)
    const { getByText } = await render(<RepoSearchScreen keyword="expo" />)

    await fireEvent.press(getByText('リポジトリを検索'))

    await waitFor(() =>
      expect(notifySearchSucceeded).toHaveBeenCalledWith('expo', results),
    )
    expect(notifySearchFailed).not.toHaveBeenCalled()
  })

  it('shows the failure and reports it to the host app', async () => {
    searchMock.mockRejectedValue(new Error('API rate limit exceeded'))
    const { getByText } = await render(<RepoSearchScreen keyword="expo" />)

    await fireEvent.press(getByText('リポジトリを検索'))

    await waitFor(() =>
      expect(getByText('API rate limit exceeded')).toBeTruthy(),
    )
    expect(notifySearchFailed).toHaveBeenCalledWith(
      'expo',
      'API rate limit exceeded',
    )
    expect(notifySearchSucceeded).not.toHaveBeenCalled()
  })

  it('asks the host app to close the screen', async () => {
    const { getByText } = await render(<RepoSearchScreen keyword="expo" />)

    await fireEvent.press(getByText('ネイティブに戻る'))

    expect(popToNative).toHaveBeenCalledWith(true)
  })
})
