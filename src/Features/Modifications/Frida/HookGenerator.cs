using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace DeepEyeUnlocker.Features.Modifications.Frida
{
    public class HookGenerator
    {
        public enum HookType
        {
            RootDetectionBypass,
            SslPinningBypass,
            ActivityLogger,
            ParameterIntercept
        }

        public string GenerateFridaScript(HookType type, string? targetClass = null, string? targetMethod = null)
        {
            return type switch
            {
                HookType.RootDetectionBypass => GenerateRootBypass(),
                HookType.SslPinningBypass => GenerateSslPinningBypass(),
                HookType.ActivityLogger => GenerateActivityLogger(),
                HookType.ParameterIntercept => GenerateParamIntercept(targetClass, targetMethod),
                _ => "// Unsupported hook type"
            };
        }

        private string GenerateRootBypass()
        {
            return @"
// DeepEye Root Bypass Utility
Java.perform(function() {
    var RootPackages = [""com.noshufou.android.su"", ""com.thirdparty.superuser"", ""eu.chainfire.supersu"", ""com.koushikdutta.superuser""];
    var RootFiles = [""/system/bin/su"", ""/system/xbin/su"", ""/sbin/su"", ""/system/sd/xbin/su""];

    var File = Java.use(""java.io.File"");
    File.exists.implementation = function() {
        var name = this.getName();
        if (RootFiles.indexOf(this.getAbsolutePath()) !== -1) {
            console.log(""[DeepEye] Bypassed check for: "" + this.getAbsolutePath());
            return false;
        }
        return this.exists();
    };
});";
        }

        private string GenerateSslPinningBypass()
        {
            return @"
// DeepEye SSL Pinning Bypass (Generic TrustManager)
Java.perform(function() {
    var TrustManagerImpl = Java.use('com.android.org.conscrypt.TrustManagerImpl');
    TrustManagerImpl.checkTrustedRecursive.implementation = function() {
        return [];
    };
});";
        }

        private string GenerateActivityLogger()
        {
            return @"
Java.perform(function() {
    var Activity = Java.use('android.app.Activity');
    Activity.onResume.implementation = function() {
        console.log('[DeepEye] Activity Resume: ' + this.getClass().getName());
        this.onResume();
    };
});";
        }

        private string GenerateParamIntercept(string? className, string? methodName)
        {
            if (string.IsNullOrEmpty(className) || string.IsNullOrEmpty(methodName))
                return "// Please specify class and method";

            return $@"
Java.perform(function() {{
    var target = Java.use('{className}');
    target.{methodName}.implementation = function() {{
        console.log('[DeepEye] Intercepted {methodName} call');
        for (var i = 0; i < arguments.length; i++) {{
            console.log('  Arg[' + i + ']: ' + arguments[i]);
        }}
        return this.{methodName}.apply(this, arguments);
    }};
}});";
        }
    }
}
