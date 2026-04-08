import { useCallback, useEffect, useMemo, useState } from "react";
import {
  addRomToQueue,
  clearRomQueue,
  getRomQueue,
  moveRomQueueItem,
  removeRomFromQueue,
  selectRomFile,
  toggleRomQueuePartition,
  type QueueItem,
} from "../lib/rom_manager";

function normalizeZipPaths(paths: string[]): string[] {
  return [...new Set(paths.map((path) => path.trim()).filter((path) => path.toLowerCase().endsWith(".zip")))];
}

function resolveSelectedQueueId(
  preferredId: string | null | undefined,
  queue: QueueItem[],
): string | null {
  if (preferredId && queue.some((item) => item.id === preferredId)) {
    return preferredId;
  }

  return queue[0]?.id ?? null;
}

function basename(path: string): string {
  return path.split(/[\\/]/).pop() ?? path;
}

export interface UseRomManagerResult {
  queue: QueueItem[];
  selectedItem: QueueItem | null;
  selectedQueueId: string | null;
  isLoadingQueue: boolean;
  isImporting: boolean;
  busyQueueId: string | null;
  dragActive: boolean;
  statusMessage: string;
  errorMessage: string;
  setDragActive: (value: boolean) => void;
  selectQueueItem: (queueId: string) => void;
  refreshQueue: (preferredId?: string | null) => Promise<void>;
  pickRom: () => Promise<void>;
  importPaths: (paths: string[]) => Promise<void>;
  removeQueueItem: (queueId: string) => Promise<void>;
  clearQueue: () => Promise<void>;
  moveQueueItemByOffset: (queueId: string, direction: -1 | 1) => Promise<void>;
  togglePartition: (queueId: string, partition: string, enabled: boolean) => Promise<void>;
  reanalyzeSelected: () => Promise<void>;
}

