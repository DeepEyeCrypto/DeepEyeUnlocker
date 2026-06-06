import { useState, useCallback, useRef } from 'react';
import { invoke } from '@tauri-apps/api/core';
import { z } from 'zod';
import {
  Smartphone,
  Cpu,
  HardDrive,
  Unlock,
  CheckCircle2,
  AlertTriangle,
  Loader2,
  Terminal,
} from 'lucide-react';

// ── Tokens ────────────────────────────────────────────────────────
const T = {
  bg: '#080808',
  card: '#0E0E0E',
  raised: '#141414',
  border: '#1A1A1A',
  cyan: '#00BCD4',
  green: '#00E676',
  orange: '#FF9800',
  red: '#FF1744',
  dim: '#444',
  mid: '#666',
  text: '#E0E0E0',
  font: '"JetBrains Mono", monospace',
} as const;

// ── Zod schemas ───────────────────────────────────────────────────
const EventSchema = z.object({
  event: z.string(),
  msg: z.string().optional(),
  pct: z.number().min(0).max(100).optional(),
  phase: z.string().optional(),
  chip_name: z.string().optional(),
  ios: z.string().optional(),
  cpid: z.string().optional(),
  is_checkm8: z.boolean().optional(),
  signal: z.boolean().optional(),
  untethered: z.boolean().optional(),
  method: z.string().optional(),
  reason: z.string().optional(),
  layer: z.string().optional(),
  retryable: z.boolean().optional(),
  notes: z.array(z.string()).optional(),
});
type DeepEvent = z.infer<typeof EventSchema>;

// ── Step definitions ──────────────────────────────────────────────
const STEPS = [
  { id: 'detect', label: 'Detect', icon: <Smartphone size={11} /> },
  { id: 'dfu', label: 'DFU', icon: <Cpu size={11} /> },
  { id: 'checkm8', label: 'checkm8', icon: <Terminal size={11} /> },
  { id: 'ramdisk', label: 'Ramdisk', icon: <HardDrive size={11} /> },
  { id: 'bypass', label: 'Bypass', icon: <Unlock size={11} /> },
  { id: 'done', label: 'Done', icon: <CheckCircle2 size={11} /> },
];

type Phase = 'idle' | 'detect' | 'dfu' | 'checkm8' | 'ramdisk' | 'bypass' | 'done' | 'error';

const STEP_IDX: Partial<Record<Phase, number>> = {
  detect: 0,
  dfu: 1,
  checkm8: 2,
  ramdisk: 3,
  bypass: 4,
  done: 5,
};

// ── Device info ───────────────────────────────────────────────────
interface DeviceInfo {
  chipName: string;
  ios: string;
  cpid: string;
  isCheckm8: boolean;
}

interface BypassResult {
  method: string;
  signal: boolean;
  untethered: boolean;
  notes: string[];
}

