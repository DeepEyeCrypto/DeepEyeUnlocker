// v2 main.rs is minimal:
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

fn main() {
    deep_eye_unlocker_lib::run();
}