export function useRomManager(): UseRomManagerResult {
  const [queue, setQueue] = useState<QueueItem[]>([]);
  const [selectedQueueId, setSelectedQueueId] = useState<string | null>(null);
  const [isLoadingQueue, setIsLoadingQueue] = useState(true);
  const [isImporting, setIsImporting] = useState(false);
  const [busyQueueId, setBusyQueueId] = useState<string | null>(null);
  const [dragActive, setDragActive] = useState(false);
  const [statusMessage, setStatusMessage] = useState("ROM Manager ready");
  const [errorMessage, setErrorMessage] = useState("");

  const refreshQueue = useCallback(
    async (preferredId?: string | null) => {
      setIsLoadingQueue(true);

      try {
        const nextQueue = await getRomQueue();
        setQueue(nextQueue);
        setSelectedQueueId((current) => resolveSelectedQueueId(preferredId ?? current, nextQueue));
        if (nextQueue.length === 0) {
          setStatusMessage("ROM queue is empty");
        }
      } catch (error: unknown) {
        setErrorMessage(String(error));
      } finally {
        setIsLoadingQueue(false);
      }
    },
    [],
  );

  useEffect(() => {
    void refreshQueue();
  }, [refreshQueue]);

  const selectedItem = useMemo(
    () => queue.find((item) => item.id === selectedQueueId) ?? queue[0] ?? null,
    [queue, selectedQueueId],
  );

  const selectQueueItem = useCallback((queueId: string) => {
    setSelectedQueueId(queueId);
    setErrorMessage("");
  }, []);

  const importPaths = useCallback(
    async (paths: string[]) => {
      const zipPaths = normalizeZipPaths(paths);
      if (zipPaths.length === 0) {
        setErrorMessage("Drop or select at least one .zip firmware package");
        return;
      }

      setIsImporting(true);
      setErrorMessage("");
      setStatusMessage(`Inspecting ${zipPaths.length} ROM package${zipPaths.length === 1 ? "" : "s"}...`);

      const failures: string[] = [];
      let importedCount = 0;
      let lastImportedId: string | null = null;

      try {
        for (const path of zipPaths) {
          try {
            const item = await addRomToQueue(path);
            importedCount += 1;
            lastImportedId = item.id;
          } catch (error: unknown) {
            failures.push(`${basename(path)} — ${String(error)}`);
          }
        }

        await refreshQueue(lastImportedId);

        if (importedCount > 0) {
          setStatusMessage(
            `Imported ${importedCount} ROM package${importedCount === 1 ? "" : "s"} into the flash queue`,
          );
        }

        if (failures.length > 0) {
          setErrorMessage(failures.join("\n"));
          if (importedCount === 0) {
            setStatusMessage("ROM import failed");
          }
        }
      } finally {
        setIsImporting(false);
      }
    },
    [refreshQueue],
  );

  const pickRom = useCallback(async () => {
    setErrorMessage("");

    try {
      const filePath = await selectRomFile();
      if (!filePath) {
        return;
      }

      await importPaths([filePath]);
    } catch (error: unknown) {
      setErrorMessage(String(error));
    }
  }, [importPaths]);

  const removeQueueItem = useCallback(
    async (queueId: string) => {
      setBusyQueueId(queueId);
      setErrorMessage("");

      try {
        const nextQueue = await removeRomFromQueue(queueId);
        setQueue(nextQueue);
        setSelectedQueueId((current) => resolveSelectedQueueId(current === queueId ? null : current, nextQueue));
        setStatusMessage("Removed ROM package from the queue");
      } catch (error: unknown) {
        setErrorMessage(String(error));
      } finally {
        setBusyQueueId(null);
      }
    },
    [],
  );

  const clearQueue = useCallback(async () => {
    setBusyQueueId("__all__");
    setErrorMessage("");

    try {
      const nextQueue = await clearRomQueue();
      setQueue(nextQueue);
      setSelectedQueueId(null);
      setStatusMessage("ROM queue cleared");
    } catch (error: unknown) {
      setErrorMessage(String(error));
    } finally {
      setBusyQueueId(null);
    }
  }, []);

  const moveQueueItemByOffset = useCallback(
    async (queueId: string, direction: -1 | 1) => {
      const fromIndex = queue.findIndex((item) => item.id === queueId);
      if (fromIndex === -1) {
        return;
      }

      const toIndex = fromIndex + direction;
      if (toIndex < 0 || toIndex >= queue.length) {
        return;
      }

      setBusyQueueId(queueId);
      setErrorMessage("");

      try {
        const nextQueue = await moveRomQueueItem(fromIndex, toIndex);
        setQueue(nextQueue);
        setSelectedQueueId(queueId);
        setStatusMessage("Reordered ROM queue");
      } catch (error: unknown) {
        setErrorMessage(String(error));
      } finally {
        setBusyQueueId(null);
      }
    },
    [queue],
  );

  const togglePartition = useCallback(
    async (queueId: string, partition: string, enabled: boolean) => {
      setBusyQueueId(queueId);
      setErrorMessage("");

      try {
        const updatedItem = await toggleRomQueuePartition(queueId, partition, enabled);
        setQueue((currentQueue) =>
          currentQueue.map((item) => (item.id === updatedItem.id ? updatedItem : item)),
        );
        setSelectedQueueId(updatedItem.id);
        setStatusMessage(
          `${enabled ? "Enabled" : "Disabled"} ${partition} for ${updatedItem.fileName}`,
        );
      } catch (error: unknown) {
        setErrorMessage(String(error));
      } finally {
        setBusyQueueId(null);
      }
    },
    [],
  );

  const reanalyzeSelected = useCallback(async () => {
    if (!selectedItem) {
      return;
    }

    await importPaths([selectedItem.filePath]);
  }, [importPaths, selectedItem]);

  return {
    queue,
    selectedItem,
    selectedQueueId,
    isLoadingQueue,
    isImporting,
    busyQueueId,
    dragActive,
    statusMessage,
    errorMessage,
    setDragActive,
    selectQueueItem,
    refreshQueue,
    pickRom,
    importPaths,
    removeQueueItem,
    clearQueue,
    moveQueueItemByOffset,
    togglePartition,
    reanalyzeSelected,
  };
}
