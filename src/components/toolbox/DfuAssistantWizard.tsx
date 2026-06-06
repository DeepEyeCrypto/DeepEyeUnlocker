import { useEffect, useState, useRef } from 'react';
import { useDeviceStatusStore } from '../../stores/useDeviceStatusStore';
import { DfuPhoneSvg, ButtonType } from './DfuPhoneSvg';

interface Props {
  deviceModel: string;
  onSuccess: () => void;
  onCancel: () => void;
}

export function DfuAssistantWizard({ deviceModel, onSuccess, onCancel }: Props) {
  const [currentStep, setCurrentStep] = useState<1 | 2 | 3>(1);
  const [countdown, setCountdown] = useState(0);
  const [maxCountdown, setMaxCountdown] = useState(1);
  const [detectionStatus, setDetectionStatus] = useState<
    'waiting' | 'detecting' | 'confirmed' | 'failed'
  >('waiting');

  const timerRef = useRef<NodeJS.Timeout>();
  const pollingRef = useRef<NodeJS.Timeout>();

  // Determine variant based on model
  let variant: 'modern' | 'seven' | 'legacy' = 'modern';
  const m = deviceModel.toLowerCase();
  if (
    m.includes('iphone 6') ||
    m.includes('iphone 5') ||
    (m.includes('se') && !m.includes('2020'))
  ) {
    variant = 'legacy';
  } else if (m.includes('iphone 7')) {
    variant = 'seven';
  }

  // Get buttons for step
  const getButtons = (step: number): ButtonType[] => {
    if (step === 3) return [];
    if (variant === 'modern') return step === 1 ? ['volumeDown', 'side'] : ['volumeDown'];
    if (variant === 'seven') return step === 1 ? ['volumeDown', 'sleepWake'] : ['volumeDown'];
    return step === 1 ? ['home', 'sleepWake'] : ['home'];
  };

  const startTimer = (seconds: number, onComplete: () => void) => {
    if (timerRef.current) clearInterval(timerRef.current);
    setCountdown(seconds);
    setMaxCountdown(seconds);

    timerRef.current = setInterval(() => {
      setCountdown((c) => {
        if (c <= 1) {
          clearInterval(timerRef.current);
          onComplete();
          return 0;
        }
        return c - 1;
      });
    }, 1000);
  };

  // Step logic
  const handleStartStep1 = () => {
    // 3 seconds initial hold
    startTimer(3, () => {
      // Step 1 done, move to step 2 manually or automatically? The prompt says "[Continue] button appears after 3s".
      // Wait, let's just make it auto transition to 2, or show a button. We will show a "Next" button.
    });
  };

  const handleStartStep2 = () => {
    setCurrentStep(2);
    // 10 seconds hold
    startTimer(10, () => {
      setCurrentStep(3);
      startDetection();
    });
  };

  const startDetection = () => {
    setDetectionStatus('detecting');
    if (pollingRef.current) clearInterval(pollingRef.current);

    let attempts = 0;
    pollingRef.current = setInterval(() => {
      attempts++;
      // Check store
      const mode = useDeviceStatusStore.getState().currentDevice?.mode;
      if (mode === 'dfu') {
        clearInterval(pollingRef.current);
        setDetectionStatus('confirmed');
        setTimeout(onSuccess, 1500); // Close after brief success message
      } else if (attempts >= 30) {
        // 15 seconds (500ms intervals)
        clearInterval(pollingRef.current);
        setDetectionStatus('failed');
      }
    }, 500);
  };

  useEffect(() => {
    handleStartStep1();
    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
      if (pollingRef.current) clearInterval(pollingRef.current);
    };
  }, []);

  const progressPct = Math.max(0, 100 - (countdown / maxCountdown) * 100);

  return (
    <div className="fixed inset-0 bg-black/60 backdrop-blur-md z-50 flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-gray-900 border border-gray-700 rounded-2xl p-8 shadow-2xl flex flex-col items-center animate-in zoom-in-95 duration-200">
        <h2 className="text-xl font-bold text-white mb-1">DFU Assistant</h2>
        <p className="text-sm text-gray-400 mb-6">{deviceModel}</p>

        <div className="text-xs font-bold text-blue-400 tracking-widest uppercase mb-4">
          Step {currentStep} of 3
        </div>

        <DfuPhoneSvg variant={variant} highlightedButtons={getButtons(currentStep)} />

        <div className="h-20 flex items-center justify-center text-center mt-6 mb-4">
          {currentStep === 1 && (
            <p className="text-gray-300 font-medium leading-relaxed">
              Hold <strong className="text-white">Volume Down</strong> +{' '}
              <strong className="text-white">Side button</strong> for 3 seconds
            </p>
          )}
          {currentStep === 2 && (
            <p className="text-gray-300 font-medium leading-relaxed">
              Release the Side button now
              <br />
              Keep holding Volume Down
            </p>
          )}
          {currentStep === 3 && (
            <p className="text-gray-300 font-medium leading-relaxed">
              {detectionStatus === 'detecting' && 'Release all buttons. Detecting device...'}
              {detectionStatus === 'confirmed' && (
                <span className="text-green-400">DFU Mode detected successfully!</span>
              )}
              {detectionStatus === 'failed' && (
                <span className="text-red-400">Failed to detect DFU mode.</span>
              )}
            </p>
          )}
        </div>

        {/* Progress bar area */}
        {currentStep < 3 && (
          <div className="w-full mb-6">
            <div className="flex justify-between text-xs text-gray-500 font-medium mb-2">
              <span>Time remaining</span>
              <span>{countdown}s</span>
            </div>
            <div className="w-full height-1.5 bg-gray-800 rounded-full overflow-hidden">
              <div
                className="h-1.5 bg-blue-500 rounded-full transition-all duration-1000 ease-linear"
                style={{ width: `${progressPct}%` }}
              />
            </div>
          </div>
        )}

        {detectionStatus === 'detecting' && (
          <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin mb-6" />
        )}

        <div className="w-full flex gap-3 mt-2">
          {currentStep === 1 && countdown === 0 && (
            <button
              onClick={handleStartStep2}
              className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-lg transition-colors"
            >
              Continue →
            </button>
          )}
          {detectionStatus === 'failed' && (
            <button
              onClick={() => {
                setCurrentStep(1);
                setDetectionStatus('waiting');
                handleStartStep1();
              }}
              className="flex-1 py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-semibold rounded-lg transition-colors"
            >
              Retry
            </button>
          )}
          <button
            onClick={onCancel}
            className="flex-1 py-2.5 bg-white/5 hover:bg-white/10 text-white font-semibold rounded-lg transition-colors border border-white/10"
          >
            Cancel
          </button>
        </div>
      </div>
    </div>
  );
}
