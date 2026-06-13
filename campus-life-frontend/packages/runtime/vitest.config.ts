import { defineConfig } from 'vitest/config'
import { fileURLToPath } from 'node:url'

const runtimeRoot = fileURLToPath(new URL('.', import.meta.url))

export default defineConfig({
  root: runtimeRoot,
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts']
  }
})
