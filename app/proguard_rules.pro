# ============================================
# PROGUARD RULES untuk Dynamic Island
# ============================================

# Jangan obfuscate data class (untuk state)
-keep class com.example.dynamicisland.IslandState { *; }
-keep class com.example.dynamicisland.IslandMode { *; }
-keep class com.example.dynamicisland.CallInfo { *; }
-keep class com.example.dynamicisland.ChatInfo { *; }
-keep class com.example.dynamicisland.ChargingInfo { *; }
-keep class com.example.dynamicisland.NavigationInfo { *; }
-keep class com.example.dynamicisland.AlarmInfo { *; }
-keep class com.example.dynamicisland.MusicInfo { *; }

# Keep service (WAJIB — kalau tidak, service mati)
-keep class com.example.dynamicisland.DynamicIslandService { *; }
-keep class com.example.dynamicisland.MediaListenerService { *; }
-keep class com.example.dynamicisland.OverlayLifecycleOwner { *; }

# Keep MediaControlBridge
-keep class com.example.dynamicisland.MediaControlBridge { *; }

# ============================================
# COMPOSE
# ============================================
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.**

# ============================================
# KOTLIN
# ============================================
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ============================================
# COROUTINES
# ============================================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================
# ANDROID
# ============================================
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Activity

# Keep native method
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep View (untuk inflate dari XML)
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ============================================
# REMOVE LOG (opsional — hemat ruang)
# ============================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ============================================
# JANGAN WARN
# ============================================
-dontwarn org.jetbrains.annotations.**
-dontwarn javax.annotation.**
