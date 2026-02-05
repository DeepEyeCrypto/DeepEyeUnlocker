cmake_minimum_required(VERSION 3.22.1)
project("deepeye_otg_jni")

# Specify the shared core directory using absolute path for safety
set(CORE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../../../../../core")
message(STATUS "DeepEye Core Dir: ${CORE_DIR}")

# Include headers from the shared core
include_directories(${CORE_DIR}/include)

# Libusb is usually provided by the system or as a prebuilt for Android
# Here we assume it's available as 'usb1.0' via standard NDK search or bundled
find_library(log-lib log)

# The JNI bridge sources
add_library(deepeye_core SHARED
    native-lib.cpp
    ${CORE_DIR}/src/protocols/protocol_engine.cpp
    ${CORE_DIR}/src/protocols/edl_manager.cpp
    ${CORE_DIR}/src/protocols/brom_manager.cpp
    ${CORE_DIR}/src/protocols/firehose.cpp
    ${CORE_DIR}/src/protocols/gpt_parser.cpp
    ${CORE_DIR}/src/protocols/sparse_handler.cpp
    ${CORE_DIR}/src/protocols/da_handler.cpp
    ${CORE_DIR}/src/protocols/boot_patcher.cpp
    ${CORE_DIR}/src/transport/usb_transport.cpp
)

find_library(USB_LIB usb1.0)

# Conditional linking based on library availability
if(USB_LIB)
    message(STATUS "Found libusb for Android JNI: ${USB_LIB}")
    target_link_libraries(deepeye_core
        ${log-lib}
        ${USB_LIB}
    )
    target_compile_definitions(deepeye_core PRIVATE HAS_LIBUSB=1)
else()
    message(WARNING "libusb NOT found - building without USB support")
    message(WARNING "USB transport will be stubbed (this is OK for initial builds)")
    target_link_libraries(deepeye_core
        ${log-lib}
    )
    # Do NOT define HAS_LIBUSB - let #ifdef guards work correctly
endif()

