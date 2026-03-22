/*
 * Root Detection Bypass Hook
 * Targets: RootBeer, common file checks (/system/xbin/su)
 */
Java.perform(function() {
    var File = Java.use('java.io.File');
    File.exists.implementation = function() {
        var name = this.getName();
        if (name == 'su' || name == 'magisk') {
            return false;
        }
        return this.exists();
    };
});
