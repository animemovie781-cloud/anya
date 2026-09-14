# App-specific R8 rules belong here.
# Library consumer rules and Android's optimized defaults currently cover release builds.

# GeckoView
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**
-dontwarn org.mozilla.javascript.**

# Retrofit / OkHttp / Moshi
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn com.squareup.moshi.**

# Coroutines
-dontwarn kotlinx.coroutines.**

# Java WebSocket
-dontwarn org.java_websocket.**
