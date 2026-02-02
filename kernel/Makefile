# Makefile for DeepEyeUnlocker Kernel Bridge (v4.0.0)

# The name of the resulting module
obj-m += deepeye_kernel.o

# Object files to include in the module
deepeye_kernel-objs := src/main.o src/ksu_bridge.o src/exploit_monitor.o

# Native Tools (Userspace)
NATIVE_CC := aarch64-linux-android-g++
NATIVE_FLAGS := -O3 -std=c++17 -Iinclude

# Python Bindings
PYTHON_CC := g++
PYTHON_FLAGS := -O3 -shared -std=c++11 -fPIC $(shell python3 -m pybind11 --includes)
PY_EXT := $(shell python3-config --extension-suffix)
KDIR ?= /lib/modules/$(shell uname -r)/build
PWD := $(shell pwd)

# Default target: Build for the current running kernel (Linux)
all: modules native python

modules:
	$(MAKE) -C $(KDIR) M=$(PWD) modules

native:
	$(NATIVE_CC) $(NATIVE_FLAGS) src/boot_patch.cpp src/boot_image_core.cpp -o deepeye_native

python:
	$(PYTHON_CC) $(PYTHON_FLAGS) src/deepeye_py.cpp src/boot_image_core.cpp -o deepeye_kernel$(PY_EXT)

# Cleanup target
clean:
	$(MAKE) -C $(KDIR) M=$(PWD) clean
	rm -f deepeye_native

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
	$(NATIVE_CC) $(NATIVE_FLAGS) src/boot_patch.cpp -o deepeye_native

# Helper to verify kernel version
verify:
	@echo "Current Kernel: $(shell uname -r)"
	@echo "Build Directory: $(KDIR)"
