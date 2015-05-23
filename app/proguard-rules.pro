
# realm
-keepnames public class * extends io.realm.RealmObject
-keep class io.realm.** { *; }
-dontwarn javax.**
-dontwarn io.realm.**

# retrolambda
-dontwarn java.lang.invoke.*

# javadns
-dontwarn org.xbill.DNS.spi.*

# rxjava
-dontwarn rx.internal.util.unsafe.*

# snackbar
-dontwarn com.nispok.snackbar.*

# materialdrawer
-dontwarn com.mikepenz.materialdrawer.*

# iconics
-dontwarn com.mikepenz.iconics.*