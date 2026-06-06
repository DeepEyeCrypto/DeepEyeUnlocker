import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ToolboxScreen } from '../ToolboxScreen';
import { useDeviceStatusStore } from '../../stores/useDeviceStatusStore';
import { useOperationSessionStore } from '../../stores/useOperationSessionStore';

// Mock stores
jest.mock('../../stores/useDeviceStatusStore', () => ({
  useDeviceStatusStore: jest.fn(),
}));
jest.mock('../../stores/useOperationSessionStore', () => ({
  useOperationSessionStore: jest.fn(),
}));

const mockDeviceStatusStore = useDeviceStatusStore as unknown as jest.Mock;
const mockSessionStore = useOperationSessionStore as unknown as jest.Mock;

describe('ToolboxScreen - Strict UI Tests', () => {
  const mockStartSession = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    mockSessionStore.mockImplementation((selector) =>
      selector({
        activeSession: null,
        startSession: mockStartSession,
      }),
    );
  });

  it('renders the ToolboxScreen header and No Device Banner', () => {
    mockDeviceStatusStore.mockImplementation((selector) => selector({ currentDevice: null }));

    render(
      <MemoryRouter>
        <ToolboxScreen />
      </MemoryRouter>,
    );
    expect(screen.getByText('Toolbox')).toBeInTheDocument();
    expect(screen.getByText(/Advanced diagnostic/)).toBeInTheDocument();
    expect(screen.getAllByText('No device connected').length).toBeGreaterThan(0);
  });

  it('renders device tools when device is connected', () => {
    mockDeviceStatusStore.mockImplementation((selector) =>
      selector({
        currentDevice: {
          id: '123',
          connectionState: 'connected',
          platform: 'ios',
          mode: 'normal',
          model: 'iPhone 14',
          osVersion: '16.0',
          capabilityFlags: ['canEnterRecovery'],
        },
      }),
    );

    render(
      <MemoryRouter>
        <ToolboxScreen />
      </MemoryRouter>,
    );

    expect(screen.getAllByText('iPhone 14').length).toBeGreaterThan(0);
    expect(screen.getByText('DFU Assistant')).toBeInTheDocument();
    expect(screen.queryByText('No device connected')).not.toBeInTheDocument();
  });

  it('launches DFU wizard when DFU Assistant is clicked', () => {
    mockDeviceStatusStore.mockImplementation((selector) =>
      selector({
        currentDevice: {
          id: '123',
          connectionState: 'connected',
          platform: 'ios',
          mode: 'normal',
          model: 'iPhone 14',
        },
      }),
    );

    render(
      <MemoryRouter>
        <ToolboxScreen />
      </MemoryRouter>,
    );

    // Find the button within the DFU assistant card that says "Run →" but DFU assistant uses a custom button "Run →"
    // Actually the DFU Assistant card has `customAction={() => setWizardOpen(true)}`
    // And its button text might be "Run →" or something.
    // By default ToolCard uses "Run →" if disabled=false.

    const toolCards = screen.getAllByRole('button', { name: 'Run →' });
    // Click the first one which should be DFU Assistant
    fireEvent.click(toolCards[0]);

    // Wait for DFU Wizard to show.
    // Text from DFU Wizard: "Step 1 of 3"
    expect(screen.getByText(/Step 1 of 3/)).toBeInTheDocument();
  });
});
