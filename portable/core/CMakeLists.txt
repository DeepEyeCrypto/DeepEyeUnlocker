cmake_minimum_required(VERSION 3.10)
project(DeepEyeCore)

set(CMAKE_CXX_STANDARD 17)

# Include Directories
include_directories(include /usr/include/libusb-1.0)

# Source Files
set(CORE_SOURCES
    src/protocols/protocol_engine.cpp
    src/protocols/edl_manager.cpp
    src/protocols/brom_manager.cpp
    src/protocols/firehose.cpp
    src/protocols/gpt_parser.cpp
    src/protocols/sparse_handler.cpp
    src/protocols/da_handler.cpp
    src/protocols/boot_patcher.cpp
    src/transport/usb_transport.cpp
    src/deepeye_exports.cpp
)

# Shared Library for Android JNI and Desktop bridge
add_library(deepeye_core SHARED ${CORE_SOURCES})

# Target-specific linking
if(ANDROID)
    find_library(USB_LIB usb1.0)
    if(USB_LIB)
        target_link_libraries(deepeye_core log ${USB_LIB})
    else()
        target_link_libraries(deepeye_core log)
    endif()
else()
    find_library(USB_LIB usb-1.0)
    if(USB_LIB)
        target_link_libraries(deepeye_core ${USB_LIB})
    endif()
endif()
