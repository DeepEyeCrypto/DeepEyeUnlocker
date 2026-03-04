// Android build: hotplug disabled to avoid libudev linkage
#ifndef LIBUSB_NO_HOTPLUG
#include "libusbi.h"

int usbi_hotplug_init(struct libusb_context *ctx);
void usbi_hotplug_exit(struct libusb_context *ctx);
void usbi_hotplug_notification(struct libusb_context *ctx, struct libusb_device *dev,
    libusb_hotplug_event event);
void usbi_hotplug_process(struct libusb_context *ctx, struct list_head *hotplug_msgs);

#else
// Stubbed hotplug APIs when hotplug support is disabled
static inline int libusb_has_capability(uint32_t cap) { (void)cap; return 0; }
#endif