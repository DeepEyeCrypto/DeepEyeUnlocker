import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { EdlScreen } from '../EdlScreen';
import { invoke } from '@tauri-apps/api/core';
import { useAppStore } from '../../stores/useAppStore';

jest.mock('@tauri-apps/api/core', () => ({
  invoke: jest.fn(),
}));

jest.mock('../../stores/useAppStore', () => ({
  useAppStore: jest.fn(),
}));

describe('EdlScreen UI Tests', () => {
  const mockAppendLog = jest.fn();
  const mockStartOperation = jest.fn();
  const mockEndOperation = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    (useAppStore as unknown as jest.Mock).mockReturnValue({
      appendLog: mockAppendLog,
      startOperation: mockStartOperation,
      endOperation: mockEndOperation,
      activeOperation: null,
    });
  });

  it('renders Qualcomm EDL header', () => {
    render(<EdlScreen />);
    expect(screen.getByText('Qualcomm EDL')).toBeInTheDocument();
    expect(screen.getByText(/Emergency Download Mode/)).toBeInTheDocument();
  });

  it('dispatches detect device IPC call', async () => {
    (invoke as jest.Mock).mockResolvedValue({
      detected: true,
      chipset: 'MSM8998',
      serial: '0x123456',
      mode: '9008',
    });

    render(<EdlScreen />);

    const detectBtn = screen.getByText('Detect 9008');
    fireEvent.click(detectBtn);

    expect(mockStartOperation).toHaveBeenCalledWith('edl_detect');

    await waitFor(() => {
      expect(invoke).toHaveBeenCalledWith('edl_detect_device');
    });

    await waitFor(() => {
      expect(mockEndOperation).toHaveBeenCalled();
    });
  });

  it('verifies partition target fields', () => {
    render(<EdlScreen />);
    expect(screen.getByText('Partition Target')).toBeInTheDocument();

    // Check if the partition select box has default 'boot'
    const select = screen.getByRole('combobox');
    expect(select).toHaveValue('boot');
  });
});
