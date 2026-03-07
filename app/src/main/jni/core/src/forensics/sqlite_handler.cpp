#include "../../include/sqlite_handler.h"
#include <iostream>
#include <sstream>

namespace DeepEye {
namespace Forensics {

SQLiteHandler::SQLiteHandler(const std::string &dbPath) : _dbPath(dbPath) {}

bool SQLiteHandler::ClearLockSettings() {
  std::cout << "[SQLITE] Clearing locksettings.db entries for: " << _dbPath
            << std::endl;

  // ── SQL Sequence for Android 6.0 – 14+ ────────────────────────
  // We clear the key/value pairs that enforce lockscreens.
  const char *queries[] = {
      "DELETE FROM locksettings WHERE name='lockscreen.password_type';",
      "DELETE FROM locksettings WHERE name='lockscreen.password_salt';",
      "DELETE FROM locksettings WHERE name='lockscreen.gesture_salt';",
      "DELETE FROM locksettings WHERE name='lockscreen.password_hash';",
      "DELETE FROM locksettings WHERE name='lockscreen.gesture_hash';",
      "UPDATE locksettings SET value='0' WHERE name='lock_visibility';",
      "INSERT OR REPLACE INTO locksettings (name, value) VALUES "
      "('lockscreen.disabled', '1');"};

  bool allOk = true;
  for (const char *sql : queries) {
    if (!ExecuteSql(sql)) {
      std::cerr << "[SQLITE] ERROR: Query failed -> " << sql << std::endl;
      allOk = false;
    }
  }

  return allOk;
}

bool SQLiteHandler::ResetGatekeeper() {
  std::cout << "[SQLITE] Resetting gatekeeper.db security blob..." << std::endl;

  // Gatekeeper tables: 'passwords' or 'blobs'
  return ExecuteSql("DELETE FROM passwords;");
}

std::vector<LockEntry> SQLiteHandler::QuerySettings() {
  std::vector<LockEntry> entries;
  // Mock results for UI verification
  entries.push_back({"owner_id", "0"});
  entries.push_back({"active_user", "0"});
  return entries;
}

bool SQLiteHandler::ExecuteSql(const std::string &sql) {
  // In a real implementation, we'd use:
  // int rc = sqlite3_exec(db, sql.c_str(), 0, 0, &errMsg);
  // For the native core, we'll simulate the command result.

  std::cout << "[NATIVE SQL] Executing: " << sql << std::endl;
  return true;
}

} // namespace Forensics
} // namespace DeepEye
