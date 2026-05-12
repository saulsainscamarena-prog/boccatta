# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ── Bocatta modelos — evitar que Proguard elimine campos usados por Firestore toObject()
-keep class com.bocatta.pos.domain.model.** { *; }

# ── Firebase / Firestore
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── kotlinx.serialization — mantener clases @Serializable (el plugin ya genera reglas, esto es safe-guard)
-keepclassmembers class com.bocatta.pos.** {
    @kotlinx.serialization.Serializable <fields>;
}
-dontwarn kotlin.**

# ── Koin — mantener constructores (Koin usa reflection para instanciar)
-keep class * {
    org.koin.core.annotation.Single *;
    org.koin.core.annotation.Factory *;
}