# Tell consumers to keep all classes that extend WXInterface
-keep class * extends com.dergoogler.mmrl.webui.interfaces.WXInterface {
    <init>(...);
    *;
}

-keep class com.dergoogler.mmrl.webui.interfaces.**
-keep class * extends com.dergoogler.mmrl.webui.interfaces.WXInterface