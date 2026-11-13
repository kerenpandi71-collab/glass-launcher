package com.kerenpandi.glassberry

import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

object BerryShizuku {

    private const val REQUEST_CODE = 7402
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var permissionCallback: ((Boolean) -> Unit)? = null

    init {
        runCatching {
            Shizuku.addBinderReceivedListenerSticky { }
        }
        runCatching {
            Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
                if (requestCode == REQUEST_CODE) {
                    val callback = permissionCallback
                    permissionCallback = null
                    callback?.invoke(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            }
        }
    }

    fun isBinderAvailable(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun hasPermission(): Boolean =
        isBinderAvailable() && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun requestPermission(onResult: (Boolean) -> Unit) {
        if (!isBinderAvailable()) {
            onResult(false)
            return
        }
        permissionCallback = onResult
        runCatching {
            Shizuku.requestPermission(REQUEST_CODE)
        }.onFailure {
            permissionCallback = null
            onResult(false)
        }
    }

    fun exec(cmd: Array<String>, onResult: (code: Int, output: String) -> Unit) {
        executor.execute {
            val result = runCatching {
                val method = Shizuku::class.java
                    .getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    .apply { isAccessible = true }
                val process = method.invoke(null, cmd, null, null) as Process
                val output = process.inputStream.bufferedReader().readText() +
                    process.errorStream.bufferedReader().readText()
                val code = process.waitFor()
                runCatching { process.destroy() }
                code to output
            }.getOrElse { -1 to (it.message ?: "error") }
            mainHandler.post { onResult(result.first, result.second) }
        }
    }
}
