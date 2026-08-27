/**
 * Minimal GitHub Search API client.
 * https://docs.github.com/en/rest/search/search#search-repositories
 */

const SEARCH_ENDPOINT = 'https://api.github.com/search/repositories'

export type Repository = {
  id: number
  fullName: string
  description: string | null
  stars: number
  language: string | null
  htmlUrl: string
}

type SearchResponse = {
  total_count: number
  items: {
    id: number
    full_name: string
    description: string | null
    stargazers_count: number
    language: string | null
    html_url: string
  }[]
  message?: string
}

export async function searchRepositories(
  keyword: string,
  perPage: number = 20,
): Promise<Repository[]> {
  const query = new URLSearchParams({
    q: keyword,
    sort: 'stars',
    order: 'desc',
    per_page: String(perPage),
  })

  const response = await fetch(`${SEARCH_ENDPOINT}?${query.toString()}`, {
    headers: {
      Accept: 'application/vnd.github+json',
      'X-GitHub-Api-Version': '2022-11-28',
    },
  })

  const json = (await response.json()) as SearchResponse

  if (!response.ok) {
    // Unauthenticated search is rate limited to 10 requests / minute.
    throw new Error(
      json.message ?? `GitHub API responded with ${response.status}`,
    )
  }

  return json.items.map((item) => ({
    id: item.id,
    fullName: item.full_name,
    description: item.description,
    stars: item.stargazers_count,
    language: item.language,
    htmlUrl: item.html_url,
  }))
}
