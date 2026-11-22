@file:Suppress("unused", "UNUSED_PARAMETER", "PackageDirectoryMismatch")

package android.graphics

import kotlinx.browser.document
import org.w3c.dom.CanvasGradient
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.Path2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/*
 * Kleine Nachbildung der Android-Grafik-Klassen auf Basis von HTML5-Canvas.
 * So läuft derselbe Spiel-Code (shared/) unverändert im Browser.
 */

object Color {
    const val BLACK = -0x1000000
    const val WHITE = -0x1
    fun alpha(c: Int) = (c ushr 24) and 0xFF
    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF
    fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b
    fun rgb(r: Int, g: Int, b: Int) = argb(255, r, g, b)

    fun HSVToColor(hsv: FloatArray): Int {
        val h = ((hsv[0] % 360f) + 360f) % 360f / 60f
        val s = hsv[1]; val v = hsv[2]
        val i = h.toInt()
        val f = h - i
        val p = v * (1 - s); val q = v * (1 - s * f); val t = v * (1 - s * (1 - f))
        val (r, g, b) = when (i) {
            0 -> Triple(v, t, p); 1 -> Triple(q, v, p); 2 -> Triple(p, v, t)
            3 -> Triple(p, q, v); 4 -> Triple(t, p, v); else -> Triple(v, p, q)
        }
        return rgb((r * 255).toInt(), (g * 255).toInt(), (b * 255).toInt())
    }

    private val cache = HashMap<Int, String>()

    /** CSS-Farbe mit Zwischenspeicher (vermeidet ständig neue Texte). */
    fun css(c: Int): String {
        cache[c]?.let { return it }
        val a = alpha(c) / 255.0
        val s = "rgba(${red(c)},${green(c)},${blue(c)},${(a * 1000).toInt() / 1000.0})"
        if (cache.size < 4000) cache[c] = s
        return s
    }
}

class Rect(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0)

class RectF(var left: Float = 0f, var top: Float = 0f, var right: Float = 0f, var bottom: Float = 0f) {
    fun set(l: Float, t: Float, r: Float, b: Float) { left = l; top = t; right = r; bottom = b }
    fun width() = right - left
    fun height() = bottom - top
}

class Typeface(val family: String, val weight: String) {
    companion object {
        const val NORMAL = 0
        const val BOLD = 1
        val DEFAULT = Typeface("sans-serif", "500")
        val DEFAULT_BOLD = Typeface("sans-serif", "800")
        fun create(base: Typeface?, style: Int) = if (style == BOLD) DEFAULT_BOLD else DEFAULT
        fun createFromAsset(mgr: android.content.res.AssetManager, path: String): Typeface = when {
            path.contains("Outfit-ExtraBold") -> Typeface("OutfitXB", "800")
            path.contains("Outfit-Medium") -> Typeface("OutfitMd", "500")
            path.contains("Manrope-ExtraBold") -> Typeface("ManropeXB", "800")
            path.contains("Manrope-SemiBold") -> Typeface("ManropeSB", "600")
            else -> DEFAULT
        }
    }
}

open class Shader {
    enum class TileMode { CLAMP, REPEAT, MIRROR }
    private var lastCtx: CanvasRenderingContext2D? = null
    private var grad: CanvasGradient? = null
    internal fun gradientFor(ctx: CanvasRenderingContext2D): CanvasGradient {
        val g = grad
        if (g != null && lastCtx === ctx) return g
        val n = create(ctx)
        lastCtx = ctx; grad = n
        return n
    }
    internal open fun create(ctx: CanvasRenderingContext2D): CanvasGradient = ctx.createLinearGradient(0.0, 0.0, 1.0, 1.0)
}

class LinearGradient(
    private val x0: Float, private val y0: Float, private val x1: Float, private val y1: Float,
    private val c0: Int, private val c1: Int, tile: TileMode
) : Shader() {
    override fun create(ctx: CanvasRenderingContext2D): CanvasGradient {
        val g = ctx.createLinearGradient(x0.toDouble(), y0.toDouble(), x1.toDouble(), y1.toDouble())
        g.addColorStop(0.0, Color.css(c0)); g.addColorStop(1.0, Color.css(c1))
        return g
    }
}

