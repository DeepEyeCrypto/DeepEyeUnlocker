use crate::session::types::OperationSession;
use std::sync::Mutex;

pub struct SessionManager {
    pub active: Mutex<Option<OperationSession>>,
    pub history: Mutex<Vec<OperationSession>>,
}

impl SessionManager {
    pub fn new() -> Self {
        Self {
            active: Mutex::new(None),
            history: Mutex::new(Vec::new()),
        }
    }
}
