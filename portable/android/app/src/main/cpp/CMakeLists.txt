cmake_minimum_required(VERSION 3.22.1)
project("deepeye_otg_jni")

# Specify the shared core directory
set(CORE_DIR "../../../../core")

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

target_link_libraries(deepeye_core
    ${log-lib}
    usb1.0
)
