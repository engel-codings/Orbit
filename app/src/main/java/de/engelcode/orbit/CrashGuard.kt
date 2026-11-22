package de.engelcode.orbit

import android.content.Context
import android.os.Build
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Absturz-Schutz: meldet Fehler an die Online-Datenbank (damit sie behoben werden können)
 * und schaltet nach einem unerwarteten Beenden auf die sichere Software-Grafik um.
 */
object CrashGuard {
    @Volatile var info = ""
    var softRender = false; private set
    private var ctx: Context? = null
    private val sent = HashSet<String>()

    fun init(c: Context) {
        ctx = c.applicationContext
        val p = c.getSharedPreferences("orbitsys", Context.MODE_PRIVATE)
        val abnormal = p.getBoolean("alive", false)
        var count = p.getInt("abnormal", 0)
        if (abnormal) {
            count++
            Thread { send("unexpected-exit", "count=$count last=" + p.getString("info", "")) }.start()
        }
        softRender = count >= 1
        p.edit().putInt("abnormal", count).putBoolean("alive", true).apply()
        val def = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                ctx?.getSharedPreferences("orbitsys", Context.MODE_PRIVATE)?.edit()?.putBoolean("alive", false)?.commit()
                val th = Thread { send("crash", trace(e)) }
                th.start(); th.join(3000)
            } catch (_: Throwable) {}
            def?.uncaughtException(t, e)
        }
    }

    fun setAlive(alive: Boolean) {
        val c = ctx ?: return
        c.getSharedPreferences("orbitsys", Context.MODE_PRIVATE).edit()
            .putBoolean("alive", alive).putString("info", info).apply()
    }

    fun saveInfo() {
        ctx?.getSharedPreferences("orbitsys", Context.MODE_PRIVATE)?.edit()?.putString("info", info)?.apply()
    }

    /** Fehler im Spiel-Thread: melden, aber weiterspielen. */
    fun caught(e: Throwable) {
        val t = trace(e)
        val key = t.take(200)
        synchronized(sent) { if (!sent.add(key) || sent.size > 5) return }
        Thread { send("caught", t) }.start()
    }

    private fun trace(e: Throwable): String {
        val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
        return sw.toString().take(3000) + "\ninfo=" + info
    }

    private fun send(kind: String, msg: String) {
        try {
            val body = Json.obj("kind" to kind, "v" to Game.VERSION_NAME, "model" to (Build.MANUFACTURER + " " + Build.MODEL),
                "sdk" to Build.VERSION.SDK_INT, "soft" to softRender, "msg" to msg, "t" to System.currentTimeMillis())
            val c = URL(Net.BASE + "/crashes.json").openConnection() as HttpURLConnection
            c.connectTimeout = 3000; c.readTimeout = 3000
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.toByteArray()) }
            c.responseCode
        } catch (_: Throwable) {}
    }
}
