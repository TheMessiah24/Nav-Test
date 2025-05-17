// Proguard rules for Mapbox Navigation App
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keep class com.mapbox.** { *; }
-keep interface com.mapbox.** { *; }

-dontwarn com.mapbox.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn java.lang.invoke.*

# Keep model classes
-keep class com.mapbox.navigation.data.** { *; }

# Keep Gson stuff
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Android stuff
-keep class androidx.** { *; }
-keep interface androidx.** { *; }
-keep class android.** { *; }
-keep interface android.** { *; }

# Keep generated files
-keep class com.mapbox.navigation.databinding.** { *; }
