# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\rupal\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# Keep Firebase model classes to prevent R8 from stripping fields needed for serialization
-keep class com.example.jamuione.domain.model.** { *; }

# Keep Room entities
-keep class com.example.jamuione.data.local.entity.** { *; }