class RadialGradient(
    private val cx: Float, private val cy: Float, private val r: Float,
    private val colors: IntArray, private val stops: FloatArray?, tile: TileMode
) : Shader() {
    override fun create(ctx: CanvasRenderingContext2D): CanvasGradient {
        val g = ctx.createRadialGradient(cx.toDouble(), cy.toDouble(), 0.0, cx.toDouble(), cy.toDouble(), r.toDouble())
        for (i in colors.indices) {
            val st = stops?.get(i)?.toDouble() ?: (i.toDouble() / max(1, colors.size - 1))
            g.addColorStop(st, Color.css(colors[i]))
        }
        return g
    }
}

open class Paint(flags: Int = 0) {
    companion object {
        const val ANTI_ALIAS_FLAG = 1
        const val FILTER_BITMAP_FLAG = 2
    }
    enum class Style { FILL, STROKE, FILL_AND_STROKE }
    enum class Cap { BUTT, ROUND, SQUARE }
    enum class Join { MITER, ROUND, BEVEL }
    enum class Align { LEFT, CENTER, RIGHT }

    var style = Style.FILL
    var color: Int = Color.BLACK
    var alpha: Int
        get() = (color ushr 24) and 0xFF
        set(v) { color = (color and 0xFFFFFF) or (v.coerceIn(0, 255) shl 24) }
    var strokeWidth = 0f
    var strokeCap = Cap.BUTT
    var strokeJoin = Join.MITER
    var textSize = 12f
    var textAlign = Align.LEFT
    var typeface: Typeface? = null
    var letterSpacing = 0f
    var shader: Shader? = null

    internal fun font(): String {
        val tf = typeface ?: Typeface.DEFAULT
        return "${tf.weight} ${textSize}px ${tf.family}, sans-serif"
    }

    fun measureText(s: String): Float {
        val m = Canvas.measureCtx
        m.font = font()
        return m.measureText(s).width.toFloat() + letterSpacing * textSize * s.length
    }
}

class Path {
    enum class Direction { CW, CCW }
    internal var p2d = Path2D()
    var isEmpty = true; private set

    fun moveTo(x: Float, y: Float) { p2d.moveTo(x.toDouble(), y.toDouble()); isEmpty = false }
    fun lineTo(x: Float, y: Float) { p2d.lineTo(x.toDouble(), y.toDouble()); isEmpty = false }
    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) {
        p2d.quadraticCurveTo(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble()); isEmpty = false
    }
    fun cubicTo(x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float) {
        p2d.bezierCurveTo(x1.toDouble(), y1.toDouble(), x2.toDouble(), y2.toDouble(), x3.toDouble(), y3.toDouble()); isEmpty = false
    }
    fun close() = p2d.closePath()
    fun addCircle(x: Float, y: Float, r: Float, dir: Direction) {
        p2d.moveTo((x + r).toDouble(), y.toDouble())
        p2d.arc(x.toDouble(), y.toDouble(), r.toDouble(), 0.0, 2 * PI, dir == Direction.CCW)
        isEmpty = false
    }
}

class Bitmap internal constructor(val element: HTMLCanvasElement) {
    enum class Config { ARGB_8888, RGB_565 }
    val width get() = element.width
    val height get() = element.height
    companion object {
        fun createBitmap(w: Int, h: Int, cfg: Config): Bitmap {
            val c = document.createElement("canvas") as HTMLCanvasElement
            c.width = w; c.height = h
            return Bitmap(c)
        }
    }
}

class Canvas(val ctx: CanvasRenderingContext2D) {

    constructor(b: Bitmap) : this(b.element.getContext("2d") as CanvasRenderingContext2D)

    companion object {
        internal val measureCtx: CanvasRenderingContext2D by lazy {
            (document.createElement("canvas") as HTMLCanvasElement).getContext("2d") as CanvasRenderingContext2D
        }
        private const val DEG = PI / 180.0
    }

