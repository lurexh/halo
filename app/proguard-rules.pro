# Keep the JNI bridge class + native method names (called from Rust by name).
-keep class dev.lurexh.halo.core.Native { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ics-openvpn is loaded reflectively by its own service/activities.
-keep class de.blinkt.openvpn.** { *; }
-keep class org.spongycastle.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn de.blinkt.openvpn.**
-dontwarn org.spongycastle.**
-dontwarn org.bouncycastle.**
