import { BrowserRouter }  from 'react-router-dom';
import { ConfigProvider } from 'antd';
import AppRouter          from './router/AppRouter';

const theme = {
  token: {
    colorPrimary:        '#2563eb',
    colorPrimaryHover:   '#1d4ed8',
    borderRadius:        8,
    fontFamily:          "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', sans-serif",
  },
};

export default function App() {
  return (
    <ConfigProvider theme={theme}>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
    </ConfigProvider>
  );
}
