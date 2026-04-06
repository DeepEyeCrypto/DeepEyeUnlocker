fn main() {
    println!("cargo:rerun-if-changed=build.rs");
    println!("cargo:rerun-if-env-changed=DEEPEYE_USE_SYSTEM_LIBUSB");

    // Vendored libusb is the default path. Set DEEPEYE_USE_SYSTEM_LIBUSB=1
    // only when intentionally falling back to a preinstalled system library.
    #[cfg(target_os = "macos")]
    {
        println!("cargo:rustc-link-search=native=/opt/homebrew/lib");
        println!("cargo:rustc-link-search=native=/usr/local/lib");

        if std::env::var_os("DEEPEYE_USE_SYSTEM_LIBUSB").is_some() {
            println!("cargo:rustc-link-lib=usb-1.0");
        }
    }

    #[cfg(target_os = "windows")]
    {
        println!("cargo:rustc-link-search=native=C:/vcpkg/installed/x64-windows/lib");

        if std::env::var_os("DEEPEYE_USE_SYSTEM_LIBUSB").is_some() {
            println!("cargo:rustc-link-lib=libusb-1.0");
        }
    }

    #[cfg(target_os = "linux")]
    {
        if std::env::var_os("DEEPEYE_USE_SYSTEM_LIBUSB").is_some() {
            println!("cargo:rustc-link-lib=usb-1.0");
        }
    }

    tauri_build::build()
}
