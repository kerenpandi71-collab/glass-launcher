package com.kerenpandi.glasslauncher

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

object ShizukuActions {

    private const val REQUEST_CODE = 7401
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

    fun stopApp(context: Context, packageName: String) {
        if (!packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
            Toast.makeText(context, "Nama paket aplikasi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }
        if (!isBinderAvailable()) {
            fallbackKill(context, packageName, "Shizuku tidak aktif")
            return
        }
        if (!hasPermission()) {
            requestPermission { granted ->
                if (granted) forceStop(context, packageName)
                else fallbackKill(context, packageName, "Izin Shizuku ditolak")
            }
            return
        }
        forceStop(context, packageName)
    }

    private fun forceStop(context: Context, packageName: String) {
        executor.execute {
            val exitCode = runCatching {
                val method = Shizuku::class.java
                    .getDeclaredMethod(
                        "newProcess",
                        Array<String>::class.java,
                        Array<String>::class.java,
                        String::class.java
                    )
                    .apply { isAccessible = true }
                val remote = method.invoke(
                    null,
                    arrayOf("am", "force-stop", "--user", "0", packageName),
                    null,
                    null
                )
                val code = remote.javaClass.getMethod("waitFor").invoke(remote) as Int
                runCatching { remote.javaClass.getMethod("destroy").invoke(remote) }
                code
            }.getOrElse { -1 }
            mainHandler.post {
                val message = if (exitCode == 0) "Aplikasi dihentikan" else "Perintah Shizuku gagal"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fallbackKill(context: Context, packageName: String, reason: String) {
        runCatching {
            val activityManager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
        }
        Toast.makeText(
            context,
            "$reason — memakai hentikan proses latar biasa",
            Toast.LENGTH_LONG
        ).show()
    }
}
