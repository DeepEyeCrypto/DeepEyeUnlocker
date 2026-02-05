cmake_minimum_required(VERSION 3.10)
project(DeepEyeCore)

set(CMAKE_CXX_STANDARD 17)

# Include Directories
include_directories(include /usr/include/libusb-1.0)

# Get absolute path to the core directory
get_filename_component(CORE_DIR "${CMAKE_CURRENT_SOURCE_DIR}" ABSOLUTE)
set(CORE_SRC_DIR "${CORE_DIR}/src")

# Verify source directory exists
if(NOT EXISTS "${CORE_SRC_DIR}")
    message(FATAL_ERROR "Core source directory not found: ${CORE_SRC_DIR}")
endif()

message(STATUS "Core directory: ${CORE_DIR}")
message(STATUS "Core source directory: ${CORE_SRC_DIR}")

# Source Files (using absolute paths)
set(CORE_SOURCES
    ${CORE_SRC_DIR}/protocols/protocol_engine.cpp
    ${CORE_SRC_DIR}/protocols/edl_manager.cpp
    ${CORE_SRC_DIR}/protocols/brom_manager.cpp
    ${CORE_SRC_DIR}/protocols/firehose.cpp
    ${CORE_SRC_DIR}/protocols/gpt_parser.cpp
    ${CORE_SRC_DIR}/protocols/sparse_handler.cpp
    ${CORE_SRC_DIR}/protocols/da_handler.cpp
    ${CORE_SRC_DIR}/protocols/boot_patcher.cpp
    ${CORE_SRC_DIR}/transport/usb_transport.cpp
    ${CORE_SRC_DIR}/deepeye_exports.cpp
)

# Shared Library for Android JNI and Desktop bridge
add_library(deepeye_core SHARED ${CORE_SOURCES})

# Target-specific linking
if(ANDROID)
    # Android OTG USB access - make optional as not all NDK environments have libusb
    find_library(USB_LIB usb1.0 PATHS ${ANDROID_NDK}/sysroot/usr/lib/${ANDROID_SYSROOT_ABI})
    if(USB_LIB)
        message(STATUS "Found libusb for Android: ${USB_LIB}")
        target_link_libraries(deepeye_core log ${USB_LIB})
        target_compile_definitions(deepeye_core PRIVATE HAS_LIBUSB=1)
    else()
        message(WARNING "libusb NOT found for Android - USB Transport will be stubbed")
        message(WARNING "This is OK for initial builds - USB support can be added later")
        target_link_libraries(deepeye_core log)
        # Do NOT define HAS_LIBUSB at all - let #ifdef guards work correctly
    endif()
else()
    find_package(PkgConfig QUIET)
    if(PkgConfig_FOUND)
        pkg_check_modules(LIBUSB libusb-1.0)
    endif()

    if(LIBUSB_FOUND)
        include_directories(${LIBUSB_INCLUDE_DIRS})
        target_link_libraries(deepeye_core ${LIBUSB_LIBRARIES})
        target_compile_definitions(deepeye_core PRIVATE HAS_LIBUSB=1)
    else()
        find_library(USB_LIB usb-1.0)
        if(USB_LIB)
            target_link_libraries(deepeye_core ${USB_LIB})
            target_compile_definitions(deepeye_core PRIVATE HAS_LIBUSB=1)
        endif()
    endif()
endif()
