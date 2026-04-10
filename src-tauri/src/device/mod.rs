//! Device detection and protocol module.
//!
//! This module provides unified device detection, protocol classification,
//! and error handling for supported device protocols including MediaTek
//! BROM/PreLoader, Qualcomm EDL, Fastboot, Samsung Odin, UniSoc FDL,
//! and ADB/MTP paths.

pub mod detector;
pub mod error;
pub mod mtk_da;
pub mod protocol_router;
