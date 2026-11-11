package com.kerenpandi.glasslauncher

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

    fun stopApp(context: Context, packageName: String) {
        if (!packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) {
            Toast.makeText(context, "Nama paket aplikasi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }
        if (!Shizuku.pingBinder()) {
            Toast.makeText(context, "Aktifkan layanan Shizuku terlebih dahulu", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
            Toast.makeText(context, "Izinkan Glass Launcher di Shizuku, lalu ulangi", Toast.LENGTH_LONG).show()
            return
        }
        executor.execute {
            val exitCode = runCatching {
                val method = Shizuku::class.java.getDeclaredMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java).apply { isAccessible = true }
                val remote = method.invoke(null, arrayOf("sh", "-c", "am force-stop --user 0 $packageName"), null, null)
                val code = remote.javaClass.getMethod("waitFor").invoke(remote) as Int
                runCatching { remote.javaClass.getMethod("destroy").invoke(remote) }
                code
            }.getOrElse { -1 }
            Handler(Looper.getMainLooper()).post {
                val message = if (exitCode == 0) "Aplikasi dihentikan" else "Perintah Shizuku gagal"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
