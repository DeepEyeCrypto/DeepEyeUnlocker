import { useNavigate } from 'react-router-dom';

export function ToolboxNoDeviceBanner() {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center p-8 bg-white/5 border border-white/10 rounded-2xl mb-8 backdrop-blur-xl">
      <div className="w-16 h-16 bg-white/5 rounded-full flex items-center justify-center mb-4">
        <span className="text-2xl">🔌</span>
      </div>
      <h3 className="text-xl font-bold text-white mb-2">No device connected</h3>
      <p className="text-gray-400 mb-6 text-center max-w-md">
        Connect an iOS device to enable the toolbox features. The tools below are disabled until a
        valid device is detected.
      </p>
      <button
        onClick={() => navigate('/')}
        className="px-6 py-2 bg-blue-500/20 text-blue-400 hover:bg-blue-500/30 font-semibold rounded-lg transition-colors border border-blue-500/30"
      >
        Go to Dashboard →
      </button>
    </div>
  );
}
