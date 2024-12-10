# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/dominik/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Behalte alle Klassen, Methoden und Felder für Reflection
-keep class com.example.myapp.** { *; }
-keepclassmembers class * {
    public <methods>;
}

# Behalte Android-spezifische Klassen
-keep class androidx.preference.** { *; }
#-keep class com.google.android.gms.oss.licenses.OssLicensesMenuActivity { *; }

# Behalte Google Play Services und andere externe Bibliotheken
#-keep class com.google.android.gms.** { *; }
-keep class com.google.** { *; }
-keep class androidx.appcompat.** { *; }
-keep class com.google.android.material.** { *; }
# Behalte alle Google Play Services OSS-Lizenz-Aktivitäten und deren Klassen
#-keep class com.google.android.gms.oss.licenses.OssLicensesMenuActivity { *; }

# Behalte alle Klassen, die mit Google Play Services in Zusammenhang stehen
#-keep class com.google.android.gms.** { *; }

# Entferne nicht genutzte Ressourcen
-dontwarn com.google.**
-dontwarn androidx.**

# Optimierung von nicht genutztem Code
-dontwarn android.support.**
-dontwarn androidx.**
