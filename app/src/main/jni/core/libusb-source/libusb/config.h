/* ============================================================
   config.h — Android NDK build of libusb 1.0
   Replaces autotools-generated config.h
   Target: Android API 26+ | arm64-v8a + armeabi-v7a
   ============================================================ */

#pragma once

/* Version */
#define VERSION "1.0.26"
#define PACKAGE "libusb"
#define PACKAGE_VERSION "1.0.26"

/* Platform */
#define OS_LINUX 1
#define HAVE_NFDS_T 1

/* Required POSIX features */
#define HAVE_SYS_TIME_H 1
#define HAVE_NFDS_T 1
#define HAVE_PIPE2 1
#define HAVE_PTHREAD_CONDATTR_SETCLOCK 1
#define HAVE_PTHREAD_SETNAME_NP 1
#define HAVE_EVENTFD 1
#define HAVE_TIMERFD 1

/* Android: use poll-based I/O backend */
#define POLL_NFDS_TYPE nfds_t

/* Logging */
#define ENABLE_LOGGING 1
#define ENABLE_DEBUG_LOGGING 0

/* No udev on Android */
#undef HAVE_LIBUDEV
#undef USE_UDEV

/* No syslog on Android */
#undef HAVE_SYSLOG_FUNC

/* Headers available in Android NDK */
#define HAVE_DLFCN_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_MEMORY_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_SYS_STAT_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_UNISTD_H 1

/* Visibility */
#define DEFAULT_VISIBILITY __attribute__((visibility("default")))

/* Stdc */
#define STDC_HEADERS 1

/* end config.h */
