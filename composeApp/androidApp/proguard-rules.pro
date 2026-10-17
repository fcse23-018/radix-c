# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Koin
-keep class org.koin.** { *; }

# Keep SQLDelight
-keep class app.cash.sqldelight.** { *; }

# Keep TensorFlow Lite
-keep class org.tensorflow.** { *; }

# Keep data classes
-keep class com.terrochroma.shared.domain.model.** { *; }

# Keep OpenCV JNI
-keep class org.opencv.** { *; }

# Keep serialization
-keepattributes InnerClasses
-keepattributes Signature
-keepattributes *Annotation*

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
