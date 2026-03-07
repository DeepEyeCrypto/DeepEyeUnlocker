cmake_minimum_required(VERSION 3.22.1)
project(deepeye_core)

set(JNI_ROOT ${CMAKE_SOURCE_DIR})
set(LIBUSB_ROOT ${JNI_ROOT}/core/libusb-source/libusb)

# ── config.h guarantee ──────────────────────────────────────
configure_file(
    ${JNI_ROOT}/core/libusb-source/libusb/config.h
    ${CMAKE_BINARY_DIR}/config.h COPYONLY
)

# ── libusb static (Android-safe sources only) ───────────────
add_library(usb_static STATIC
    ${LIBUSB_ROOT}/core.c
    ${LIBUSB_ROOT}/descriptor.c
    ${LIBUSB_ROOT}/hotplug.c
    ${LIBUSB_ROOT}/io.c
    ${LIBUSB_ROOT}/sync.c
    ${LIBUSB_ROOT}/strerror.c
    ${LIBUSB_ROOT}/os/linux_usbfs.c
    ${LIBUSB_ROOT}/os/events_posix.c
    ${LIBUSB_ROOT}/os/threads_posix.c
)
# NOTE: linux_netlink.c and linux_udev.c EXCLUDED — no udev on Android
# hotplug.c included — it's pure logic, no udev dependency

target_include_directories(usb_static PUBLIC
    ${JNI_ROOT}/core/include
    ${LIBUSB_ROOT}
    ${JNI_ROOT}/core/libusb-source
    ${CMAKE_BINARY_DIR}
)

target_compile_definitions(usb_static PRIVATE
    HAVE_NFDS_T
    LINUX
    PLATFORM_POSIX
    __ANDROID__
    ANDROID
)

# ── deepeye_core shared library ─────────────────────────────
# native-lib.cpp = JNI entry point
# transport/usb_transport.cpp provides LibUsbTransport implementation used by
# native-lib.cpp and deepeye_exports.cpp.
add_library(deepeye_core SHARED
    native-lib.cpp
    core/src/deepeye_exports.cpp
    core/src/transport/usb_transport.cpp
    core/src/protocols/protocol_engine.cpp
    core/src/protocols/edl_manager.cpp
    core/src/protocols/brom_manager.cpp
    core/src/protocols/odin_manager.cpp
    core/src/protocols/fastboot_manager.cpp
    core/src/protocols/fdl_manager.cpp
    core/src/protocols/firehose.cpp
    core/src/protocols/gpt_parser.cpp
    core/src/protocols/sparse_handler.cpp
    core/src/protocols/da_handler.cpp
    core/src/protocols/boot_patcher.cpp
    core/src/forensics/forensic_engine.cpp
    core/src/forensics/sqlite_handler.cpp
)

target_include_directories(deepeye_core PRIVATE
    ${JNI_ROOT}
    ${JNI_ROOT}/core/include
    ${LIBUSB_ROOT}
    ${CMAKE_BINARY_DIR}
)

target_compile_definitions(deepeye_core PRIVATE
    __ANDROID__
    ANDROID
    HAS_LIBUSB=1
)

target_link_libraries(deepeye_core
    usb_static
    android
    log
)
