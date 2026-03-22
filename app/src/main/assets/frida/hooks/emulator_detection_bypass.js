/*
 * Emulator Detection Bypass
 * Targets: Build.MODEL, Build.PRODUCT, etc.
 */
Java.perform(function() {
    var Build = Java.use('android.os.Build');
    Build.MODEL.value = 'Pixel 8 Pro';
    Build.PRODUCT.value = 'husky';
    Build.MANUFACTURER.value = 'Google';
    console.log('[FRIDA] Spoofed device info to bypass emulator detection');
});
