# ideviceactivation Quick Reference

## ✅ Installation Status

- **Binary:** `/usr/local/bin/ideviceactivation`
- **Version:** 1.1.1-28-g9ca1851
- **Status:** INSTALLED ✓

---

## Quick Commands

### Check Activation State

```bash
ideviceactivation -u <UDID> state
```

### Activate Device

```bash
ideviceactivation -u <UDID> activate
```

### List Connected Devices

```bash
idevice_id -l
```

### Get Device Info

```bash
ideviceinfo -u <UDID>
```

---

## Test Your Device (UDID: 00008120-000924940A42201E)

```bash
# Check state
ideviceactivation -u 00008120-000924940A42201E state

# Expected: "Activation state: MobileActivated" or "Unactivated"
```

---

## If Tool Goes Missing

### Reinstall from Source

```bash
cd ~/Downloads/libideviceactivation
make clean
./configure --prefix=/usr/local
make -j4
sudo make install
```

### Or Rebuild from Scratch

```bash
cd ~/Downloads
rm -rf libideviceactivation
git clone https://github.com/libimobiledevice/libideviceactivation.git
cd libideviceactivation
export PKG_CONFIG_PATH="/usr/local/opt/openssl@3/lib/pkgconfig:/usr/local/lib/pkgconfig"
./autogen.sh --prefix=/usr/local
./configure --prefix=/usr/local
make -j4
sudo make install
```

---

## App Testing

```bash
cd /Users/enayat/Documents/DeepEyeUnlocker
npm run tauri dev
```

Test: Apple Tools → ONE-CLICK BYPASS

---

## Common Issues

| Issue               | Solution                       |
| ------------------- | ------------------------------ |
| `command not found` | Add `/usr/local/bin` to PATH   |
| `Device not found`  | Connect iPhone and tap "Trust" |
| Build fails         | Run `make clean` and rebuild   |
| No devices listed   | Check USB cable, unlock phone  |

---

## Man Page

```bash
man ideviceactivation
```
