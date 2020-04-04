package me.iacn.biliroaming.hook

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers.*
import me.iacn.biliroaming.Constant.TAG
import me.iacn.biliroaming.XposedInit
import java.net.InetAddress

class CDNHook(classLoader: ClassLoader) : BaseHook(classLoader) {
    override fun startHook() {
        if (!XposedInit.sPrefs.getBoolean("use_cdn", false)) return;

        Log.d(TAG, "startHook: CDN")

        findAndHookMethod("java.net.InetAddress", mClassLoader,
                "getAllByName", String::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val host = param.args[0] as String
                val cdn: String = getCDN()
                if (cdn.isNotEmpty() && host == "upos-hz-mirrorakam.akamaized.net") {
                    param.result = arrayOf(InetAddress.getByName(cdn))
                    Log.d(TAG, "Replace by CDN: $cdn")
                }
            }
        })

        findAndHookMethod("java.net.InetAddress", mClassLoader,
                "getByName", String::class.java, object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun afterHookedMethod(param: MethodHookParam) {
                val host = param.args[0] as String
                val cdn: String = getCDN()
                if (cdn.isNotEmpty() && host == "upos-hz-mirrorakam.akamaized.net") {
                    param.result = InetAddress.getByName(cdn)
                    Log.d(TAG, "Replace by CDN: $cdn")
                }
            }
        })

        findAndHookMethod("tv.danmaku.ijk.media.player.IjkMediaPlayerItem", mClassLoader,
                "setItemOptions", object : XC_MethodHook() {
            @Throws(Throwable::class)
            override fun beforeHookedMethod(param: MethodHookParam) {
                val url = callMethod(param.thisObject, "mediaAssetToUrl", 0, 0) as String
                val cdn: String = getCDN()
                if (cdn.isNotEmpty() && url.contains("upos-hz-mirrorakam.akamaized.net")) {
                    val params = getObjectField(param.thisObject, "mIjkMediaConfigParams")
                    setObjectField(params, "mHttpProxy", "http://$cdn:80")
                    val proxy = getObjectField(params, "mHttpProxy") as String
                    Log.d(TAG, "Using cdn as proxy: $proxy")
                }
            }
        })
    }

    fun getCDN(): String {
        var cdn = XposedInit.sPrefs.getString("cdn", "")!!
        if (cdn.isEmpty()) cdn = XposedInit.sPrefs.getString("custom_cdn", "")!!
        return cdn
    }

}