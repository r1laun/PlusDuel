import { useCallback, useEffect, useState } from 'react';
import { safeGet, safeSet } from './storage';

export type Theme = 'light' | 'dark';

const KEY = 'pd:theme';

function systemDark(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );
}

/** Saved choice, if the user ever toggled. Null = follow the system. */
function savedTheme(): Theme | null {
  const s = safeGet(KEY);
  return s === 'light' || s === 'dark' ? s : null;
}

export function useTheme() {
  const [explicit, setExplicit] = useState<Theme | null>(savedTheme);
  const [sysDark, setSysDark] = useState(systemDark);

  // Follow live system changes only while the user hasn't chosen.
  useEffect(() => {
    if (explicit) return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSysDark(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, [explicit]);

  const theme: Theme = explicit ?? (sysDark ? 'dark' : 'light');

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const toggle = useCallback(() => {
    setExplicit((prev) => {
      const next: Theme = (prev ?? (systemDark() ? 'dark' : 'light')) === 'dark' ? 'light' : 'dark';
      safeSet(KEY, next);
      return next;
    });
  }, []);

  return { theme, toggle };
}
