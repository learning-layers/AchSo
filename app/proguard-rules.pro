# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Applications/Android Studio.app/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

#####
# General
-dontobfuscate
-keepattributes Signature, *Annotation*

#####
# Retrofit
-dontwarn rx.**
-dontwarn com.google.appengine.api.urlfetch.*

-keep class retrofit.** { *; }
-keepclasseswithmembers class * {
    @retrofit.http.* <methods>;
}

#####
# Gson
-keep class sun.misc.Unsafe { *; }

#####
# Okio
-dontwarn java.nio.file.*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

#####
# OkHttp
-dontwarn com.squareup.okhttp.**
-keepnames class com.squareup.okhttp.** { *; }
-keepnames interface com.squareup.okhttp.** { *; }

#####
# Otto
-keepclassmembers class ** {
    @com.squareup.otto.Subscribe public *;
    @com.squareup.otto.Produce public *;
}

#####
# Joda-Time
-dontwarn org.joda.convert.**

#####
# mp4parser
-keep class com.coremedia.iso.boxes.** { *; }

#####
# Guava
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue

#####
# Support library v7
-keep class android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

#####
# Support library v4
-keep class android.support.v4.** { *; }
-keep interface android.support.v4.** { *; }

#####
# Google Play Services
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Apache HTTP client
-keep class org.apache.http.** { *; }
-dontwarn org.apache.http.*

-keepclassmembers class fi.aalto.legroup.achso.sharing.AchRailsJavascriptInterface {
    public *;
}

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
