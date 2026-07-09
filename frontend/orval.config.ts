import { defineConfig } from 'orval'

export default defineConfig({
  blueforge: {
    input: {
      target: 'http://localhost:8080/v3/api-docs',
    },
    output: {
      target: 'src/api/generated.ts',
      client: 'react-query',
      httpClient: 'fetch',
      mode: 'single',
      override: {
        mutator: {
          path: 'src/api/mutator.ts',
          name: 'apiFetch',
        },
        fetch: {
          includeHttpResponseReturnType: false,
        },
      },
    },
  },
})
