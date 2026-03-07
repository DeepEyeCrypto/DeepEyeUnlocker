#ifndef DEEPEYE_SQLITE_HANDLER_H
#define DEEPEYE_SQLITE_HANDLER_H

#include <string>
#include <vector>

namespace DeepEye {
namespace Forensics {

struct LockEntry {
  std::string key;
  std::string value;
};

class SQLiteHandler {
public:
  SQLiteHandler(const std::string &dbPath);

  /**
   * Remove Lock Flags: Clears lockscreen.password_type, lockscreen.disabled,
   * etc. from the system's locksettings.db.
   */
  bool ClearLockSettings();

  /**
   * Reset Gatekeeper: Clears the gatekeeper.db hash to bypass biometric/PIN
   * security on reboot.
   */
  bool ResetGatekeeper();

  /**
   * Query Database: Execute raw SQL if needed.
   */
  std::vector<LockEntry> QuerySettings();

private:
  std::string _dbPath;
  bool ExecuteSql(const std::string &sql);
};

} // namespace Forensics
} // namespace DeepEye

#endif // DEEPEYE_SQLITE_HANDLER_H
