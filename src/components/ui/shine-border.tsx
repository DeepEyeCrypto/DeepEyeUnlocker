import type { CSSProperties, ReactNode } from "react";
import {
  Check,
  Download,
  Repeat,
  Send,
  Shapes,
  type LucideIcon,
} from "lucide-react";

import { cn } from "@/lib/utils";

type TColorProp = string | string[];

interface ShineBorderProps {
  borderRadius?: number;
  borderWidth?: number;
  duration?: number;
  color?: TColorProp;
  className?: string;
  children: ReactNode;
}

function ShineBorder({
  borderRadius = 18,
  borderWidth = 1,
  duration = 14,
  color = "#000000",
  className,
  children,
}: ShineBorderProps) {
  return (
    <div
      style={
        {
          "--border-radius": `${borderRadius}px`,
        } as CSSProperties
      }
      className={cn(
        "relative h-full w-full overflow-hidden rounded-[var(--border-radius)]",
        className,
      )}
    >
      <div
        style={
          {
            "--border-width": `${borderWidth}px`,
            "--shine-pulse-duration": `${duration}s`,
            "--background-radial-gradient": `radial-gradient(transparent, transparent, ${
              color instanceof Array ? color.join(",") : color
            }, transparent, transparent)`,
            WebkitMask:
              "linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0)",
            WebkitMaskComposite: "xor",
            maskComposite: "exclude",
          } as CSSProperties
        }
        className="pointer-events-none absolute inset-0 rounded-[var(--border-radius)] p-[var(--border-width)] [background-image:var(--background-radial-gradient)] [background-size:300%_300%] motion-safe:animate-[shine-pulse_var(--shine-pulse-duration)_infinite_linear]"
      />
      <div className="relative z-10 h-full w-full rounded-[var(--border-radius)]">
        {children}
      </div>
    </div>
  );
}

export function TimelineContainer({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto flex w-full max-w-md flex-col justify-center gap-3 md:order-2">
      {children}
    </div>
  );
}

interface TimelineEventItem {
  label: string;
  message: string;
  icon: {
    component: LucideIcon;
    textColor: string;
    borderColor: string;
  };
}

export function TimelineEvent({
  label,
  message,
  icon,
  isLast = false,
}: TimelineEventItem & {
  isLast?: boolean;
}) {
  const Icon = icon.component;

  return (
    <div className="group relative -m-2 flex gap-4 border border-transparent p-2">
      <div className="relative">
        <div
          className={cn(
            "rounded-full border bg-background p-2",
            icon.borderColor,
          )}
        >
          <Icon className={cn("h-4 w-4", icon.textColor)} />
        </div>
        {!isLast ? (
          <div className="absolute inset-x-0 mx-auto h-full w-[2px] bg-muted" />
        ) : null}
      </div>
      <div className="mt-1 flex flex-1 flex-col gap-1">
        <div className="flex items-center justify-between gap-4">
          <p className="text-lg font-semibold">{label}</p>
        </div>
        <p className="text-xs text-muted-foreground">{message}</p>
      </div>
    </div>
  );
}

const timeline = [
  {
    label: "Choose Your Design",
    message:
      "Browse and select a design that fits your needs, then access your personalized dashboard.",
    icon: {
      component: Shapes,
      textColor: "text-orange-500",
      borderColor: "border-orange-500/40",
    },
  },
  {
    label: "Provide Your Brief",
    message: "Share your design preferences and requirements with us.",
    icon: {
      component: Send,
      textColor: "text-amber-500",
      borderColor: "border-amber-500/40",
    },
  },
  {
    label: "Receive Your Designs",
    message: "Get your initial designs within 48 hours.",
    icon: {
      component: Check,
      textColor: "text-blue-500",
      borderColor: "border-blue-500/40",
    },
  },
  {
    label: "Request Revisions",
    message:
      "We’re committed to perfection—request as many revisions as needed until you’re satisfied.",
    icon: {
      component: Repeat,
      textColor: "text-green-500",
      borderColor: "border-green-500/40",
    },
  },
  {
    label: "Get Final Files",
    message: "Once approved, we’ll deliver the final files to you.",
    icon: {
      component: Download,
      textColor: "text-green-500",
      borderColor: "border-green-500/40",
    },
  },
] satisfies TimelineEventItem[];

export function Timeline() {
  return (
    <div className="w-full max-w-3xl">
      <TimelineContainer>
        {timeline.map((event, index) => (
          <TimelineEvent
            key={event.message}
            isLast={index === timeline.length - 1}
            {...event}
          />
        ))}
      </TimelineContainer>
    </div>
  );
}

export { ShineBorder };
