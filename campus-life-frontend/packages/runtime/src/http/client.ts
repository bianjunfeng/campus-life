export interface HttpClientOptions {
  baseURL?: string
  withCredentials?: boolean
}

export interface HttpClient {
  get<T = unknown>(url: string): Promise<T>
  post<T = unknown>(url: string, body?: unknown): Promise<T>
}

/**
 * Stub HTTP client factory. Axios wiring arrives in PR-2.
 */
export function createHttpClient(options: HttpClientOptions = {}): HttpClient {
  const { withCredentials = true } = options

  return {
    async get<T>(_url: string): Promise<T> {
      void withCredentials
      throw new Error('createHttpClient: not implemented')
    },
    async post<T>(_url: string, _body?: unknown): Promise<T> {
      void withCredentials
      throw new Error('createHttpClient: not implemented')
    }
  }
}
