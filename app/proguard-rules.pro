# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.aipdf.assistant.ai.models.** { *; }
-keep class com.aipdf.assistant.ai.providers.** { *; }
-keep class com.aipdf.assistant.network.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.google.gson.**
# PdfBox
-dontwarn com.tom_roush.pdfbox.**
-keep class com.tom_roush.pdfbox.** { *; }
# PdfBox references optional desktop / JVM classes that are not present on
# Android. R8 reports these as missing-class errors; suppress them.
-dontwarn java.awt.**
-dontwarn javax.imageio.**
-dontwarn javax.swing.**
-dontwarn javax.media.jai.**
-dontwarn org.bouncycastle.**
-dontwarn org.apache.**
-dontwarn com.gemalto.jp2.**
-dontwarn com.sun.**
-dontwarn java.lang.management.**
-dontwarn java.applet.**
-dontwarn java.rmi.**
-dontwarn javax.activation.**
-dontwarn javax.xml.stream.**
-dontwarn org.slf4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn android.util.**
# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
# General safety net for minified release builds
-ignorewarnings
