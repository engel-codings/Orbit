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

    fun showForm(spec: FormSpec, cb: (String, Map<String, String>) -> Unit) {
        val sb = StringBuilder("{")
        sb.append("\"title\":").append(Json.q(spec.title)).append(",\"subtitle\":").append(Json.q(spec.subtitle))
        sb.append(",\"submit\":").append(Json.q(spec.submit)).append(",\"divider\":").append(Json.q(spec.divider))
        sb.append(",\"google\":").append(if (spec.google != null) Json.q(spec.google) else "null")
        sb.append(",\"fields\":[")
        spec.fields.forEachIndexed { i, f ->
            if (i > 0) sb.append(',')
            sb.append(Json.obj("key" to f.key, "hint" to f.hint, "type" to f.type, "value" to f.value, "max" to f.max))
        }
        sb.append("],\"links\":[")
        spec.links.forEachIndexed { i, l -> if (i > 0) sb.append(','); sb.append('[').append(Json.q(l.first)).append(',').append(Json.q(l.second)).append(']') }
        sb.append("]}")
        val json = sb.toString()
        try {
            val f: dynamic = js("window.orbitForm")
            f(json, { action: String, vals: String ->
                val m = Json.map(Json.parse(vals)).mapValues { Json.str(it.value) }
                cb(action, m)
            })
        } catch (e: Throwable) { println("FORMERR " + e) }
    }

    fun googleSignIn(cb: (String?) -> Unit) {
        val f: dynamic = js("window.orbitGoogle")
        f({ rt: String? -> cb(rt) })
    }

    fun googlePending(cb: (String?) -> Unit) {
        try {
            val f: dynamic = js("window.orbitGooglePending")
            f({ rt: String? -> cb(rt) })
        } catch (_: Throwable) {}
    }

    fun pickImage(cb: (String?) -> Unit) {
        val f: dynamic = js("window.orbitPickImage")
        f({ b: String? -> cb(b) })
    }

    fun decodeImage(b64: String, cb: (android.graphics.Bitmap?) -> Unit) {
        val f: dynamic = js("window.orbitDecodeImage")
        f(b64, { c: dynamic -> cb(if (c == null) null else android.graphics.Bitmap.wrap(c.unsafeCast<org.w3c.dom.HTMLCanvasElement>())) })
    }
}
