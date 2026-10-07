import { useLang } from '../i18n';
import { playClick } from '../sound/click';
import { useTheme } from '../theme';

export default function ThemeButton() {
  const { theme, toggle } = useTheme();
  const { t } = useLang();
  const dark = theme === 'dark';

  return (
    <button
      className="pd-btn pd-btn--outline pd-btn--sm"
      onClick={() => {
        playClick();
        toggle();
      }}
      title={t('themeToggle')}
      aria-label={t('themeToggle')}
    >
      {dark ? '☀' : '☾'}
    </button>
  );
}
