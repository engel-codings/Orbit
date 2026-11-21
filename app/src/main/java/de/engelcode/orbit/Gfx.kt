package de.engelcode.orbit

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.sin

/**
 * Alle Farben, Pinsel und Zeichen-Hilfen. Wird einmal erstellt –
 * beim Zeichnen wird nichts Neues angelegt (wichtig für flüssige FPS).
 */
class Gfx {

    companion object {
        const val BG = 0xFF0B0E1A.toInt()
        const val PANEL = 0xFF161B2E.toInt()
        const val PANEL2 = 0xFF222A47.toInt()
        const val ACCENT = 0xFF4CC9F0.toInt()
        const val GOLD = 0xFFFFD166.toInt()
        const val GREEN = 0xFF2EE59D.toInt()
        const val RED = 0xFFFF4D6D.toInt()
        const val DIM = 0xFF8A93B2.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val ICE = 0xFFBDE8FF.toInt()

        /** Planetenfarben – Index 6 ist Eis. */
        val PALETTE = intArrayOf(
            0xFF2EC4B6.toInt(), 0xFFFF6B6B.toInt(), 0xFF9B5DE5.toInt(),
            0xFFF4A261.toInt(), 0xFF4CC9F0.toInt(), 0xFFF15BB5.toInt(), ICE
        )
        const val ICE_IDX = 6
    }

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val bmp = Paint(Paint.FILTER_BITMAP_FLAG)
    private val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val reg = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT }
    val rect = RectF()

    val starPath = Path().apply {
        moveTo(0f, -1f)
        quadTo(0.18f, -0.18f, 1f, 0f)
        quadTo(0.18f, 0.18f, 0f, 1f)
        quadTo(-0.18f, 0.18f, -1f, 0f)
        quadTo(-0.18f, -0.18f, 0f, -1f)
        close()
    }
    private val unitCircle = Path().apply { addCircle(0f, 0f, 1f, Path.Direction.CW) }

    val rainbow = IntArray(36) { Color.HSVToColor(floatArrayOf(it * 10f, 0.6f, 1f)) }

    val planetGlow = Array(PALETTE.size) { makeGlow(PALETTE[it], 0x80) }
    val starGlow = makeGlow(GOLD, 0x70)
    val holeGlow = makeGlow(0xFF7B2CBF.toInt(), 0xA0)
    val goalGlow = makeGlow(GREEN, 0x90)
    val skinGlow = Array(Skins.skins.size) { makeGlow(Skins.skins[it].glow, 0xA0) }
    private val bodyGlow = arrayOf(
        planetGlow[0], makeGlow(0xFF4C9BE8.toInt(), 0x90), makeGlow(0xFFC9CED6.toInt(), 0x70),
        makeGlow(0xFFE07A4F.toInt(), 0x80), makeGlow(0xFF9C8B78.toInt(), 0x60),
        makeGlow(0xFFE0B98A.toInt(), 0x80), makeGlow(0xFFEED9A6.toInt(), 0x80)
    )

    // ---------------------------------------------------------------- Grundformen

    fun glow(c: Canvas, b: Bitmap, x: Float, y: Float, r: Float) {
        rect.set(x - r, y - r, x + r, y + r)
        c.drawBitmap(b, null, rect, bmp)
    }

    fun circle(c: Canvas, x: Float, y: Float, r: Float, color: Int, alpha: Int = 255) {
        fill.color = color; fill.alpha = alpha
        c.drawCircle(x, y, r, fill)
    }

    fun ring(c: Canvas, x: Float, y: Float, r: Float, color: Int, width: Float, alpha: Int = 255) {
        stroke.color = color; stroke.alpha = alpha; stroke.strokeWidth = width
        c.drawCircle(x, y, r, stroke)
    }

    fun roundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, alpha: Int = 255) {
        fill.color = color; fill.alpha = alpha
        rect.set(l, t, r, b)
        c.drawRoundRect(rect, rad, rad, fill)
    }

    fun strokeRoundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, color: Int, width: Float, alpha: Int = 255) {
        stroke.color = color; stroke.alpha = alpha; stroke.strokeWidth = width
        rect.set(l, t, r, b)
        c.drawRoundRect(rect, rad, rad, stroke)
    }

    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        isBold: Boolean = true, align: Paint.Align = Paint.Align.CENTER, alpha: Int = 255
    ) {
        val p = if (isBold) bold else reg
        p.textSize = size; p.color = color; p.alpha = alpha; p.textAlign = align
        c.drawText(s, x, y, p)
    }

    fun textWidth(s: String, size: Float, isBold: Boolean = true): Float {
        val p = if (isBold) bold else reg
        p.textSize = size
        return p.measureText(s)
    }

    fun starShape(c: Canvas, x: Float, y: Float, size: Float, rotDeg: Float, color: Int, alpha: Int = 255) {
        c.save()
        c.translate(x, y)
        c.rotate(rotDeg)
        c.scale(size, size)
        fill.color = color; fill.alpha = alpha
        c.drawPath(starPath, fill)
        c.restore()
    }

    fun lerpColor(a: Int, b: Int, f: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
        return Color.rgb(r, g, bl)
    }

    /** Leichter 3D-Look: Schatten unten rechts + Glanzpunkt. */
    private fun shade(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        circle(c, x + r * 0.18f, y + r * 0.18f, r * 0.82f, Color.BLACK, 50)
        circle(c, x - r * 0.06f, y - r * 0.06f, r * 0.80f, col)
    }

    private fun highlight(c: Canvas, x: Float, y: Float, r: Float) {
        circle(c, x - r * 0.35f, y - r * 0.35f, r * 0.22f, Color.WHITE, 60)
    }

    // ---------------------------------------------------------------- Planeten

    /** Normaler Planet (Typ 0), Eisplanet (1) oder Gasriese (2). */
    fun drawPlanet(c: Canvas, type: Int, colorIdx: Int, x: Float, y: Float, r: Float, t: Float, reduced: Boolean) {
        val col = PALETTE[colorIdx]
        if (!reduced) glow(c, planetGlow[colorIdx], x, y, r * 2.3f)
        if (type == 2) ringBack(c, x, y, r, lighten(col))
        circle(c, x, y, r, col)
        shade(c, x, y, r, col)
        if (type == 1) {
            line.color = Color.WHITE; line.alpha = 110; line.strokeWidth = 4f
            c.drawLine(x - r * 0.5f, y - r * 0.2f, x + r * 0.1f, y + r * 0.45f, line)
            c.drawLine(x + r * 0.1f, y - r * 0.55f, x + r * 0.45f, y + r * 0.1f, line)
        }
        highlight(c, x, y, r)
        if (type == 2) ringFront(c, x, y, r, lighten(col))
    }

    private fun lighten(col: Int) = lerpColor(col, Color.WHITE, 0.45f)

    private fun ringBack(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        c.save(); c.translate(x, y); c.rotate(-18f)
        stroke.color = col; stroke.alpha = 170; stroke.strokeWidth = r * 0.12f
        rect.set(-r * 1.75f, -r * 0.45f, r * 1.75f, r * 0.45f)
        c.drawArc(rect, 180f, 180f, false, stroke)
        c.restore()
    }

    private fun ringFront(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        c.save(); c.translate(x, y); c.rotate(-18f)
        stroke.color = col; stroke.alpha = 220; stroke.strokeWidth = r * 0.12f
        rect.set(-r * 1.75f, -r * 0.45f, r * 1.75f, r * 0.45f)
        c.drawArc(rect, 0f, 180f, false, stroke)
        c.restore()
    }

    /** Echte Himmelskörper der Reise (Erde, Mond, Mars, ...). */
    fun drawBody(c: Canvas, body: Int, x: Float, y: Float, r: Float, t: Float, reduced: Boolean) {
        if (!reduced) glow(c, bodyGlow[body], x, y, r * 2.2f)
        when (body) {
            Levels.B_EARTH -> {
                ring(c, x, y, r * 1.06f, 0xFF8FD3FF.toInt(), r * 0.08f, 120)
                circle(c, x, y, r, 0xFF2B6CB0.toInt())
                val g = 0xFF3FA34D.toInt()
                circle(c, x - r * 0.32f, y - r * 0.18f, r * 0.36f, g)
                circle(c, x - r * 0.12f, y - r * 0.42f, r * 0.22f, g)
                circle(c, x + r * 0.30f, y + r * 0.30f, r * 0.30f, g)
                circle(c, x + r * 0.45f, y - r * 0.30f, r * 0.16f, g)
                circle(c, x - r * 0.2f, y + r * 0.5f, r * 0.2f, Color.WHITE, 130)
                circle(c, x + r * 0.1f, y - r * 0.05f, r * 0.14f, Color.WHITE, 110)
                circle(c, x + r * 0.2f, y + r * 0.2f, r * 0.85f, Color.BLACK, 35)
                highlight(c, x, y, r)
            }
            Levels.B_MOON -> {
                val col = 0xFFC9CED6.toInt()
                circle(c, x, y, r, col)
                shade(c, x, y, r, col)
                val cr = 0xFF9AA0AA.toInt()
                circle(c, x - r * 0.3f, y - r * 0.25f, r * 0.2f, cr)
                circle(c, x + r * 0.3f, y + r * 0.15f, r * 0.26f, cr)
                circle(c, x - r * 0.1f, y + r * 0.45f, r * 0.12f, cr)
                circle(c, x + r * 0.35f, y - r * 0.4f, r * 0.1f, cr)
                highlight(c, x, y, r)
            }
            Levels.B_MARS -> {
                val col = 0xFFD9653B.toInt()
                circle(c, x, y, r, col)
                shade(c, x, y, r, col)
                val dk = 0xFFA8432A.toInt()
                circle(c, x - r * 0.25f, y + r * 0.1f, r * 0.3f, dk, 160)
                circle(c, x + r * 0.35f, y - r * 0.1f, r * 0.2f, dk, 160)
                circle(c, x, y - r * 0.82f, r * 0.17f, Color.WHITE, 220)
                highlight(c, x, y, r)
            }
            Levels.B_CERES -> {
                val col = 0xFF8D7B68.toInt()
                circle(c, x, y, r, col)
                shade(c, x, y, r, col)
                val cr = 0xFF6B5B4B.toInt()
                circle(c, x - r * 0.3f, y - r * 0.2f, r * 0.22f, cr)
                circle(c, x + r * 0.28f, y + r * 0.3f, r * 0.18f, cr)
                highlight(c, x, y, r)
                // Asteroiden drumherum
                for (k in 0 until 7) {
                    val a = t * 0.6f + k * 0.8976f
                    val d = r * (1.45f + 0.12f * sin(k * 2.1f))
                    circle(c, x + kotlin.math.cos(a) * d, y + sin(a) * d * 0.55f, r * (0.07f + 0.03f * (k % 3)), 0xFFA89A88.toInt())
                }
            }
            Levels.B_JUPITER, Levels.B_SATURN -> {
                val jup = body == Levels.B_JUPITER
                val base = if (jup) 0xFFD8B48A.toInt() else 0xFFE3C98F.toInt()
                val b1 = if (jup) 0xFFB5835A.toInt() else 0xFFC9A96E.toInt()
                val b2 = if (jup) 0xFFEFDCBB.toInt() else 0xFFF2E2B8.toInt()
                if (!jup) ringBack(c, x, y, r, 0xFFF5E6C4.toInt())
                c.save()
                c.translate(x, y); c.scale(r, r)
                c.clipPath(unitCircle)
                fill.color = base; fill.alpha = 255
                c.drawRect(-1f, -1f, 1f, 1f, fill)
                fill.color = b1
                c.drawRect(-1f, -0.55f, 1f, -0.38f, fill)
                c.drawRect(-1f, 0.05f, 1f, 0.22f, fill)
                c.drawRect(-1f, 0.5f, 1f, 0.62f, fill)
                fill.color = b2
                c.drawRect(-1f, -0.2f, 1f, -0.08f, fill)
                c.drawRect(-1f, 0.32f, 1f, 0.42f, fill)
                if (jup) {
                    fill.color = 0xFFC1440E.toInt(); fill.alpha = 220
                    rect.set(0.15f, 0.12f, 0.55f, 0.32f)
                    c.drawOval(rect, fill)
                }
                fill.color = Color.BLACK; fill.alpha = 45
                c.drawCircle(0.22f, 0.22f, 0.9f, fill)
                c.restore()
                highlight(c, x, y, r)
                if (!jup) ringFront(c, x, y, r, 0xFFF5E6C4.toInt())
            }
            else -> drawPlanet(c, 0, 0, x, y, r, t, reduced)
        }
    }

    // ---------------------------------------------------------------- Spieler-Skins

    fun skinColor(idx: Int, t: Float): Int {
        val sk = Skins.skins[idx]
        return if (sk.style == Skins.RAINBOW) rainbow[(((t * 14f).toInt() % 36) + 36) % 36] else sk.color
    }

    fun drawSkin(c: Canvas, idx: Int, x: Float, y: Float, s: Float, t: Float, reduced: Boolean) {
        val sk = Skins.skins[idx]
        val col = skinColor(idx, t)
        if (!reduced) glow(c, skinGlow[idx], x, y, 55f * s)
        when (sk.style) {
            Skins.PLAIN -> circle(c, x, y, 14f * s, col)
            Skins.PULSE -> {
                val r = 14f * s * (1f + 0.18f * sin(t * 6f))
                ring(c, x, y, r * 1.7f, col, 3f * s, 120)
                circle(c, x, y, r, col)
                circle(c, x, y, r * 0.45f, Color.WHITE, 200)
            }
            Skins.RING -> {
                circle(c, x, y, 13f * s, col)
                c.save(); c.translate(x, y); c.rotate(-25f)
                stroke.color = sk.color2; stroke.alpha = 255; stroke.strokeWidth = 3.5f * s
                rect.set(-27f * s, -9f * s, 27f * s, 9f * s)
                c.drawOval(rect, stroke)
                c.restore()
            }
            Skins.DIAMOND -> {
                c.save(); c.translate(x, y); c.rotate(t * 90f)
                fill.color = col; fill.alpha = 255
                c.drawRect(-12f * s, -12f * s, 12f * s, 12f * s, fill)
                fill.color = Color.WHITE; fill.alpha = 110
                c.drawRect(-5f * s, -5f * s, 5f * s, 5f * s, fill)
                c.restore()
            }
            Skins.STAR -> {
                starShape(c, x, y, 22f * s, t * 120f, col)
                circle(c, x, y, 5f * s, Color.WHITE, 230)
            }
            Skins.RAINBOW -> {
                circle(c, x, y, 14f * s, col)
                circle(c, x, y, 6f * s, Color.WHITE, 220)
            }
            Skins.HOLE -> {
                ring(c, x, y, 15f * s, sk.color, 4f * s)
                circle(c, x, y, 13f * s, Color.BLACK)
                stroke.color = 0xFFB388FF.toInt(); stroke.alpha = 200; stroke.strokeWidth = 3f * s
                rect.set(x - 24f * s, y - 24f * s, x + 24f * s, y + 24f * s)
                c.drawArc(rect, t * 220f, 80f, false, stroke)
                c.drawArc(rect, t * 220f + 180f, 80f, false, stroke)
            }
        }
    }

    // ---------------------------------------------------------------- Spuren

    fun drawTrail(
        c: Canvas, idx: Int, xs: FloatArray, ys: FloatArray, start: Int, count: Int, size: Int,
        skinCol: Int, t: Float
    ) {
        if (count < 2) return
        val tr = Skins.trails[idx]
        var px = xs[start % size]
        var py = ys[start % size]
        for (i in 1 until count) {
            val k = (start + i) % size
            val x = xs[k]; val y = ys[k]
            val f = i.toFloat() / count
            when (tr.style) {
                Skins.T_LINE -> seg(c, px, py, x, y, if (tr.color == 0) skinCol else tr.color, (f * 150f).toInt(), 3f + 13f * f)
                Skins.T_FIRE -> seg(c, px, py, x, y, lerpColor(0xFFFF3D00.toInt(), GOLD, f), (f * 200f).toInt(), 4f + 18f * f)
                Skins.T_SPARK -> if (i % 2 == 0) {
                    val j = sin(i * 12.99f + t * 20f) * 8f * (1f - f)
                    circle(c, x + j, y - j, 2f + 6f * f, if (i % 4 == 0) GOLD else Color.WHITE, (f * 230f).toInt())
                }
                Skins.T_RAINBOW -> seg(c, px, py, x, y, rainbow[(i * 3 + (t * 30f).toInt()) % 36], (f * 200f).toInt(), 4f + 14f * f)
                Skins.T_GOLD -> {
                    seg(c, px, py, x, y, GOLD, (f * 120f).toInt(), 6f + 20f * f)
                    seg(c, px, py, x, y, Color.WHITE, (f * 220f).toInt(), 2f + 4f * f)
                }
                Skins.T_GHOST -> {
                    seg(c, px, py, x, y, tr.color, (f * 60f).toInt(), 10f + 26f * f)
                    seg(c, px, py, x, y, tr.color, (f * 170f).toInt(), 2f + 3f * f)
                }
            }
            px = x; py = y
        }
    }

    private fun seg(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, alpha: Int, w: Float) {
        line.color = color; line.alpha = alpha; line.strokeWidth = w
        c.drawLine(x1, y1, x2, y2, line)
    }

    // ---------------------------------------------------------------- Hilfen

    private fun makeGlow(color: Int, alpha: Int): Bitmap {
        val size = 128
        val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        val r = Color.red(color); val g = Color.green(color); val bl = Color.blue(color)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = RadialGradient(
            size / 2f, size / 2f, size / 2f,
            intArrayOf(Color.argb(alpha, r, g, bl), Color.argb(alpha / 3, r, g, bl), Color.argb(0, r, g, bl)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        cv.drawCircle(size / 2f, size / 2f, size / 2f, p)
        return b
    }
}
