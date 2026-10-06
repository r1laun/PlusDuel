import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { safeGet, safeSet } from '../storage';
import { playClick } from '../sound/click';
import en from './en';
import zh from './zh';
import ru from './ru';
import ar from './ar';
import es from './es';

export type Lang = 'en' | 'zh' | 'ru' | 'ar' | 'es';
export type Key = keyof typeof en;

/** Compile-time check: every locale must define every key. */
const LOCALES: Record<Lang, Record<Key, string>> = { en, zh, ru, ar, es };

export const LANG_ORDER: Lang[] = ['en', 'zh', 'ru', 'ar', 'es'];
export const LANG_LABELS: Record<Lang, string> = {
  en: 'EN',
  zh: '中文',
  ru: 'РУ',
  ar: 'عربي',
  es: 'ES',
};

const LANG_KEY = 'pd:lang';

function detectLang(): Lang {
  const saved = safeGet(LANG_KEY);
  if (saved === 'en' || saved === 'zh' || saved === 'ru' || saved === 'ar' || saved === 'es') return saved;
  const nav = typeof navigator !== 'undefined' ? navigator.language.slice(0, 2).toLowerCase() : 'en';
  if (nav === 'zh' || nav === 'ru' || nav === 'ar' || nav === 'es') return nav;
  return 'en';
}

interface LangCtx {
  lang: Lang;
  label: string;
  cycle: () => void;
  t: (key: Key, vars?: Record<string, string | number>) => string;
  titleLabel: (serverTitle: string) => string;
}

const Ctx = createContext<LangCtx | null>(null);

export function LanguageProvider({ children }: { children: ReactNode }) {
  const [lang, setLang] = useState<Lang>(detectLang);

  useEffect(() => {
    safeSet(LANG_KEY, lang);
    document.documentElement.lang = lang;
    document.documentElement.dir = lang === 'ar' ? 'rtl' : 'ltr';
  }, [lang]);

  const value = useMemo<LangCtx>(() => {
    const dict = LOCALES[lang];
    const t = (key: Key, vars?: Record<string, string | number>) => {
      let s: string = dict[key] ?? en[key] ?? key;
      if (vars) {
        for (const [k, v] of Object.entries(vars)) s = s.replace(`{${k}}`, String(v));
      }
      return s;
    };
    return {
      lang,
      label: LANG_LABELS[lang],
      cycle: () => {
        playClick();
        setLang((l) => LANG_ORDER[(LANG_ORDER.indexOf(l) + 1) % LANG_ORDER.length]!);
      },
      t,
      titleLabel: (serverTitle: string) => {
        const k = `title.${serverTitle}` as Key;
        return dict[k] ?? serverTitle;
      },
    };
  }, [lang]);

  return <Ctx.Provider value={value}>{children}</Ctx.Provider>;
}

export function useLang(): LangCtx {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useLang must be used inside LanguageProvider');
  return ctx;
}
