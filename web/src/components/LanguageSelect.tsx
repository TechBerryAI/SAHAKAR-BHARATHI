import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { useLanguage } from '../contexts/language.tsx'
import type { LanguageOption } from '../lib/languages.ts'

type LanguageSelectProps = {
  variant?: 'dark' | 'light' | 'bar'
}

export function LanguageSelect({ variant = 'dark' }: LanguageSelectProps) {
  const { languages, current, language, setLanguage, copy } = useLanguage()
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const frame = open ? document.querySelector('[data-app-frame]') : null

  useEffect(() => {
    if (!open) return
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') setOpen(false)
    }
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [open])

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase()
    if (!needle) return languages
    return languages.filter(
      (option) =>
        option.name.toLowerCase().includes(needle) || option.native.toLowerCase().includes(needle),
    )
  }, [languages, query])

  function choose(option: LanguageOption) {
    setLanguage(option.code)
    setOpen(false)
  }

  return (
    <>
      <div className={`lang-select lang-select--${variant}`}>
        <button
          type="button"
          className="lang-select__button"
          aria-haspopup="dialog"
          aria-expanded={open}
          onClick={() => {
            setQuery('')
            setOpen(true)
          }}
        >
          <svg className="lang-select__globe" width="16" height="16" viewBox="0 0 24 24" aria-hidden="true">
            <path
              fill="currentColor"
              d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2m-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39"
            />
          </svg>
          <span className="lang-select__current">{current.native}</span>
          <svg className="lang-select__chevron" width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="currentColor" d="M7 10l5 5 5-5z" />
          </svg>
        </button>
      </div>
      {open && frame
        ? createPortal(
            <div className="lang-sheet" role="presentation">
              <button
                type="button"
                className="lang-sheet__backdrop"
                aria-label={copy.back}
                onClick={() => setOpen(false)}
              />
              <div
                className="lang-sheet__panel"
                role="dialog"
                aria-modal="true"
                aria-labelledby="lang-sheet-title"
              >
                <div className="lang-sheet__handle" aria-hidden="true" />
                <h2 id="lang-sheet-title" className="lang-sheet__title">
                  {copy.chooseLanguage}
                </h2>
                <label className="lang-sheet__search">
                  <svg width="18" height="18" viewBox="0 0 24 24" aria-hidden="true">
                    <path
                      fill="currentColor"
                      d="M15.5 14h-.79l-.28-.27A6.47 6.47 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14"
                    />
                  </svg>
                  <input
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder={copy.searchLanguages}
                    autoFocus
                  />
                </label>
                <ul className="lang-sheet__list" role="listbox" aria-label={copy.chooseLanguage}>
                  {filtered.length === 0 ? (
                    <li className="lang-sheet__empty">{copy.noLanguages}</li>
                  ) : (
                    filtered.map((option) => {
                      const selected = option.code === language
                      return (
                        <li key={option.code}>
                          <button
                            type="button"
                            role="option"
                            aria-selected={selected}
                            className={`lang-sheet__option${selected ? ' is-active' : ''}`}
                            onClick={() => choose(option)}
                          >
                            <span className="lang-sheet__copy">
                              <span className="lang-sheet__native">{option.native}</span>
                              {/* English names only in English; every other UI stays in its own language. */}
                              {language === 'en' ? (
                                <span className="lang-sheet__name">{option.name}</span>
                              ) : null}
                            </span>
                            {selected ? (
                              <svg width="22" height="22" viewBox="0 0 24 24" aria-hidden="true">
                                <path
                                  fill="currentColor"
                                  d="M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"
                                />
                              </svg>
                            ) : null}
                          </button>
                        </li>
                      )
                    })
                  )}
                </ul>
              </div>
            </div>,
            frame,
          )
        : null}
    </>
  )
}
