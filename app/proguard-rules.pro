# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep data models
-keep class com.bp.uunwlm.model.** { *; }

# Firebase / Firestore / Auth
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.provider.FirebaseInitProvider
-keep class com.google.firebase.auth.FirebaseAuth
-keep class com.google.firebase.firestore.FirebaseFirestore
-keep class com.google.firebase.firestore.FirebaseFirestoreSettings
-keep class com.google.firebase.firestore.PersistentCacheSettings

# Supabase / OkHttp / Gson / Retrofit
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve line numbers for stack traces
-keepattributes SourceFile,LineNumberTable
