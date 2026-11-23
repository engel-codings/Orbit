package de.engelcode.orbit

import kotlinx.browser.window
import kotlin.js.Date

/** Web-Teil der Plattform-Funktionen. */
object Platform {
    fun now(): Long = Date.now().toLong()
    fun tzOffset(now: Long): Int = (-Date(now.toDouble()).getTimezoneOffset() * 60000.0).toInt()
    fun systemLanguage(): String = window.navigator.language.substringBefore('-').lowercase()

    /** Kann die Seite als App auf den Home-Bildschirm gelegt werden? (siehe index.html) */
    fun canInstall(): Boolean = try {
        js("window.orbitCanInstall ? window.orbitCanInstall() : false") as Boolean
    } catch (_: Throwable) { false }

    fun showInstall() {
        try { js("if (window.orbitShowInstall) window.orbitShowInstall()") } catch (_: Throwable) {}
    }

    fun http(method: String, url: String, body: String?, cb: (String?) -> Unit) {
        try {
            val init: dynamic = js("({})")
            init.method = method
            if (body != null) { init.body = body; init.headers = js("({'Content-Type':'application/json'})") }
            val f: dynamic = js("fetch")
            f(url, init).then({ r: dynamic ->
                val ok = r.ok as Boolean
                r.text().then({ t: dynamic -> cb(if (ok) t as String else "\u0000" + (t as String)) }, { _: dynamic -> cb(null) })
                null
            }, { _: dynamic -> cb(null) })
        } catch (_: Throwable) { cb(null) }
    }

    fun drain() {}

    fun askText(title: String, current: String, maxLen: Int, secret: Boolean = false, cb: (String?) -> Unit) {
        val r = window.prompt(title, current) ?: return
        cb(r.take(maxLen))
    }

    fun prefsExport(): String {
        val ls = kotlinx.browser.localStorage
        val sb = StringBuilder("{")
        var first = true
        for (i in 0 until ls.length) {
            val k = ls.key(i) ?: continue
            if (!k.startsWith("orbit.")) continue
            val v = ls.getItem(k) ?: continue
            if (!first) sb.append(','); first = false
            sb.append(Json.q(k.removePrefix("orbit."))).append(':').append(Json.q(v))
        }
        return sb.append('}').toString()
    }

    fun prefsImport(data: String) {
        val m = Json.map(Json.parse(data))
        if (m.isEmpty()) return
        val ls = kotlinx.browser.localStorage
        val old = (0 until ls.length).mapNotNull { ls.key(it) }.filter { it.startsWith("orbit.") }
        for (k in old) ls.removeItem(k)
        for ((k, v) in m) ls.setItem("orbit.$k", Json.str(v))
    }

    fun restart() { window.location.reload() }
}
