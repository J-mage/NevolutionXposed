package com.notxx.xposed.hook

import android.os.Bundle

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

import com.oasisfeng.nevo.decorators.wechat.WeChatDecorator.EXTRA_PICTURE_PATH
import com.oasisfeng.nevo.xposed.BuildConfig

import com.notxx.xposed.XLog
import com.notxx.xposed.hookConstructor
import com.notxx.xposed.hookMethod

class FileOutputStream {
	companion object {
		val TAG = "WeChatDecorator.FileOutputStream"
		val PATH = "path"
		val CREATED = "created"
		fun now() = System.currentTimeMillis()
	}

	private var mPath: String? = null
	private var mCreated: Long? = null
	private var mClosed: Long? = null

	fun inject(msg: String, extras: Bundle) {
		val closed = mClosed;
		if ("[图片]" == msg && mPath != null && closed != null && closed - now() < 1000) {
			synchronized (this) {
				if (BuildConfig.DEBUG) XLog.d(TAG, "putString $mPath")
				extras.putString(EXTRA_PICTURE_PATH, mPath) // 保存图片地址
				mPath = null
				mClosed = null
			}
		}
	}

	fun hook(loadPackageParam: XC_LoadPackage.LoadPackageParam) {
		val clazz = java.io.FileOutputStream::class.java
		// FileOutputStream(String name, boolean append)
		clazz.hookConstructor(String::class.java, java.lang.Boolean.TYPE) {
			doBefore {
				val path = args[0] as String?
				if (path == null) return@doBefore
				val created = now()
				if (path.endsWith("/test_writable") || path.endsWith("/xlogtest_writable")) return@doBefore
				if (!path.contains("/image2/")) {
					XLog.d(TAG, "$created (file, append) ? $path")
					return@doBefore
				}
				XposedHelpers.setAdditionalInstanceField(thisObject, PATH, path)
				XposedHelpers.setAdditionalInstanceField(thisObject, CREATED, created)
				XLog.d(TAG, "created: $created path: $path")
			}
		}
		clazz.hookMethod("close") {
			doAfter {
				val path = XposedHelpers.getAdditionalInstanceField(thisObject, PATH) as String?
				if (path == null) return@doAfter
				val created = XposedHelpers.getAdditionalInstanceField(thisObject, CREATED) as Long
				val closed = now()
				if (BuildConfig.DEBUG) XLog.d(TAG, "$created => $closed $path")
				synchronized (this) {
					mPath = path
					mCreated = created
					mClosed = closed
				}
			}
		}
	}
}