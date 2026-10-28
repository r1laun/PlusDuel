import { useEffect, useState } from 'react';
import { useLang, type Key } from '../i18n';
import {
  getAudioSettings,
  playClick,
  playStyleEquip,
  setHapticsEnabled,
  setMusicEnabled,
  setSoundEnabled,
  subscribeAudio,
  toggleAllAudio,
} from '../sound/click';
import {
  ACCENT_SKINS,
  TILE_SHAPES,
  useTheme,
  type AccentSkinId,
  type ThemeMode,
  type TileShapeId,
} from '../theme';

const PD_LOGO_DOTS: Array<[number, number]> = [
  [1, 0], [2, 0], [3, 0], [4, 0], [5, 0],
  [0, 1], [1, 1],
  [4, 2], [5, 2],
  [0, 3], [1, 3], [2, 3], [3, 3], [4, 3],
];

export function PlusDuelLogo({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`pd-logo${compact ? ' pd-logo--compact' : ''}`}>
      <svg
        className="pd-logo__mark"
        width={compact ? 36 : 48}
        height={compact ? 24 : 32}
        viewBox="0 0 60 40"
        aria-hidden="true"
      >
        {PD_LOGO_DOTS.map(([c, r]) => (
          <circle
            key={`${c}-${r}`}
            cx={5 + c * 10}
            cy={5 + r * 10}
            r={4.3}
            fill="currentColor"
          />
        ))}
      </svg>
      <span>
        <span className="pd-logo__plus">Plus</span>
        <span className="pd-logo__dual">Duel</span>
      </span>
    </div>
  );
}

export default function ThemeButton() {
  const { theme, themeMode, skin, tileShape, toggle, setThemeMode, setSkin, setTileShape } =
    useTheme();
  const { t } = useLang();
  const [openModal, setOpenModal] = useState(false);
  const [audio, setAudio] = useState(getAudioSettings);

  useEffect(() => {
    return subscribeAudio(() => setAudio(getAudioSettings()));
  }, []);

  const dark = theme === 'dark';
  const anyAudioOn = audio.soundEnabled || audio.musicEnabled;

  const skinTitle = (id: AccentSkinId) => t(`skin.${id}` as Key);
  const shapeTitle = (id: TileShapeId) => t(`shape.${id}` as Key);

  return (
    <>
      <button
        className="pd-pill-btn"
        onClick={() => {
          playClick();
          toggle();
        }}
        title={t('themeToggle')}
        aria-label={t('themeToggle')}
      >
        {dark ? '☀' : '☾'}
      </button>

      <button
        className={`pd-pill-btn${anyAudioOn ? ' pd-pill-btn--active' : ''}`}
        onClick={() => {
          toggleAllAudio();
        }}
        title={t('audioSection')}
        aria-label={t('audioSection')}
      >
        {anyAudioOn ? 'SND' : 'OFF'}
      </button>

      <button
        className={`pd-pill-btn${openModal ? ' pd-pill-btn--active' : ''}`}
        onClick={() => {
          playClick();
          setOpenModal(true);
        }}
        title={t('styleModalTitle')}
      >
        <span>●</span>
        <span>{t('styleBtn')}</span>
      </button>

      {openModal && (
        <div
          className="pd-modal-backdrop"
          onClick={() => {
            playClick();
            setOpenModal(false);
          }}
        >
          <div className="pd-modal" onClick={(e) => e.stopPropagation()}>
            <div className="pd-modal__header">
              <h2 className="pd-modal__title">{t('styleModalTitle')}</h2>
              <button
                className="pd-pill-btn"
                onClick={() => {
                  playClick();
                  setOpenModal(false);
                }}
                aria-label={t('close')}
              >
                ✕
              </button>
            </div>

            {/* 1. Accent Color Skins (from PlusMath CustomizationCatalog) */}
            <div>
              <div className="pd-section-label">{t('skinSection')}</div>
              <div className="pd-skin-list">
                {ACCENT_SKINS.map((s) => {
                  const active = skin === s.id;
                  const swatch = dark ? s.darkAccent : s.lightAccent;
                  return (
                    <button
                      key={s.id}
                      className={`pd-skin-item${active ? ' pd-skin-item--active' : ''}`}
                      onClick={() => {
                        playStyleEquip();
                        setSkin(s.id);
                      }}
                    >
                      <span
                        className="pd-skin-swatch"
                        style={{ backgroundColor: swatch }}
                      />
                      <span>{skinTitle(s.id)}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 2. Digit Tile Shape */}
            <div>
              <div className="pd-section-label">{t('shapeSection')}</div>
              <div className="pd-shape-row">
                {TILE_SHAPES.map((sh) => {
                  const active = tileShape === sh.id;
                  return (
                    <button
                      key={sh.id}
                      className={`pd-shape-item${active ? ' pd-shape-item--active' : ''}`}
                      onClick={() => {
                        playStyleEquip();
                        setTileShape(sh.id);
                      }}
                    >
                      <span
                        className="pd-shape-preview"
                        style={{ borderRadius: `${sh.radiusPx}px` }}
                      >
                        7
                      </span>
                      <span>{shapeTitle(sh.id)}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* 3. Theme Mode (Auto / Paper / Carbon) */}
            <div>
              <div className="pd-section-label">{t('themeSection')}</div>
              <div className="pd-shape-row">
                {(
                  [
                    ['system', t('themeAuto')],
                    ['light', t('themeLight')],
                    ['dark', t('themeDark')],
                  ] as Array<[ThemeMode, string]>
                ).map(([mode, label]) => (
                  <button
                    key={mode}
                    className={`pd-shape-item${themeMode === mode ? ' pd-shape-item--active' : ''}`}
                    onClick={() => {
                      playClick();
                      setThemeMode(mode);
                    }}
                  >
                    <span>{label}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* 4. Audio & Haptics */}
            <div>
              <div className="pd-section-label">{t('audioSection')}</div>
              <div className="pd-toggle-row">
                <span className="pd-toggle-row__title">{t('sfxToggle')}</span>
                <button
                  className={`pd-pill-btn${audio.soundEnabled ? ' pd-pill-btn--active' : ''}`}
                  onClick={() => {
                    const next = !audio.soundEnabled;
                    setSoundEnabled(next);
                    if (next) playClick();
                  }}
                >
                  {audio.soundEnabled ? 'ON' : 'OFF'}
                </button>
              </div>
              <div className="pd-toggle-row">
                <span className="pd-toggle-row__title">{t('musicToggle')}</span>
                <button
                  className={`pd-pill-btn${audio.musicEnabled ? ' pd-pill-btn--active' : ''}`}
                  onClick={() => {
                    playClick();
                    setMusicEnabled(!audio.musicEnabled);
                  }}
                >
                  {audio.musicEnabled ? 'ON' : 'OFF'}
                </button>
              </div>
              <div className="pd-toggle-row">
                <span className="pd-toggle-row__title">{t('hapticsToggle')}</span>
                <button
                  className={`pd-pill-btn${audio.hapticsEnabled ? ' pd-pill-btn--active' : ''}`}
                  onClick={() => {
                    setHapticsEnabled(!audio.hapticsEnabled);
                    playClick();
                  }}
                >
                  {audio.hapticsEnabled ? 'ON' : 'OFF'}
                </button>
              </div>
            </div>

            <button
              className="pd-btn pd-btn--primary"
              onClick={() => {
                playClick();
                setOpenModal(false);
              }}
            >
              {t('close')}
            </button>
          </div>
        </div>
      )}
    </>
  );
}
