package com.kerenpandi.glasslauncher

import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

object ShizukuActions {
    private const val REQUEST_CODE = 7401
    private val executor = Executors.newSingleThreadExecutor()

    fun stopApp(context: Context, packageName: String) {
        if (!Shizuku.pingBinder()) {
            Toast.makeText(context, "Aktifkan layanan Shizuku terlebih dahulu", Toast.LENGTH_LONG).show()
            return
        }
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(REQUEST_CODE)
            Toast.makeText(context, "Izinkan Glass Launcher di aplikasi Shizuku, lalu ulangi", Toast.LENGTH_LONG).show()
            return
        }
        executor.execute {
            val result = runCatching {
                val process = Shizuku.newProcess(
                    arrayOf("sh", "-c", "am force-stop --user 0 " + packageName),
                    null,
                    null
                )
                val code = process.waitFor()
                process.destroy()
                code
            }.getOrElse { -1 }
            android.os.Handler(context.mainLooper).post {
                Toast.makeText(
                    context,
                    if (result == 0) "Aplikasi dihentikan" else "Gagal menghentikan aplikasi",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
