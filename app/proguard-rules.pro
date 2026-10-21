# Google API Client rules
-keep class com.google.api.services.drive.** { *; }
-keep class com.google.api.client.** { *; }
-keepclassmembers class * {
  @com.google.api.client.util.Key <fields>;
}
-dontwarn javax.annotation.**
-dontwarn org.apache.commons.logging.**

# AndroidX Security Crypto & Tink
-keepattributes Signature,InnerClasses,EnclosingMethod
-dontwarn androidx.security.crypto.**
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
