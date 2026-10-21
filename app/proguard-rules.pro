# Google API Client rules
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-dontwarn javax.annotation.**
-dontwarn org.apache.commons.logging.**
-dontwarn com.google.android.gms.auth.**
-dontwarn com.google.android.gms.common.**
-dontwarn javax.naming.**
-dontwarn org.ietf.jgss.**
-dontwarn org.joda.time.**

# AndroidX Security Crypto & Tink
-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn androidx.security.crypto.**
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# Google Auth Library rules
-keep class com.google.auth.** { *; }
-dontwarn com.google.auth.**
-keep class com.google.api.client.json.gson.** { *; }
-keep class com.google.gson.** { *; }

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# App Application, ViewModels and UI (Evita NoSuchMethodException no ViewModelProvider e crash no arranque)
-keep class com.portzone.app.PortZoneApp { *; }
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
    <fields>;
    <methods>;
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(...);
    <fields>;
    <methods>;
}
-keep class com.portzone.app.ui.** { *; }
-keep class com.portzone.app.data.model.** { *; }
-keep class com.portzone.app.data.service.** { *; }
-keep class com.portzone.app.data.repository.** { *; }
-keep class com.portzone.app.data.saf.** { *; }
