# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.aipdf.assistant.ai.models.** { *; }
-keep class com.aipdf.assistant.ai.providers.** { *; }
-keep class com.aipdf.assistant.network.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
# PdfBox
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
# Room
-keep class * extends androidx.room.RoomDatabase