    fun save(): Int { ctx.save(); return 0 }
    fun restore() = ctx.restore()
    fun translate(dx: Float, dy: Float) = ctx.translate(dx.toDouble(), dy.toDouble())
    fun scale(sx: Float, sy: Float) = ctx.scale(sx.toDouble(), sy.toDouble())
    fun rotate(deg: Float) = ctx.rotate(deg * DEG)
    fun rotate(deg: Float, px: Float, py: Float) {
        ctx.translate(px.toDouble(), py.toDouble()); ctx.rotate(deg * DEG); ctx.translate(-px.toDouble(), -py.toDouble())
    }

    fun clipRect(l: Float, t: Float, r: Float, b: Float): Boolean {
        ctx.beginPath()
        ctx.rect(l.toDouble(), t.toDouble(), (r - l).toDouble(), (b - t).toDouble())
        ctx.clip()
        return true
    }

    fun clipPath(p: Path): Boolean { ctx.clip(p.p2d); return true }

    fun drawColor(c: Int) {
        ctx.save()
        ctx.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        ctx.globalAlpha = 1.0
        ctx.fillStyle = Color.css(c)
        ctx.fillRect(0.0, 0.0, ctx.canvas.width.toDouble(), ctx.canvas.height.toDouble())
        ctx.restore()
    }

    private fun prepare(p: Paint, forFill: Boolean) {
        val sh = p.shader
        if (sh != null) {
            ctx.globalAlpha = p.alpha / 255.0
            val g = sh.gradientFor(ctx)
            if (forFill) ctx.fillStyle = g else ctx.strokeStyle = g
        } else {
            ctx.globalAlpha = 1.0
            if (forFill) ctx.fillStyle = Color.css(p.color) else ctx.strokeStyle = Color.css(p.color)
        }
        if (!forFill) {
            ctx.lineWidth = max(p.strokeWidth, 0.5f).toDouble()
            ctx.asDynamic().lineCap = when (p.strokeCap) { Paint.Cap.ROUND -> "round"; Paint.Cap.SQUARE -> "square"; else -> "butt" }
            ctx.asDynamic().lineJoin = when (p.strokeJoin) { Paint.Join.ROUND -> "round"; Paint.Join.BEVEL -> "bevel"; else -> "miter" }
        }
    }

    private fun finish(p: Paint) {
        if (p.style == Paint.Style.STROKE) { prepare(p, false); ctx.stroke() }
        else {
            prepare(p, true); ctx.fill()
            if (p.style == Paint.Style.FILL_AND_STROKE) { prepare(p, false); ctx.stroke() }
        }
    }

    fun drawRect(l: Float, t: Float, r: Float, b: Float, p: Paint) {
        if (p.style == Paint.Style.STROKE) {
            prepare(p, false)
            ctx.strokeRect(l.toDouble(), t.toDouble(), (r - l).toDouble(), (b - t).toDouble())
        } else {
            prepare(p, true)
            ctx.fillRect(l.toDouble(), t.toDouble(), (r - l).toDouble(), (b - t).toDouble())
        }
    }

    fun drawRect(r: RectF, p: Paint) = drawRect(r.left, r.top, r.right, r.bottom, p)

    fun drawRoundRect(r: RectF, rx: Float, ry: Float, p: Paint) {
        val w = r.right - r.left; val h = r.bottom - r.top
        if (w <= 0f || h <= 0f) return
        val rad = min(min(rx, ry), min(w, h) / 2f).toDouble()
        val l = r.left.toDouble(); val t = r.top.toDouble(); val rr = r.right.toDouble(); val bb = r.bottom.toDouble()
        ctx.beginPath()
        ctx.moveTo(l + rad, t)
        ctx.lineTo(rr - rad, t); ctx.arcTo(rr, t, rr, t + rad, rad)
        ctx.lineTo(rr, bb - rad); ctx.arcTo(rr, bb, rr - rad, bb, rad)
        ctx.lineTo(l + rad, bb); ctx.arcTo(l, bb, l, bb - rad, rad)
        ctx.lineTo(l, t + rad); ctx.arcTo(l, t, l + rad, t, rad)
        ctx.closePath()
        finish(p)
    }

