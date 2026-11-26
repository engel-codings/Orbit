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
    fun hasPcMode(): Boolean = false
    fun setPcMode(on: Boolean, lang: Int, fullscreen: Boolean) {}

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

    // ------------------------------------------------------------------ Modernes Formular

    private fun dp(v: Float): Int = (v * (activity?.resources?.displayMetrics?.density ?: 2f)).toInt()

    private fun post(f: () -> Unit) { synchronized(pending) { pending.add(f) } }

    fun showForm(spec: FormSpec, cb: (String, Map<String, String>) -> Unit) {
        val a = activity ?: return
        a.runOnUiThread {
            try { buildForm(a, spec, cb) } catch (_: Throwable) {}
        }
    }

    private fun buildForm(a: Activity, spec: FormSpec, cb: (String, Map<String, String>) -> Unit) {
        val bold = try { android.graphics.Typeface.createFromAsset(a.assets, "fonts/Outfit-ExtraBold.ttf") } catch (_: Throwable) { android.graphics.Typeface.DEFAULT_BOLD }
        val med = try { android.graphics.Typeface.createFromAsset(a.assets, "fonts/Outfit-Medium.ttf") } catch (_: Throwable) { android.graphics.Typeface.DEFAULT }
        val d = android.app.Dialog(a, android.R.style.Theme_Translucent_NoTitleBar)
        var done = false
        fun finish(action: String, vals: Map<String, String>) {
            if (done) return
            done = true
            d.dismiss()
            post { cb(action, vals) }
        }
        val root = android.widget.FrameLayout(a)
        root.setBackgroundColor(0xCC05070F.toInt())
        root.setOnClickListener { finish("cancel", emptyMap()) }
        val scroll = android.widget.ScrollView(a)
        scroll.isFillViewport = false
        val card = android.widget.LinearLayout(a)
        card.orientation = android.widget.LinearLayout.VERTICAL
        card.setPadding(dp(22f), dp(22f), dp(22f), dp(20f))
        card.isClickable = true
        val bg = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF1E2550.toInt(), 0xFF141936.toInt()))
        bg.cornerRadius = dp(26f).toFloat()
        bg.setStroke(dp(1f), 0x40FFFFFF)
        card.background = bg

        val head = android.widget.LinearLayout(a)
        head.orientation = android.widget.LinearLayout.HORIZONTAL
        head.gravity = android.view.Gravity.CENTER_VERTICAL
        val title = android.widget.TextView(a)
        title.text = spec.title; title.setTextColor(-1); title.textSize = 24f; title.typeface = bold
        head.addView(title, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
        val close = android.widget.TextView(a)
        close.text = "✕"; close.setTextColor(0xFF9AA3C7.toInt()); close.textSize = 20f
        close.setPadding(dp(10f), dp(2f), dp(4f), dp(2f))
        close.setOnClickListener { finish("cancel", emptyMap()) }
        head.addView(close)
        card.addView(head)
        if (spec.subtitle.isNotEmpty()) {
            val sub = android.widget.TextView(a)
            sub.text = spec.subtitle; sub.setTextColor(0xFF9AA3C7.toInt()); sub.textSize = 14f; sub.typeface = med
            sub.setPadding(0, dp(4f), 0, dp(6f))
            card.addView(sub)
        }

        fun pill(c1: Int, c2: Int, r: Float): android.graphics.drawable.GradientDrawable {
            val g = android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(c1, c2))
            g.cornerRadius = dp(r).toFloat(); return g
        }

        val inputs = ArrayList<Pair<String, EditText>>()
        for (f in spec.fields) {
            val e = EditText(a)
            e.hint = f.hint; e.setText(f.value)
            e.setTextColor(-1); e.setHintTextColor(0xFF6E779C.toInt()); e.textSize = 17f; e.typeface = med
            e.setSingleLine()
            e.filters = arrayOf(InputFilter.LengthFilter(f.max))
            e.inputType = when (f.type) {
                FormField.EMAIL -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                FormField.PASSWORD -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                else -> android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            }
            if (f.type == FormField.PASSWORD) e.transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            val eb = android.graphics.drawable.GradientDrawable()
            eb.setColor(0xFF0D1230.toInt()); eb.cornerRadius = dp(16f).toFloat(); eb.setStroke(dp(1f), 0x33FFFFFF)
            e.background = eb
            e.setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
            val lp = android.widget.LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(12f)
            card.addView(e, lp)
            inputs.add(f.key to e)
        }
        fun values() = inputs.associate { it.first to it.second.text.toString() }

        val btn = android.widget.TextView(a)
        btn.text = spec.submit; btn.setTextColor(-1); btn.textSize = 18f; btn.typeface = bold
        btn.gravity = android.view.Gravity.CENTER
        btn.background = pill(0xFF4CC9F0.toInt(), 0xFF7B61FF.toInt(), 28f)
        btn.setPadding(0, dp(15f), 0, dp(15f))
        btn.setOnClickListener { finish("submit", values()) }
        val blp = android.widget.LinearLayout.LayoutParams(-1, -2); blp.topMargin = dp(18f)
        card.addView(btn, blp)

        if (spec.google != null) {
            val or = android.widget.TextView(a)
            or.text = spec.divider; or.setTextColor(0xFF6E779C.toInt()); or.textSize = 13f; or.gravity = android.view.Gravity.CENTER
            or.setPadding(0, dp(12f), 0, 0)
            card.addView(or)
            val g = android.widget.TextView(a)
            g.text = "G   " + spec.google; g.setTextColor(0xFF1F1F1F.toInt()); g.textSize = 17f; g.typeface = bold
            g.gravity = android.view.Gravity.CENTER
            val gb = android.graphics.drawable.GradientDrawable(); gb.setColor(-1); gb.cornerRadius = dp(28f).toFloat()
            g.background = gb
            g.setPadding(0, dp(14f), 0, dp(14f))
            g.setOnClickListener { finish("google", values()) }
            val glp = android.widget.LinearLayout.LayoutParams(-1, -2); glp.topMargin = dp(10f)
            card.addView(g, glp)
        }
        for ((act, label) in spec.links) {
            val t = android.widget.TextView(a)
            t.text = label; t.setTextColor(0xFF4CC9F0.toInt()); t.textSize = 15f; t.typeface = med
            t.gravity = android.view.Gravity.CENTER
            t.setPadding(0, dp(14f), 0, dp(2f))
            t.setOnClickListener { finish(act, values()) }
            card.addView(t)
        }
        if (inputs.isNotEmpty()) {
            inputs.last().second.imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            inputs.last().second.setOnEditorActionListener { _, _, _ -> finish("submit", values()); true }
        }
        val clp = android.widget.FrameLayout.LayoutParams(-1, -2)
        clp.gravity = android.view.Gravity.CENTER
        clp.setMargins(dp(20f), dp(30f), dp(20f), dp(30f))
        scroll.addView(card)
        root.addView(scroll, clp)
        d.setContentView(root)
        d.setOnCancelListener { finish("cancel", emptyMap()) }
        d.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        d.show()
        inputs.firstOrNull()?.second?.requestFocus()
    }

    // ------------------------------------------------------------------ Google-Anmeldung

    /** Öffnet die Google-Anmeldung im Browser; das Ergebnis kommt per orbit://auth zurück. */
    fun googleSignIn(cb: (String?) -> Unit) {
        googleCb = cb
        val a = activity ?: return
        a.runOnUiThread {
            try {
                a.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://engel-codings.github.io/Orbit/google.html?app=android")))
            } catch (_: Throwable) { post { cb(null) } }
        }
    }

    private var googleCb: ((String?) -> Unit)? = null

    fun googlePending(cb: (String?) -> Unit) {}

    fun onAuthIntent(uri: android.net.Uri?) {
        if (uri == null || uri.scheme != "orbit") return
        val rt = uri.getQueryParameter("rt")
        val cb = googleCb ?: return
        googleCb = null
        post { cb(rt) }
    }

    // ------------------------------------------------------------------ Bilder

    private var imageCb: ((String?) -> Unit)? = null
    const val PICK_IMAGE = 4711

    fun pickImage(cb: (String?) -> Unit) {
        imageCb = cb
        val a = activity ?: return
        a.runOnUiThread {
            try {
                val i = android.content.Intent(android.content.Intent.ACTION_GET_CONTENT)
                i.type = "image/*"
                a.startActivityForResult(i, PICK_IMAGE)
            } catch (_: Throwable) { post { cb(null) } }
        }
    }

    fun onImagePicked(uri: android.net.Uri?) {
        val cb = imageCb ?: return
        imageCb = null
        val a = activity
        if (uri == null || a == null) { post { cb(null) }; return }
        exec.execute {
            val res = try {
                val src = a.contentResolver.openInputStream(uri).use { android.graphics.BitmapFactory.decodeStream(it) }
                val s = minOf(src.width, src.height)
                val crop = android.graphics.Bitmap.createBitmap(src, (src.width - s) / 2, (src.height - s) / 2, s, s)
                val small = android.graphics.Bitmap.createScaledBitmap(crop, 128, 128, true)
                val out = java.io.ByteArrayOutputStream()
                small.compress(android.graphics.Bitmap.CompressFormat.JPEG, 78, out)
                android.util.Base64.encodeToString(out.toByteArray(), android.util.Base64.NO_WRAP)
            } catch (_: Throwable) { null }
            post { cb(res) }
        }
    }

    fun decodeImage(b64: String, cb: (android.graphics.Bitmap?) -> Unit) {
        exec.execute {
            val bmp = try {
                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Throwable) { null }
            post { cb(bmp) }
        }
    }
}
