# Add project specific ProGuard rules here.
-keep class com.avitrisphare.whiteboard.** { *; }

# Keep Room entities
-keep class com.avitrisphare.whiteboard.data.local.** { *; }
-keep class com.avitrisphare.whiteboard.domain.model.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# Keep Gson classes
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Keep Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ZXing
-dontwarn com.google.zxing.**
-keep class com.google.zxing.** { *; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Motion prediction & DataStore
-dontwarn androidx.input.motionprediction.**
-dontwarn androidx.datastore.**
