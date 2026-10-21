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