    fun drawCircle(x: Float, y: Float, r: Float, p: Paint) {
        if (r <= 0f) return
        ctx.beginPath()
        ctx.arc(x.toDouble(), y.toDouble(), r.toDouble(), 0.0, 2 * PI)
        finish(p)
    }

    fun drawOval(r: RectF, p: Paint) {
        val rx = abs(r.right - r.left) / 2f; val ry = abs(r.bottom - r.top) / 2f
        if (rx <= 0f || ry <= 0f) return
        ctx.beginPath()
        ctx.ellipse(((r.left + r.right) / 2f).toDouble(), ((r.top + r.bottom) / 2f).toDouble(), rx.toDouble(), ry.toDouble(), 0.0, 0.0, 2 * PI)
        finish(p)
    }

    fun drawArc(r: RectF, startDeg: Float, sweepDeg: Float, useCenter: Boolean, p: Paint) {
        val rx = abs(r.right - r.left) / 2f; val ry = abs(r.bottom - r.top) / 2f
        if (rx <= 0f || ry <= 0f) return
        val cx = ((r.left + r.right) / 2f).toDouble(); val cy = ((r.top + r.bottom) / 2f).toDouble()
        val a0 = startDeg * DEG; val a1 = (startDeg + sweepDeg) * DEG
        ctx.beginPath()
        if (useCenter) ctx.moveTo(cx, cy)
        ctx.ellipse(cx, cy, rx.toDouble(), ry.toDouble(), 0.0, a0, a1, sweepDeg < 0f)
        if (useCenter) ctx.closePath()
        finish(p)
    }

    fun drawLine(x1: Float, y1: Float, x2: Float, y2: Float, p: Paint) {
        prepare(p, false)
        ctx.beginPath()
        ctx.moveTo(x1.toDouble(), y1.toDouble())
        ctx.lineTo(x2.toDouble(), y2.toDouble())
        ctx.stroke()
    }

    fun drawPath(path: Path, p: Paint) {
        if (p.style == Paint.Style.STROKE) { prepare(p, false); ctx.stroke(path.p2d) }
        else { prepare(p, true); ctx.fill(path.p2d) }
    }

    fun drawPoints(pts: FloatArray, p: Paint) {
        prepare(p, true)
        val r = max(p.strokeWidth / 2f, 0.5f).toDouble()
        ctx.beginPath()
        var i = 0
        while (i + 1 < pts.size) {
            val x = pts[i].toDouble(); val y = pts[i + 1].toDouble()
            ctx.moveTo(x + r, y)
            ctx.arc(x, y, r, 0.0, 2 * PI)
            i += 2
        }
        ctx.fill()
    }

    fun drawText(s: String, x: Float, y: Float, p: Paint) {
        prepare(p, true)
        ctx.font = p.font()
        ctx.asDynamic().textAlign = when (p.textAlign) { Paint.Align.CENTER -> "center"; Paint.Align.RIGHT -> "right"; else -> "left" }
        ctx.asDynamic().textBaseline = "alphabetic"
        val ls = p.letterSpacing
        if (ls != 0f) ctx.asDynamic().letterSpacing = "${ls * p.textSize}px"
        ctx.fillText(s, x.toDouble(), y.toDouble())
        if (ls != 0f) ctx.asDynamic().letterSpacing = "0px"
    }

    fun drawBitmap(b: Bitmap, src: Rect?, dst: RectF, p: Paint?) {
        ctx.globalAlpha = (p?.alpha ?: 255) / 255.0
        ctx.drawImage(b.element, dst.left.toDouble(), dst.top.toDouble(), (dst.right - dst.left).toDouble(), (dst.bottom - dst.top).toDouble())
        ctx.globalAlpha = 1.0
    }
}
