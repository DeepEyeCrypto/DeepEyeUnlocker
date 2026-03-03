use rusqlite::{params, Connection, Result as SqlResult};
use std::sync::{Arc, Mutex};
use tracing::info;

#[derive(Clone)]
pub struct DatabaseManager {
    conn: Arc<Mutex<Connection>>,
}

impl DatabaseManager {
    pub fn new() -> SqlResult<Self> {
        // Open local SQLite DB (creates file if not present)
        let conn = Connection::open("deepeye_state.db")?;

        let db_manager = Self {
            conn: Arc::new(Mutex::new(conn)),
        };

        db_manager.initialize_schema()?;

        info!("DeepEye Local SQLite Database mapped successfully.");
        Ok(db_manager)
    }

    fn initialize_schema(&self) -> SqlResult<()> {
        let conn = self.conn.lock().unwrap();

        // Operational History to power the "Logs" & "Telemetry" dashboards
        conn.execute(
            "CREATE TABLE IF NOT EXISTS service_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                platform TEXT NOT NULL,
                feature_name TEXT NOT NULL,
                status TEXT NOT NULL
            )",
            [],
        )?;

        // Known Local Device Profiles (For matching auth/loaders to specific device variants)
        conn.execute(
            "CREATE TABLE IF NOT EXISTS device_profiles (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                brand TEXT NOT NULL,
                model TEXT NOT NULL,
                chipset_id TEXT NOT NULL,
                auth_file_blob BLOB,
                loader_blob BLOB
            )",
            [],
        )?;

        Ok(())
    }

    pub fn insert_job_log(
        &self,
        platform: &str,
        feature_name: &str,
        status: &str,
    ) -> SqlResult<()> {
        let conn = self.conn.lock().unwrap();
        conn.execute(
            "INSERT INTO service_logs (platform, feature_name, status) VALUES (?1, ?2, ?3)",
            params![platform, feature_name, status],
        )?;
        Ok(())
    }
}
