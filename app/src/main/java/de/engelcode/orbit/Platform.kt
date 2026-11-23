package de.engelcode.orbit

import android.app.Activity
import android.app.AlertDialog
import android.text.InputFilter
import android.widget.EditText
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.Executors

/** Android-Teil der Plattform-Funktionen (die Web-Version hat ihre eigene Platform.kt). */
object Platform {
    fun now(): Long = System.currentTimeMillis()
    fun tzOffset(now: Long): Int = TimeZone.getDefault().getOffset(now)
    fun systemLanguage(): String = Locale.getDefault().language

    /** „Als App installieren“ gibt es nur in der Web-Version. */
    fun canInstall(): Boolean = false
    fun showInstall() {}

    var activity: Activity? = null

    private val exec = Executors.newFixedThreadPool(3)
    private val pending = ArrayList<() -> Unit>()

    /** Internet-Anfrage im Hintergrund; das Ergebnis kommt über drain() im Spiel-Thread an. */
    fun http(method: String, url: String, body: String?, cb: (String?) -> Unit) {
        exec.execute {
            val res = try {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 8000
                c.requestMethod = if (method == "PATCH") "POST" else method
                if (method == "PATCH") c.setRequestProperty("X-HTTP-Method-Override", "PATCH")
                if (body != null) {
                    c.doOutput = true
                    c.setRequestProperty("Content-Type", "application/json")
                    c.outputStream.use { it.write(body.toByteArray()) }
                }
                if (c.responseCode in 200..299) c.inputStream.bufferedReader().use { it.readText() }
                else "\u0000" + (c.errorStream?.bufferedReader()?.use { it.readText() } ?: "")
            } catch (_: Throwable) { null }
            synchronized(pending) { pending.add { cb(res) } }
        }
    }

    fun drain() {
        val list: List<() -> Unit>
        synchronized(pending) {
            if (pending.isEmpty()) return
            list = ArrayList(pending); pending.clear()
        }
        for (f in list) try { f() } catch (_: Throwable) {}
    }

    /** Texteingabe (Name, Freundescode). */
    fun askText(title: String, current: String, maxLen: Int, secret: Boolean = false, cb: (String?) -> Unit) {
        val a = activity ?: return
        a.runOnUiThread {
            try {
                val e = EditText(a)
                e.setText(current)
                e.filters = arrayOf(InputFilter.LengthFilter(maxLen))
                e.setSingleLine()
                e.inputType = if (secret) android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    else android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                AlertDialog.Builder(a).setTitle(title).setView(e)
                    .setPositiveButton("OK") { _, _ -> val t = e.text.toString(); synchronized(pending) { pending.add { cb(t) } } }
                    .setNegativeButton("✕") { _, _ -> }
                    .show()
            } catch (_: Throwable) {}
        }
    }

    /** Spielstand als Text (für den Cloud-Speicher). */
    fun prefsExport(): String {
        val a = activity ?: return ""
        val all = a.getSharedPreferences("orbit", 0).all
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in all) {
            if (v == null) continue
            if (!first) sb.append(','); first = false
            sb.append(Json.q(k)).append(':').append(Json.q(v.toString()))
        }
        return sb.append('}').toString()
    }

    fun prefsImport(data: String) {
        val a = activity ?: return
        val m = Json.map(Json.parse(data))
        if (m.isEmpty()) return
        val e = a.getSharedPreferences("orbit", 0).edit().clear()
        for ((k, vv) in m) {
            val v = Json.str(vv)
            try {
                when (Save.typeOf(k, v)) {
                    'b' -> e.putBoolean(k, v == "true")
                    'l' -> e.putLong(k, v.toLong())
                    'f' -> e.putFloat(k, v.toFloat())
                    's' -> e.putString(k, v)
                    else -> e.putInt(k, v.toInt())
                }
            } catch (_: Throwable) {}
        }
        e.commit()
    }

    fun restart() {
        val a = activity ?: return
        a.runOnUiThread { a.recreate() }
    }
}
