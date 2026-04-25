import { OverlayTrigger, Tooltip } from 'react-bootstrap';
import { FiSun, FiMoon } from 'react-icons/fi';
import { useTheme } from '../../contexts/ThemeContext';

/**
 * Animated sun/moon slide toggle — styled with React Bootstrap + custom CSS.
 * Pattern inspired by Lemonade / Hippo Insurance / Snapsheet dashboards.
 */
export default function ThemeToggle() {
  const { theme, toggleTheme } = useTheme();
  const isDark = theme === 'dark';

  const tooltipText = isDark ? 'Switch to light mode' : 'Switch to dark mode';

  return (
    <OverlayTrigger
      placement="bottom"
      delay={{ show: 400, hide: 100 }}
      overlay={<Tooltip id="theme-toggle-tooltip">{tooltipText}</Tooltip>}
    >
      <button
        onClick={toggleTheme}
        className={`ci360-theme-toggle ${isDark ? 'is-dark' : 'is-light'}`}
        aria-label={tooltipText}
        aria-pressed={isDark}
        type="button"
      >
        <span className="ci360-theme-toggle__track">
          <span className="ci360-theme-toggle__icon ci360-theme-toggle__icon--sun" aria-hidden="true">
            <FiSun size={14} />
          </span>
          <span className="ci360-theme-toggle__icon ci360-theme-toggle__icon--moon" aria-hidden="true">
            <FiMoon size={14} />
          </span>
          <span className="ci360-theme-toggle__thumb" />
        </span>
      </button>
    </OverlayTrigger>
  );
}
