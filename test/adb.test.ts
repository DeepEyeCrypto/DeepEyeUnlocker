import { exec } from 'child_process';
import { promisify } from 'util';

const execAsync = promisify(exec);

describe('ADB Command Testing', () => {
  it('should list connected devices', async () => {
    const { stdout } = await execAsync('adb devices');
    expect(stdout).toContain('device');
  });

  it('should run adb shell commands', async () => {
    const { stdout } = await execAsync('adb shell ls /');
    expect(stdout).toContain('system');
  });

  it('should capture screenshot', async () => {
    const { stdout } = await execAsync('adb shell screencap -p /sdcard/screenshot.png && adb pull /sdcard/screenshot.png test/screenshot.png');
    expect(stdout).toContain('screenshot.png');
  });

  it('should get device info', async () => {
    const { stdout } = await execAsync('adb shell getprop ro.product.model');
    expect(stdout).not.toBe('');
  });

  it('should check USB debugging status', async () => {
    const { stdout } = await execAsync('adb shell settings get global adb_enabled');
    expect(stdout).toContain('1');
  });
});