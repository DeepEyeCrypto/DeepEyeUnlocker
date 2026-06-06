import { render, screen, fireEvent } from '@testing-library/react';
import { ToolCard } from '../ToolCard';

describe('ToolCard', () => {
  it('Test 25: ToolCard_renders_disabled_with_reason', () => {
    render(
      <ToolCard
        title="Test Tool"
        description="A test tool"
        operationType="DeviceCheck"
        requiredMode="recovery"
        riskLevel="none"
        requiresConfirm={false}
        disabled={true}
        disabledReason="Needs recovery mode"
        icon={<div data-testid="icon" />}
      />,
    );

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    expect(button).toHaveTextContent('Needs recovery mode');
  });

  it('Renders normally and triggers action', () => {
    const mockAction = jest.fn();
    render(
      <ToolCard
        title="Test Tool"
        description="A test tool"
        operationType="DeviceCheck"
        requiredMode="any"
        riskLevel="none"
        requiresConfirm={false}
        customAction={mockAction}
        icon={<div data-testid="icon" />}
      />,
    );

    const button = screen.getByRole('button');
    expect(button).toBeEnabled();
    expect(button).toHaveTextContent('Run →');

    fireEvent.click(button);
    expect(mockAction).toHaveBeenCalled();
  });

  it('Shows confirm dialog for dangerous operations', () => {
    const mockAction = jest.fn();
    render(
      <ToolCard
        title="Test Tool"
        description="A test tool"
        operationType="DeviceCheck"
        requiredMode="any"
        riskLevel="high"
        requiresConfirm={true}
        confirmMessage="DANGER ZONE"
        customAction={mockAction}
        icon={<div data-testid="icon" />}
      />,
    );

    const runButton = screen.getByRole('button');
    fireEvent.click(runButton);

    // confirm panel opens
    expect(screen.getByText(/DANGER ZONE/i)).toBeInTheDocument();
    const confirmBtn = screen.getByRole('button', { name: /Confirm/i });
    expect(mockAction).not.toHaveBeenCalled();

    fireEvent.click(confirmBtn);
    expect(mockAction).toHaveBeenCalled();
  });
});
