import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  createStableId,
  fetchConnectedDevices,
  type ConnectedDevice,
} from "../lib/devices";

export interface QueuedOperation<TParams extends Record<string, unknown>> {
  id: string;
  name: string;
  params: TParams;
  queuedAt: number;
}

type UseWaitQueueOptions<TParams extends Record<string, unknown>> = {
  onExecute: (device: ConnectedDevice, operation: QueuedOperation<TParams>) => Promise<void>;
  onQueued?: (operation: QueuedOperation<TParams>) => void;
  onDeviceDetected?: (device: ConnectedDevice, operation: QueuedOperation<TParams>) => void;
  onError?: (message: string, operation: QueuedOperation<TParams> | null) => void;
  isDeviceCompatible?: (
    device: ConnectedDevice,
    operation: QueuedOperation<TParams>,
  ) => boolean;
  pollIntervalMs?: number;
};

export function useWaitQueue<TParams extends Record<string, unknown>>({
  onExecute,
  onQueued,
  onDeviceDetected,
  onError,
  isDeviceCompatible,
  pollIntervalMs = 2000,
}: UseWaitQueueOptions<TParams>) {
  const [pendingQueue, setPendingQueue] = useState<QueuedOperation<TParams>[]>([]);
  const [isWaitingForDevice, setIsWaitingForDevice] = useState(false);
  const [now, setNow] = useState(Date.now());
  const queueRef = useRef<QueuedOperation<TParams>[]>([]);
  const executingRef = useRef(false);

  useEffect(() => {
    queueRef.current = pendingQueue;
  }, [pendingQueue]);

  const findCompatibleDevice = useCallback(
    (devices: ConnectedDevice[], operation: QueuedOperation<TParams>) => {
      if (!isDeviceCompatible) {
        return devices[0] ?? null;
      }
      return devices.find((device) => isDeviceCompatible(device, operation)) ?? null;
    },
    [isDeviceCompatible],
  );

  const cancelQueuedOperation = useCallback((operationId: string) => {
    setPendingQueue((previous) => {
      const nextQueue = previous.filter((operation) => operation.id !== operationId);
      if (nextQueue.length === 0) {
        setIsWaitingForDevice(false);
      }
      return nextQueue;
    });
  }, []);

  const enqueueOrRunOperation = useCallback(
    async (name: string, params: TParams) => {
      const operation: QueuedOperation<TParams> = {
        id: createStableId("queue"),
        name,
        params,
        queuedAt: Date.now(),
      };

      try {
        const devices = await fetchConnectedDevices();
        const compatibleDevice = findCompatibleDevice(devices, operation);
        if (compatibleDevice) {
          await onExecute(compatibleDevice, operation);
          return { queued: false, operation };
        }
      } catch (queueError: unknown) {
        onError?.(String(queueError), operation);
        return { queued: false, operation, error: String(queueError) };
      }

      setPendingQueue((previous) => [...previous, operation]);
      setNow(Date.now());
      setIsWaitingForDevice(true);
      onQueued?.(operation);
      return { queued: true, operation };
    },
    [findCompatibleDevice, onError, onExecute, onQueued],
  );

  useEffect(() => {
    if (!isWaitingForDevice) {
      return;
    }

    const ticker = window.setInterval(() => {
      setNow(Date.now());
    }, 1000);

    return () => window.clearInterval(ticker);
  }, [isWaitingForDevice]);

  useEffect(() => {
    if (!isWaitingForDevice) {
      return;
    }

    const interval = window.setInterval(async () => {
      if (executingRef.current) {
        return;
      }

      const nextOperation = queueRef.current[0] ?? null;
      if (!nextOperation) {
        setIsWaitingForDevice(false);
        return;
      }

      try {
        const devices = await fetchConnectedDevices();
        const compatibleDevice = findCompatibleDevice(devices, nextOperation);
        if (!compatibleDevice) {
          return;
        }

        executingRef.current = true;
        setIsWaitingForDevice(false);
        onDeviceDetected?.(compatibleDevice, nextOperation);
        await onExecute(compatibleDevice, nextOperation);
      } catch (queueError: unknown) {
        onError?.(String(queueError), nextOperation);
      } finally {
        setPendingQueue((previous) => {
          const nextQueue = previous.filter((operation) => operation.id !== nextOperation.id);
          if (nextQueue.length > 0) {
            setIsWaitingForDevice(true);
          }
          return nextQueue;
        });
        executingRef.current = false;
      }
    }, pollIntervalMs);

    return () => window.clearInterval(interval);
  }, [
    findCompatibleDevice,
    isWaitingForDevice,
    onDeviceDetected,
    onError,
    onExecute,
    pollIntervalMs,
  ]);

  const waitingOperation = useMemo(() => pendingQueue[0] ?? null, [pendingQueue]);
  const waitingElapsedMs = waitingOperation ? now - waitingOperation.queuedAt : 0;

  return {
    pendingQueue,
    isWaitingForDevice,
    waitingOperation,
    waitingElapsedMs,
    enqueueOrRunOperation,
    cancelQueuedOperation,
  };
}
