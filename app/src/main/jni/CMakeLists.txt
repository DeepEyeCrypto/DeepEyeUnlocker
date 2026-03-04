cmake_minimum_required(VERSION 3.22.1)
project("deepeye_otg_jni")

# Core Paths (Pure Android structure)
set(CORE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/core")
set(LIBUSB_ROOT_PATH "${CORE_DIR}/libusb-source")

message(STATUS "DeepEye Core Dir: ${CORE_DIR}")
message(STATUS "LibUSB Source Dir: ${LIBUSB_ROOT_PATH}")

include_directories(${CORE_DIR}/include)
include_directories(${LIBUSB_ROOT_PATH}/libusb)

# --- LIBUSB BUILD (Static) ---
set(LIBUSB_SRC
    ${LIBUSB_ROOT_PATH}/libusb/core.c
    ${LIBUSB_ROOT_PATH}/libusb/descriptor.c
    ${LIBUSB_ROOT_PATH}/libusb/io.c
    ${LIBUSB_ROOT_PATH}/libusb/sync.c
    ${LIBUSB_ROOT_PATH}/libusb/strerror.c
    ${LIBUSB_ROOT_PATH}/libusb/os/linux_usbfs.c
    ${LIBUSB_ROOT_PATH}/libusb/os/events_posix.c
    ${LIBUSB_ROOT_PATH}/libusb/os/threads_posix.c
)

add_library(usb_static STATIC ${LIBUSB_SRC})
target_compile_definitions(usb_static PRIVATE 
    HAVE_NFDS_T
    LINUX
    PLATFORM_POSIX
    __ANDROID__
    ANDROID
    LIBUSB_CALL_MISSING_HOTPLUG=1
)
target_include_directories(usb_static PRIVATE
    ${CORE_DIR}/include
    ${LIBUSB_ROOT_PATH}/libusb
    ${LIBUSB_ROOT_PATH}
)

# --- DEEPEYE JNI CORE ---
add_library(deepeye_core SHARED
    native-lib.cpp
    ${CORE_DIR}/src/protocols/protocol_engine.cpp
    ${CORE_DIR}/src/protocols/edl_manager.cpp
    ${CORE_DIR}/src/protocols/brom_manager.cpp
    ${CORE_DIR}/src/protocols/odin_manager.cpp
    ${CORE_DIR}/src/protocols/fdl_manager.cpp
    ${CORE_DIR}/src/protocols/firehose.cpp
    ${CORE_DIR}/src/protocols/gpt_parser.cpp
    ${CORE_DIR}/src/protocols/sparse_handler.cpp
    ${CORE_DIR}/src/protocols/da_handler.cpp
    ${CORE_DIR}/src/protocols/boot_patcher.cpp
    ${CORE_DIR}/src/transport/usb_transport.cpp
)

find_library(log-lib log)

# Link LibUSB Static
target_link_libraries(deepeye_core
    ${log-lib}
    usb_static
)

# Enable USB Support Flag
target_compile_definitions(deepeye_core PRIVATE HAS_LIBUSB=1)
message(STATUS "LibUSB configured successfully (Static Build).")
