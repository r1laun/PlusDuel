import { useCallback, useEffect, useState } from 'react';
import { safeGet, safeSet } from './storage';

export type Theme = 'light' | 'dark';
export type ThemeMode = 'system' | 'light' | 'dark';
export type AccentSkinId = 'orange' | 'emerald' | 'violet' | 'gold' | 'cyan' | 'crimson';
export type TileShapeId = 'rounded' | 'pill' | 'tech';

export interface AccentSkinSpec {
  id: AccentSkinId;
  lightAccent: string;
  darkAccent: string;
  lightTint: string;
  darkTint: string;
}

export interface TileShapeSpec {
  id: TileShapeId;
  radiusPx: number;
}

export const ACCENT_SKINS: AccentSkinSpec[] = [
  {
    id: 'orange',
    lightAccent: '#ff5c00',
    darkAccent: '#ff7a2e',
    lightTint: '#fff0e6',
    darkTint: '#2c1b10',
  },
  {
    id: 'emerald',
    lightAccent: '#0d9488',
    darkAccent: '#2dd4bf',
    lightTint: '#e6fffa',
    darkTint: '#0f2926',
  },
  {
    id: 'violet',
    lightAccent: '#7c3aed',
    darkAccent: '#a78bfa',
    lightTint: '#f3e8ff',
    darkTint: '#221538',
  },
  {
    id: 'gold',
    lightAccent: '#d97706',
    darkAccent: '#fbbf24',
    lightTint: '#fef3c7',
    darkTint: '#2e210b',
  },
  {
    id: 'cyan',
    lightAccent: '#0284c7',
    darkAccent: '#38bdf8',
    lightTint: '#e0f2fe',
    darkTint: '#0c2536',
  },
  {
    id: 'crimson',
    lightAccent: '#e11d48',
    darkAccent: '#fb7185',
    lightTint: '#ffe4e6',
    darkTint: '#301219',
  },
];

export const TILE_SHAPES: TileShapeSpec[] = [
  { id: 'rounded', radiusPx: 12 },
  { id: 'pill', radiusPx: 28 },
  { id: 'tech', radiusPx: 4 },
];

const THEME_KEY = 'pd:theme';
const SKIN_KEY = 'pd:skin';
const SHAPE_KEY = 'pd:shape';

function systemDark(): boolean {
  return (
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-color-scheme: dark)').matches
  );
}

function savedThemeMode(): ThemeMode {
  const s = safeGet(THEME_KEY);
  return s === 'light' || s === 'dark' ? s : 'system';
}

function savedSkin(): AccentSkinId {
  const s = safeGet(SKIN_KEY);
  if (ACCENT_SKINS.some((k) => k.id === s)) return s as AccentSkinId;
  return 'orange';
}

function savedShape(): TileShapeId {
  const s = safeGet(SHAPE_KEY);
  if (TILE_SHAPES.some((k) => k.id === s)) return s as TileShapeId;
  return 'rounded';
}

let currentThemeMode: ThemeMode = savedThemeMode();
let currentSkin: AccentSkinId = savedSkin();
let currentShape: TileShapeId = savedShape();
const themeListeners = new Set<() => void>();

function applyDomVariables(resolvedTheme: Theme, skinId: AccentSkinId, shapeId: TileShapeId): void {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  root.dataset.theme = resolvedTheme;
  root.dataset.skin = skinId;
  root.dataset.shape = shapeId;

  const skin = ACCENT_SKINS.find((s) => s.id === skinId) ?? ACCENT_SKINS[0]!;
  const shape = TILE_SHAPES.find((s) => s.id === shapeId) ?? TILE_SHAPES[0]!;
  const isDark = resolvedTheme === 'dark';

  root.style.setProperty('--pd-orange', isDark ? skin.darkAccent : skin.lightAccent);
  root.style.setProperty('--pd-tint', isDark ? skin.darkTint : skin.lightTint);
  root.style.setProperty('--pd-radius-sm', `${shape.radiusPx}px`);
}

function notifyThemeListeners(): void {
  for (const fn of themeListeners) fn();
}

export function useTheme() {
  const [sysDark, setSysDark] = useState(systemDark);
  const [, setTick] = useState(0);

  useEffect(() => {
    const onUpdate = () => setTick((t) => t + 1);
    themeListeners.add(onUpdate);
    return () => {
      themeListeners.delete(onUpdate);
    };
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return;
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSysDark(e.matches);
    mq.addEventListener('change', onChange);
    return () => mq.removeEventListener('change', onChange);
  }, []);

  const theme: Theme =
    currentThemeMode === 'system' ? (sysDark ? 'dark' : 'light') : currentThemeMode;

  useEffect(() => {
    applyDomVariables(theme, currentSkin, currentShape);
  }, [theme]);

  const setThemeMode = useCallback((mode: ThemeMode) => {
    currentThemeMode = mode;
    safeSet(THEME_KEY, mode);
    const resolved: Theme = mode === 'system' ? (systemDark() ? 'dark' : 'light') : mode;
    applyDomVariables(resolved, currentSkin, currentShape);
    notifyThemeListeners();
  }, []);

  const toggle = useCallback(() => {
    const resolved: Theme =
      currentThemeMode === 'system' ? (systemDark() ? 'dark' : 'light') : currentThemeMode;
    const next: Theme = resolved === 'dark' ? 'light' : 'dark';
    setThemeMode(next);
  }, [setThemeMode]);

  const setSkin = useCallback((skin: AccentSkinId) => {
    currentSkin = skin;
    safeSet(SKIN_KEY, skin);
    const resolved: Theme =
      currentThemeMode === 'system' ? (systemDark() ? 'dark' : 'light') : currentThemeMode;
    applyDomVariables(resolved, currentSkin, currentShape);
    notifyThemeListeners();
  }, []);

  const setTileShape = useCallback((shape: TileShapeId) => {
    currentShape = shape;
    safeSet(SHAPE_KEY, shape);
    const resolved: Theme =
      currentThemeMode === 'system' ? (systemDark() ? 'dark' : 'light') : currentThemeMode;
    applyDomVariables(resolved, currentSkin, currentShape);
    notifyThemeListeners();
  }, []);

  return {
    theme,
    themeMode: currentThemeMode,
    skin: currentSkin,
    tileShape: currentShape,
    toggle,
    setThemeMode,
    setSkin,
    setTileShape,
  };
}