// ── Component ─────────────────────────────────────────────────────
export default function F3arRa1nPanel(): React.ReactElement {
  const [phase, setPhase] = useState<Phase>('idle');
  const [device, setDevice] = useState<DeviceInfo | null>(null);
  const [result, setResult] = useState<BypassResult | null>(null);
  const [pct, setPct] = useState(0);
  const [msg, setMsg] = useState('');
  const [log, setLog] = useState<DeepEvent[]>([]);
  const [error, setError] = useState<string | null>(null);
  const logRef = useRef<HTMLDivElement>(null);

  const sid = () => crypto.randomUUID();
  const push = (ev: DeepEvent) => setLog((l) => [ev, ...l].slice(0, 200));

  const handle = useCallback((raw: unknown[]) => {
    for (const item of raw) {
      const p = EventSchema.safeParse(item);
      if (!p.success) continue;
      const ev = p.data;
      push(ev);
      if (ev.pct !== undefined) setPct(ev.pct);
      if (ev.phase) setMsg(ev.phase);

      switch (ev.event) {
        case 'device_found':
          setDevice({
            chipName: ev.chip_name ?? '',
            ios: ev.ios ?? '',
            cpid: ev.cpid ?? '',
            isCheckm8: ev.is_checkm8 ?? false,
          });
          break;
        case 'dfu_ok':
          setPhase('dfu');
          break;
        case 'checkm8_ok':
          setPhase('checkm8');
          break;
        case 'ramdisk_ok':
          setPhase('ramdisk');
          break;
        case 'activation_ok':
        case 'activation_partial':
          setPhase('bypass');
          break;
        case 'bypass_complete':
          setResult({
            method: ev.method ?? '',
            signal: ev.signal ?? false,
            untethered: ev.untethered ?? false,
            notes: ev.notes ?? [],
          });
          setPhase('done');
          setPct(100);
          break;
        case 'error':
          setError(`[${ev.layer ?? 'ERROR'}] ${ev.reason ?? 'Unknown'}`);
          setPhase('error');
          break;
      }
    }
  }, []);

  const run = useCallback(async () => {
    setPhase('detect');
    setError(null);
    setResult(null);
    setLog([]);
    setPct(0);
    setMsg('Starting F3arRa1n chain...');
    try {
      const raw = await invoke<unknown[]>('f3arrain_full', { sessionId: sid() });
      handle(raw);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setPhase('error');
    }
  }, [handle]);

  const reset = () => {
    setPhase('idle');
    setDevice(null);
    setResult(null);
    setLog([]);
    setPct(0);
    setError(null);
    setMsg('');
  };

  const activeIdx = STEP_IDX[phase] ?? -1;
  const running = !['idle', 'done', 'error'].includes(phase);

  return (
    <div
      style={{
        fontFamily: T.font,
        background: T.bg,
        borderRadius: 16,
        border: `1px solid ${T.border}`,
        overflow: 'hidden',
        minWidth: 400,
      }}
    >
      {/* Header */}
      <div
        style={{
          background: '#050505',
          padding: '13px 20px',
          borderBottom: `1px solid ${T.border}`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
        }}
      >
        <div>
          <div style={{ color: T.cyan, fontSize: 11, letterSpacing: 3 }}>⚡ F3ARRA1N BYPASS</div>
          <div style={{ color: T.mid, fontSize: 9, marginTop: 2, letterSpacing: 1 }}>
            checkm8 · A7–A11 · iOS 12–16.7.8 · FREE
          </div>
        </div>
        {running && (
          <Loader2 size={13} color={T.cyan} style={{ animation: 'spin 1s linear infinite' }} />
        )}
      </div>

      <div style={{ padding: 20 }}>
        {/* Device card */}
        {device ? (
          <div
            style={{
              background: '#111',
              borderRadius: 10,
              padding: '12px 14px',
              border: `1px solid ${T.cyan}33`,
              marginBottom: 14,
            }}
          >
            <div style={{ color: T.text, fontSize: 12, fontWeight: 'bold' }}>{device.chipName}</div>
            <div style={{ color: T.mid, fontSize: 10, marginTop: 3 }}>
              iOS {device.ios} · CPID {device.cpid}
            </div>
            <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
              {device.isCheckm8 ? (
                <Chip text="checkm8 ✓" color={T.green} />
              ) : (
                <Chip text="NOT VULNERABLE" color={T.red} />
              )}
              <Chip text="A7–A11 ONLY" color={T.cyan} />
            </div>
          </div>
        ) : (
          <div
            style={{
              background: '#0A0A0A',
              borderRadius: 10,
              padding: 14,
              border: `1px dashed ${T.border}`,
              marginBottom: 14,
              textAlign: 'center',
            }}
          >
            <Smartphone size={18} color={T.dim} style={{ margin: '0 auto 6px' }} />
            <div style={{ color: T.dim, fontSize: 11 }}>
              Connect iPhone — USB — DFU or Normal mode
            </div>
          </div>
        )}

        {/* Step pipeline */}
        <div style={{ display: 'flex', gap: 4, marginBottom: 12, overflowX: 'auto' }}>
          {STEPS.map((s, i) => {
            const done = phase === 'done' || i < activeIdx;
            const active = i === activeIdx && running;
            const clr = done ? T.green : active ? T.cyan : T.dim;
            return (
              <div
                key={s.id}
                style={{
                  background: done ? `${T.green}12` : active ? `${T.cyan}12` : '#0A0A0A',
                  border: `1px solid ${clr}44`,
                  borderRadius: 6,
                  padding: '4px 10px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 5,
                  flexShrink: 0,
                }}
              >
                <span style={{ color: clr }}>{s.icon}</span>
                <span style={{ color: clr, fontSize: 9, letterSpacing: 1 }}>{s.label}</span>
                {done && <CheckCircle2 size={8} color={T.green} />}
                {active && (
                  <Loader2
                    size={8}
                    color={T.cyan}
                    style={{ animation: 'spin 1s linear infinite' }}
                  />
                )}
              </div>
            );
          })}
        </div>

        {/* Progress bar */}
        {running && (
          <div style={{ marginBottom: 12 }}>
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                marginBottom: 4,
              }}
            >
              <span style={{ color: T.mid, fontSize: 9 }}>{msg}</span>
              <span style={{ color: T.cyan, fontSize: 9 }}>{pct}%</span>
            </div>
            <div style={{ background: T.raised, borderRadius: 3, height: 3 }}>
              <div
                style={{
                  background: T.cyan,
                  borderRadius: 3,
                  height: 3,
                  width: `${pct}%`,
                  transition: 'width 0.4s ease',
                  boxShadow: `0 0 8px ${T.cyan}55`,
                }}
              />
            </div>
          </div>
        )}

        {/* Result */}
        {phase === 'done' && result && (
          <div
            style={{
              background: `${T.green}08`,
              border: `1px solid ${T.green}25`,
              borderRadius: 10,
              padding: 14,
              marginBottom: 14,
            }}
          >
            <div
              style={{
                color: T.green,
                fontSize: 12,
                fontWeight: 'bold',
                marginBottom: 8,
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <CheckCircle2 size={14} /> Hello Screen Bypassed ✓
            </div>
            {result.notes.map((note, i) => (
              <div key={i} style={{ color: T.mid, fontSize: 9, lineHeight: 2 }}>
                • {note}
              </div>
            ))}
          </div>
        )}

        {/* Error */}
        {phase === 'error' && error && (
          <div
            style={{
              background: `${T.red}08`,
              border: `1px solid ${T.red}30`,
              borderRadius: 8,
              padding: 12,
              marginBottom: 12,
              display: 'flex',
              gap: 8,
            }}
          >
            <AlertTriangle size={12} color={T.red} style={{ flexShrink: 0 }} />
            <div style={{ color: T.red, fontSize: 10, lineHeight: 1.7 }}>{error}</div>
          </div>
        )}

        {/* Actions */}
        <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
          {!running && phase !== 'done' && (
            <Btn
              label={phase === 'error' ? '↻ RETRY' : '▶ START F3ARRA1N'}
              color={T.cyan}
              onClick={run}
            />
          )}
          {(phase === 'done' || phase === 'error') && (
            <Btn label="RESET" color={T.dim} small onClick={reset} />
          )}
        </div>

        {/* Log */}
        {log.length > 0 && (
          <div
            ref={logRef}
            style={{
              background: '#050505',
              borderRadius: 8,
              padding: '10px 12px',
              maxHeight: 160,
              overflow: 'auto',
              border: `1px solid ${T.border}`,
            }}
          >
            {log.map((ev, i) => (
              <div
                key={i}
                style={{
                  fontSize: 9,
                  lineHeight: 1.9,
                  fontFamily: T.font,
                  color:
                    ev.event === 'error'
                      ? T.red
                      : ev.event === 'bypass_complete'
                        ? T.green
                        : ev.event === 'checkm8_ok'
                          ? T.cyan
                          : ev.event.includes('ok') || ev.event.includes('complete')
                            ? T.green
                            : T.mid,
                }}
              >
                {ev.event === 'progress'
                  ? `  ${ev.pct}% — ${ev.phase}`
                  : ev.msg
                    ? `[${ev.event}] ${ev.msg}`
                    : `[${ev.event}]`}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function Chip({ text, color }: { text: string; color: string }) {
  return (
    <span
      style={{
        background: `${color}18`,
        color,
        border: `1px solid ${color}40`,
        borderRadius: 3,
        padding: '1px 6px',
        fontSize: 8,
        letterSpacing: 1,
      }}
    >
      {text}
    </span>
  );
}

function Btn({
  label,
  color,
  onClick,
  small,
}: {
  label: string;
  color: string;
  onClick: () => void;
  small?: boolean;
}) {
  return (
    <button
      onClick={onClick}
      style={{
        background: `${color}18`,
        color,
        border: `1px solid ${color}55`,
        borderRadius: 8,
        cursor: 'pointer',
        padding: small ? '6px 12px' : '9px 20px',
        fontSize: small ? 9 : 11,
        fontFamily: '"JetBrains Mono", monospace',
        letterSpacing: 1,
        fontWeight: 'bold',
      }}
    >
      {label}
    </button>
  );
}
