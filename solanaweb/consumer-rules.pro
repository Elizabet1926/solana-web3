# Keep SolanaWeb and bridge public API for consumers who enable minify/R8
-keep class com.elizabet1926.solanaweb.SolanaWeb { *; }
-keep class com.elizabet1926.solanaweb.bridge.WebViewJavascriptBridge { *; }
-keep class com.elizabet1926.solanaweb.bridge.WebViewJavascriptBridge$AndroidBridge { *; }
# Keep @JavascriptInterface methods so JS can call into Android
-keepclassmembers class com.elizabet1926.solanaweb.bridge.WebViewJavascriptBridge$AndroidBridge {
    @android.webkit.JavascriptInterface <methods>;
}
