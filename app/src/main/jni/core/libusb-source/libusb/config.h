#pragma once
#define VERSION "1.0.26"
#define PACKAGE "libusb"
#define OS_LINUX 1
#define HAVE_NFDS_T 1
#define HAVE_SYS_TIME_H 1
#define HAVE_PIPE2 1
#define HAVE_PTHREAD_CONDATTR_SETCLOCK 1
#define HAVE_PTHREAD_SETNAME_NP 1
#define HAVE_EVENTFD 1
#define HAVE_TIMERFD 1
#define POLL_NFDS_TYPE nfds_t
#define ENABLE_LOGGING 1
#define ENABLE_DEBUG_LOGGING 0
#undef HAVE_LIBUDEV
#undef USE_UDEV
#define HAVE_DLFCN_H 1
#define HAVE_STDINT_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1
#define HAVE_UNISTD_H 1
#define DEFAULT_VISIBILITY __attribute__((visibility("default")))
#define STDC_HEADERS 1
