# retrolambda
-dontwarn java.lang.invoke.*

# javadns
-dontwarn org.xbill.DNS.spi.*

# rxjava
-dontwarn rx.internal.util.unsafe.*
-keep class rx.internal.util.unsafe.** { *; }

# materialdrawer
-dontwarn com.mikepenz.materialdrawer.*

# iconics
-dontwarn com.mikepenz.iconics.*

# gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class sun.misc.Unsafe { *; }
-keep class com.google.gson.** { *; }

# universal-image-loader
-dontwarn com.nostra13.universalimageloader.**
-keep class com.nostra13.universalimageloader.** { *; }

# xsocks
-keep class io.github.xsocks.model.** { *; }