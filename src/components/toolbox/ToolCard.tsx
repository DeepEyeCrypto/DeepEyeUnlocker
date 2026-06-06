import React, { useState } from 'react';
import { OperationType } from '../../lib/session-types';
import { DeviceMode } from '../../lib/device-types';

export interface ToolCardProps {
  title: string;
  description: string;
  operationType: OperationType;
  requiredMode: DeviceMode | DeviceMode[] | 'any';
  riskLevel: 'none' | 'low' | 'medium' | 'high';
  requiresConfirm: boolean;
  confirmMessage?: string;
  disabled?: boolean;
  disabledReason?: string;
  customAction?: () => void;
  icon: React.ReactNode;
}

export function ToolCard(props: ToolCardProps) {
  const [confirmOpen, setConfirmOpen] = useState(false);

  const handleAction = () => {
    if (props.disabled) return;
    if (props.requiresConfirm && !confirmOpen) {
      setConfirmOpen(true);
      return;
    }
    if (props.customAction) {
      props.customAction();
    }
    setConfirmOpen(false);
  };

  const getRiskStyles = () => {
    switch (props.riskLevel) {
      case 'none':
        return { text: 'SAFE', colorClass: 'text-green-400 bg-green-500/10 border-green-500/20' };
      case 'low':
        return { text: 'LOW', colorClass: 'text-blue-400 bg-blue-500/10 border-blue-500/20' };
      case 'medium':
        return {
          text: 'CAUTION',
          colorClass: 'text-yellow-400 bg-yellow-500/10 border-yellow-500/20',
        };
      case 'high':
        return { text: 'DANGER', colorClass: 'text-red-400 bg-red-500/10 border-red-500/20' };
    }
  };

  const risk = getRiskStyles();
  const highRiskBase =
    props.riskLevel === 'high' ? 'border-red-500/30 bg-red-500/5' : 'border-white/10 bg-white/5';

  return (
    <div
      className={`relative flex flex-col p-5 rounded-2xl backdrop-blur-xl shadow-sm transition-all duration-180 border ${highRiskBase} ${props.disabled ? 'opacity-60 cursor-not-allowed' : 'hover:shadow-md hover:-translate-y-[1px]'}`}
    >
      <div className="flex items-start justify-between mb-3">
        <div className="p-2.5 bg-white/5 rounded-xl border border-white/10 shadow-inner">
          {props.icon}
        </div>
        <span
          className={`px-2.5 py-1 text-[10px] font-bold uppercase tracking-widest border rounded-full ${risk.colorClass}`}
        >
          {risk.text}
        </span>
      </div>

      <div className="flex-1 mb-4">
        <h3 className="text-base font-bold text-white mb-1.5 tracking-tight">{props.title}</h3>
        <p className="text-xs text-gray-400 leading-relaxed line-clamp-2" title={props.description}>
          {props.description}
        </p>
      </div>

      <div className="mb-4">
        <span className="text-[10px] text-gray-500 uppercase tracking-widest font-semibold block">
          Required:{' '}
          {props.requiredMode === 'any'
            ? 'Any connected'
            : Array.isArray(props.requiredMode)
              ? props.requiredMode.join(' / ')
              : props.requiredMode}
        </span>
      </div>

      {confirmOpen ? (
        <div className="p-3 bg-gray-900 border border-gray-700 rounded-lg animate-in fade-in slide-in-from-top-2 duration-200">
          <p className="text-xs text-gray-300 mb-3 leading-relaxed">
            <span className="text-yellow-400 mr-1">⚠️</span>
            {props.confirmMessage || 'Are you sure you want to proceed?'}
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setConfirmOpen(false)}
              className="flex-1 py-1.5 bg-gray-800 hover:bg-gray-700 text-gray-300 text-xs font-semibold rounded transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleAction}
              className="flex-1 py-1.5 bg-red-500/20 hover:bg-red-500/30 text-red-400 border border-red-500/20 text-xs font-semibold rounded transition-colors"
            >
              Confirm →
            </button>
          </div>
        </div>
      ) : (
        <button
          onClick={handleAction}
          disabled={props.disabled}
          title={props.disabled ? props.disabledReason : undefined}
          className={`w-full py-2.5 rounded-lg text-sm font-semibold transition-colors border ${
            props.disabled
              ? 'bg-white/5 text-gray-500 border-transparent cursor-not-allowed'
              : 'bg-white/10 hover:bg-white/15 text-white border-white/5'
          }`}
        >
          {props.disabled ? props.disabledReason || 'Disabled' : 'Run →'}
        </button>
      )}
    </div>
  );
}
