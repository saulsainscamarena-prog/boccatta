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
-keepclassmembers class com.bocatta.pos.domain.model.** {
    <fields>;
}

# ── kotlinx.serialization — mantener clases @Serializable (el plugin ya genera reglas, esto es safe-guard)
-keepclassmembers class com.bocatta.pos.** {
    @kotlinx.serialization.Serializable <fields>;
}

# ── Koin (no requiere reglas especiales con el DSL module { single { ... } })