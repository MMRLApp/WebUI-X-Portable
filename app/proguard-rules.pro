-verbose
-optimizationpasses 5

# -dontobfuscate

-dontwarn org.conscrypt.**
-dontwarn kotlinx.serialization.**

# Keep DataStore fields
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* {
   <fields>;
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-keep class androidx.compose.ui.graphics.Color { *; }
-keep class androidx.compose.material3.ButtonColors { *; }
-keep class androidx.compose.material3.CardColors { *; }
-keep class androidx.compose.material3.ColorScheme { *; }