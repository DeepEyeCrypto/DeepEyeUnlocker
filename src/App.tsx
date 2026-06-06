import { BrowserRouter, Routes, Route } from 'react-router-dom';
import '@fontsource/space-grotesk/500.css';
import '@fontsource/space-grotesk/600.css';
import '@fontsource/space-grotesk/700.css';
import '@fontsource/inter/400.css';
import '@fontsource/inter/500.css';
import '@fontsource/inter/700.css';
import './styles/globals.css';

import { AppShell } from './components/Layout/AppShell';
import { DashboardScreen } from './screens/DashboardScreen';
import { ModesScreen } from './screens/ModesScreen';
import { ToolboxScreen } from './screens/ToolboxScreen';
import { LogsScreen } from './screens/LogsScreen';
import { FmiScreen } from './screens/FmiScreen';
import { SettingsScreen } from './screens/SettingsScreen';
import { EdlScreen } from './screens/EdlScreen';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<AppShell />}>
          <Route index element={<DashboardScreen />} />
          <Route path="modes" element={<ModesScreen />} />
          <Route path="fmi" element={<FmiScreen />} />
          <Route path="toolbox" element={<ToolboxScreen />} />
          <Route path="logs" element={<LogsScreen />} />
          <Route path="edl" element={<EdlScreen />} />
          <Route path="settings" element={<SettingsScreen />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}

export default App;
