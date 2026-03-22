/*
 * SSL Pinning Bypass Hook
 * Targets: TrustManager, OkHttp, WebView
 */
Java.perform(function() {
    var TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');
    TrustManagerImpl.checkServerTrusted.overload('[Ljava.security.cert.X509Certificate;', 'java.lang.String', 'java.lang.String').implementation = function(chain, authType, host) {
        console.log('[FRIDA] Bypassed SSL Pinning for host: ' + host);
        return chain;
    };
});
