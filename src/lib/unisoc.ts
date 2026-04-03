export const UNISOC_COMMANDS = {
  DETECT: "unisoc_detect_device",
} as const;

export type UnisocDeviceInfo = {
  detected: boolean;
  vendorId: string;
  productId: string;
  mode: string;
  transport: string;
  productName: string;
  locationId: string;
  serviceGuidance: string;
};
