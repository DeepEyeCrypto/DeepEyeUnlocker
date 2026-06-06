import { render, screen, fireEvent } from '@testing-library/react';
import { SettingsScreen } from '../SettingsScreen';
import { useSettingsStore } from '../../stores/useSettingsStore';

jest.mock('../../stores/useSettingsStore', () => ({
  useSettingsStore: jest.fn(),
}));

jest.mock('../../stores/useUpdateStore', () => ({
  useUpdateStore: jest.fn(() => ({
    info: null,
    isChecking: false,
    isInstalling: false,
    checkForUpdates: jest.fn(),
    installUpdate: jest.fn(),
  })),
}));

jest.mock('../../stores/useLicenseStore', () => ({
  useLicenseStore: jest.fn(() => ({
    status: null,
    isLoading: false,
    error: null,
    loadStatus: jest.fn(),
    activate: jest.fn(),
    deactivate: jest.fn(),
  })),
}));

describe('SettingsScreen', () => {
  const mockUpdateSettings = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useSettingsStore as unknown as jest.Mock).mockReturnValue({
      settings: {
        theme: 'dark',
        language: 'en',
        logLevel: 'info',
        autoDetectDevice: true,
        confirmDangerousActions: true,
        showRiskBadges: true,
        autoCheckUpdates: true,
        sendAnonymousDiagnostics: false,
        exportPath: null,
      },
      updateSettings: mockUpdateSettings,
    });
  });

  it('renders General Settings tab by default', () => {
    render(<SettingsScreen />);
    expect(screen.getByText('Appearance & Behavior')).toBeInTheDocument();
  });

  it('switches to License tab', () => {
    render(<SettingsScreen />);
    fireEvent.click(screen.getByText('License & Access'));
    expect(screen.getByText('License Status')).toBeInTheDocument();
  });
});
