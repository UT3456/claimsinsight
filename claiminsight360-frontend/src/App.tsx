import { BrowserRouter }                   from 'react-router-dom';
import { ConfigProvider, theme as antdTheme } from 'antd';
import AppRouter                              from './router/AppRouter';
import { ThemeProvider, useTheme }            from './contexts/ThemeContext';

/** Inner component so we can consume the ThemeContext created by ThemeProvider. */
function ThemedApp() {
  const { theme } = useTheme();
  const isDark    = theme === 'dark';

  const antTheme = {
    algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      // Color Hunt palette — cool slate
      colorPrimary:      '#2563eb',
      colorPrimaryHover: '#1d4ed8',
      colorSuccess:      '#3CB371',
      colorWarning:      '#F8E16C',
      colorError:        '#F45B69',
      colorInfo:         '#526D82',
      borderRadius:      8,
      fontFamily:        "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif",
      colorBgLayout:     isDark ? '#1B2430' : '#DDE6ED',
      colorBgContainer:  isDark ? '#27374D' : '#ffffff',
      colorTextBase:     isDark ? '#DDE6ED' : '#27374D',
      colorBorder:       isDark ? '#344966' : '#D4DEE8',
    },
  };

  return (
    <ConfigProvider theme={antTheme}>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
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
