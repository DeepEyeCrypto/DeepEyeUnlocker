pub mod connection;
pub mod policy;
pub mod protocols;
pub mod database;

/// Core Initialization
pub fn init() {
    tracing_subscriber::fmt::init();
    tracing::info!("DeepEyeCore Universal Engine Initialized.");
}
