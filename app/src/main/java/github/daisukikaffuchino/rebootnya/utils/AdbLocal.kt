package github.daisukikaffuchino.rebootnya.utils

import android.content.Context
import dadb.AdbKeyPair
import dadb.Dadb
import java.io.File
import java.io.IOException

/**
 * Talks to the device's own adbd over loopback (127.0.0.1:5555), exactly like `adb shell`
 * from a computer would. Needs neither root nor Shizuku, only "network/wireless debugging"
 * enabled on the device. The first connection shows the usual "Allow USB debugging?" prompt.
 */
object AdbLocal {
    private const val HOST = "127.0.0.1"
    private const val DEFAULT_PORT = 5555

    /** Time allowed for a normal connection + command. */
    const val TIMEOUT_MS = 12_000L

    /** Time allowed while the user may still be answering the authorization prompt. */
    const val AUTH_TIMEOUT_MS = 60_000L

    sealed class Result {
        data class Ok(val exitCode: Int, val output: String) : Result()
        /** adbd unreachable or authorization refused / not answered in time. */
        data class Unavailable(val reason: String) : Result()
        data class Failed(val reason: String) : Result()
    }

    private fun keyPair(context: Context): AdbKeyPair {
        val dir = context.filesDir
        val priv = File(dir, "adbkey")
        val pub = File(dir, "adbkey.pub")
        if (!priv.exists() || !pub.exists()) {
            AdbKeyPair.generate(priv, pub)
        }
        return AdbKeyPair.read(priv, pub)
    }

    /** Ports adbd may be listening on, most likely first. */
    private fun candidatePorts(): List<Int> {
        val ports = linkedSetOf<Int>()
        for (prop in arrayOf("service.adb.tcp.port", "persist.adb.tcp.port")) {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("getprop", prop))
                val v = p.inputStream.bufferedReader().readText().trim().toIntOrNull()
                p.waitFor()
                if (v != null && v in 1..65535) ports.add(v)
            } catch (_: Exception) {
            }
        }
        ports.add(DEFAULT_PORT)
        return ports.toList()
    }

    /**
     * Runs [command] through adbd. Blocking: never call from the main thread.
     * With [expectDisconnect] a dropped connection after the command was sent
     * (the device is rebooting) counts as success.
     */
    fun run(
        context: Context,
        command: String,
        expectDisconnect: Boolean = false,
        timeoutMs: Long = TIMEOUT_MS
    ): Result {
        val keys = try {
            keyPair(context.applicationContext)
        } catch (e: Exception) {
            return Result.Failed("key: ${e.message}")
        }

        var result: Result = Result.Unavailable("no adbd")
        val worker = Thread {
            var dadb: Dadb? = null
            try {
                var lastError: Exception? = null
                for (port in candidatePorts()) {
                    try {
                        dadb = Dadb.create(HOST, port, keys)
                        // Forces the connection and (first time) the authorization prompt.
                        dadb.shell("true")
                        NyaSettings.preferences.edit().putBoolean("adb_authorized", true).apply()
                        lastError = null
                        break
                    } catch (e: Exception) {
                        lastError = e
                        try {
                            dadb?.close()
                        } catch (_: Exception) {
                        }
                        dadb = null
                    }
                }
                val d = dadb
                if (d == null) {
                    result = Result.Unavailable(lastError?.message ?: "connect failed")
                    return@Thread
                }
                result = try {
                    val r = d.shell(command)
                    Result.Ok(r.exitCode, r.allOutput)
                } catch (e: IOException) {
                    if (expectDisconnect) Result.Ok(0, "") else Result.Failed(e.message ?: "io")
                } catch (e: Exception) {
                    Result.Failed(e.message ?: e.javaClass.simpleName)
                }
            } catch (e: Throwable) {
                result = Result.Unavailable(e.message ?: e.javaClass.simpleName)
            } finally {
                try {
                    dadb?.close()
                } catch (_: Exception) {
                }
            }
        }
        worker.isDaemon = true
        worker.start()
        worker.join(timeoutMs)
        if (worker.isAlive) {
            worker.interrupt()
            return Result.Unavailable("timeout")
        }
        return result
    }

    /** Opens a connection only (triggers the authorization prompt). Blocking. */
    fun authorize(context: Context): Result =
        run(context, "true", timeoutMs = AUTH_TIMEOUT_MS)
}
