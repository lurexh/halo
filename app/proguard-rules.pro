# Keep the JNI bridge class + native method names (called from Rust by name).
-keep class dev.lurexh.halo.core.Native { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
