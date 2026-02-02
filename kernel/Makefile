# Makefile for DeepEyeUnlocker Kernel Bridge (v4.0.0)

# The name of the resulting module
obj-m += deepeye_kernel.o

# Object files to include in the module
deepeye_kernel-objs := src/main.o

# Paths and Environment
KDIR ?= /lib/modules/$(shell uname -r)/build
PWD := $(shell pwd)

# Default target: Build for the current running kernel (Linux)
all:
	$(MAKE) -C $(KDIR) M=$(PWD) modules

# Cleanup target
clean:
	$(MAKE) -C $(KDIR) M=$(PWD) clean

# Android Cross-Compilation
# Usage: make android ANDROID_KERNEL_PATH=/path/to/android/kernel/source
android:
	@if [ -z "$(ANDROID_KERNEL_PATH)" ]; then \
		echo "Error: ANDROID_KERNEL_PATH is not set."; \
		echo "Usage: make android ANDROID_KERNEL_PATH=/path/to/kernel ARCH=arm64 CROSS_COMPILE=aarch64-linux-android-"; \
		exit 1; \
	fi
	$(MAKE) -C $(ANDROID_KERNEL_PATH) \
		ARCH=$(ARCH) \
		CROSS_COMPILE=$(CROSS_COMPILE) \
		M=$(PWD) modules

# Helper to verify kernel version
verify:
	@echo "Current Kernel: $(shell uname -r)"
	@echo "Build Directory: $(KDIR)"
