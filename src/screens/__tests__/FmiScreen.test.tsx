import { render, screen, fireEvent } from '@testing-library/react';
import { FmiScreen } from '../FmiScreen';

const mockStartSession = jest.fn();

jest.mock('../../stores/useOperationSessionStore', () => ({
  useOperationSessionStore: jest.fn((selector) => {
    const state = {
      startSession: mockStartSession,
      isStarting: false,
    };
    return selector(state);
  }),
}));

jest.mock('../../stores/useAppStore', () => ({
  useAppStore: jest.fn(() => ({
    id: 'mock-udid-123',
    model: 'iPhone 12',
  })),
}));

describe('FmiScreen - Strict UI Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders the FMI/Account header correctly', () => {
    render(<FmiScreen />);
    expect(screen.getByText('FMI / Account')).toBeInTheDocument();
  });

  it('renders the Token Extractor action card', () => {
    render(<FmiScreen />);
    expect(screen.getByText('Extract Auth Token')).toBeInTheDocument();
  });

  it('dispatches correct IPC command when Token Extractor is clicked', async () => {
    render(<FmiScreen />);
    const button = screen.getByText('Extract Auth Token');
    fireEvent.click(button);
    expect(mockStartSession).toHaveBeenCalledWith({ customCommand: 'extract_token' });
  });
});
