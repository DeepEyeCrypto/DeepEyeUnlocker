import { TerminalLog } from "./ui/TerminalLog";

interface Props {
  output: string;
  status: "idle" | "running" | "success" | "error";
}

export default function Terminal({ output, status }: Props) {
  if (status === "idle" && !output) return null;

  const lines = (output || "Waiting for execution...")
    .split("\n")
    .filter((line) => line.length > 0);

  const statusLine =
    status === "running"
      ? "[info] Operation running"
      : status === "success"
        ? "[info] Operation complete"
        : status === "error"
          ? "[info] Operation failed"
          : "[info] Idle";

  return <TerminalLog lines={[statusLine, ...lines]} />;
}
