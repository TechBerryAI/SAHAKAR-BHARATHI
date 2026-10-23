import { describe, expect, it } from 'vitest'
import { productionApiUrlProblem } from './vite.config.ts'

describe('production API URL guard', () => {
  it.each([
    [undefined, 'is not set'],
    ['', 'is not set'],
    ['not a url', 'is not a valid URL'],
    ['http://sahakar-bharathi-api.onrender.com', 'must use https://'],
    ['https://127.0.0.1:8000', 'points at a local development server'],
    ['https://localhost:8000', 'points at a local development server'],
    ['https://api.invalid', 'is a placeholder host'],
    ['https://api.example.com', 'is a placeholder host'],
    ['https://sahakar-bharathi-api.onrender.com/api/v1', 'must be an origin only (no path, query or fragment)'],
  ])('rejects %s', (value, problem) => {
    expect(productionApiUrlProblem(value)).toBe(problem)
  })

  it.each(['https://sahakar-bharathi-api.onrender.com', 'https://sahakar-bharathi-api.onrender.com/'])(
    'accepts %s',
    (value) => {
      expect(productionApiUrlProblem(value)).toBeNull()
    },
  )
})
