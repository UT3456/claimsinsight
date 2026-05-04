import { BrowserRouter }                           from 'react-router-dom';
import { ConfigProvider, App as AntApp, theme as antdTheme } from 'antd';
import AppRouter                                   from './router/AppRouter';
import { ThemeProvider, useTheme }                 from './contexts/ThemeContext';
import ErrorBoundary                               from './components/common/ErrorBoundary';

/** Inner component so we can consume the ThemeContext created by ThemeProvider. */
function ThemedApp() {
  const { theme } = useTheme();
  const isDark    = theme === 'dark';

  const antTheme = {
    algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      // ── ClaimInsight360 polish spec tokens ──
      colorPrimary:      '#185FA5',
      colorPrimaryHover: '#0C447C',
      colorSuccess:      '#27500A',
      colorWarning:      '#633806',
      colorError:        '#791F1F',
      colorInfo:         '#0C447C',
      borderRadius:      8,
      borderRadiusLG:    10,
      borderRadiusSM:    6,
      fontFamily:        "-apple-system, BlinkMacSystemFont, 'Inter', 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif",
      fontSize:          12,
      fontWeightStrong:  500,   // spec: never 600/700
      colorBgLayout:     isDark ? '#1A1A18' : '#F1EFE8',
      colorBgContainer:  isDark ? '#242421' : '#ffffff',
      colorBgElevated:   isDark ? '#242421' : '#ffffff',
      colorTextBase:     isDark ? '#F1EFE8' : '#2C2C2A',
      colorBorder:       isDark ? '#33332F' : '#E8E6DF',
      colorBorderSecondary: isDark ? '#33332F' : '#E8E6DF',
      boxShadow:         'none',
      boxShadowSecondary:'none',
    },
    components: {
      Table: {
        headerBg:        isDark ? '#2E2E2A' : '#F8F7F3',
        headerColor:     isDark ? '#B4B2A9' : '#888780',
        headerSplitColor:'transparent',
        rowHoverBg:      isDark ? '#2E2E2A' : '#F8F7F3',
        fontSize:        12,
      },
      Card: { paddingLG: 16, borderRadiusLG: 10 },
      Button: { fontWeight: 500, controlHeight: 32, paddingInline: 12 },
      Tag: { defaultBg: 'transparent', defaultColor: '#5F5E5A' },
    },
  };

  return (
    <ConfigProvider theme={antTheme}>
      {/* AntApp provides context for message/notification/modal static APIs so
          they pick up the configured theme and sit above the rest of the tree. */}
      <AntApp>
        <BrowserRouter>
          {/* ErrorBoundary catches render-time exceptions in any page so a
              single bad component cannot blank the whole app. Async errors
              are NOT caught here — those go through the axios interceptor
              and per-page catch blocks. */}
          <ErrorBoundary>
            <AppRouter />
          </ErrorBoundary>
        </BrowserRouter>
      </AntApp>
    </ConfigProvider>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <ThemedApp />
    </ThemeProvider>
  );
}
