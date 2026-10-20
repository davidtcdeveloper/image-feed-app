# R8 / Proguard Rules for Android Image Feed App

# --- Kotlinx Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Keep serializer companion objects and fields for @Serializable classes
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}

-keepclassmembers class *$Companion {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,allowobfuscation,allowshrinking class * implements kotlinx.serialization.KSerializer

# --- Navigation 3 & Route Hierarchy ---
-keep class * implements androidx.navigation3.runtime.NavKey { *; }
-keep class com.example.imagefeed.android.navigation.AppRoute* { *; }
-keep class * implements java.io.Serializable { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# --- Metro Compile-Time Dependency Injection ---
-keep class dev.zacsweers.metro.** { *; }
-keep class com.example.imagefeed.di.** { *; }
-keep class * extends dev.zacsweers.metro.DependencyGraph { *; }
-keep class * implements dev.zacsweers.metro.DependencyGraph { *; }
-keep @dev.zacsweers.metro.DependencyGraph class * { *; }
-keep @dev.zacsweers.metro.BindingContainer class * { *; }
-keepclassmembers class * {
    @dev.zacsweers.metro.Provides <methods>;
    @dev.zacsweers.metro.Inject <init>(...);
    @dev.zacsweers.metro.Inject <methods>;
    @dev.zacsweers.metro.Inject <fields>;
}

# --- Google Play Services Maps & Maps Compose ---
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# --- Networking (Ktor, OkHttp, Okio) & Image Loading (Coil) ---
-dontwarn okhttp3.**
-dontwarn io.ktor.**
-dontwarn okio.**
-keep class coil3.** { *; }
