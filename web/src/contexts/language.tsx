import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { copyFor, type AppCopy } from '../lib/copy.ts'
import { LANGUAGES, isLangCode, languageByCode, type LangCode } from '../lib/languages.ts'

const STORAGE_KEY = 'sahakar.language'

type LanguageContextValue = {
  language: LangCode
  setLanguage: (code: LangCode) => void
  copy: AppCopy
}

const LanguageContext = createContext<LanguageContextValue | null>(null)

function readStoredLanguage(): LangCode {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    if (isLangCode(stored)) return stored
  } catch {
    // Ignore storage access errors.
  }
  return 'en'
}

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<LangCode>(readStoredLanguage)

  useEffect(() => {
    const option = languageByCode(language)
    document.documentElement.lang = language
    document.documentElement.dir = option.rtl ? 'rtl' : 'ltr'
    document.title = copyFor(language).appTitle
    try {
      localStorage.setItem(STORAGE_KEY, language)
    } catch {
      // Ignore storage access errors.
    }
  }, [language])

  const value = useMemo<LanguageContextValue>(
    () => ({
      language,
      setLanguage: setLanguageState,
      copy: copyFor(language),
    }),
    [language],
  )

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

// The hook lives beside its provider; editing this file just triggers a full reload in dev.
// eslint-disable-next-line react-refresh/only-export-components
export function useLanguage() {
  const context = useContext(LanguageContext)
  if (!context) {
    throw new Error('useLanguage must be used inside LanguageProvider')
  }
  return { ...context, languages: LANGUAGES, current: languageByCode(context.language) }
}
