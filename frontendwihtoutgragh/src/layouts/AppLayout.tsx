import { useState }            from 'react';
import { Outlet }              from 'react-router-dom';
import Sidebar                 from '../components/common/Sidebar';
import Navbar                  from '../components/common/Navbar';
import { useServiceWarmup }    from '../hooks/useServiceWarmup';

export default function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  // Fire lightweight pings to each backend service so their lazy-initialised
  // beans are warm by the time the first page's real data requests land.
  useServiceWarmup();

  return (
    <div style={styles.shell}>
      <Sidebar collapsed={collapsed} />

      <div style={styles.main}>
        <Navbar
          collapsed={collapsed}
          onToggle={() => setCollapsed((c) => !c)}
        />
        <main style={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  shell: {
    display: 'flex',
    minHeight: '100vh',
    background: 'var(--ci-bg-app)',
  },
  main: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0,          // prevents flex child overflow
    overflow: 'hidden',
  },
  content: {
    flex: 1,
    overflowY: 'auto',
    padding: '24px',
  },
};
