package de.engelcode.orbit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Alle Farben, Schriften, Icons und Zeichen-Hilfen. Wird einmal erstellt –
 * beim Zeichnen wird nichts Neues angelegt (wichtig für flüssige FPS).
 */
class Gfx(context: Context) {

    companion object {
        const val BG = 0xFF070A16.toInt()
        const val PANEL = 0xFF121833.toInt()
        const val PANEL2 = 0xFF1E2650.toInt()
        const val ACCENT = 0xFF4CC9F0.toInt()
        const val VIOLET = 0xFF7B61FF.toInt()
        const val GOLD = 0xFFFFD166.toInt()
        const val ORANGE = 0xFFFF9F1C.toInt()
        const val GREEN = 0xFF2EE59D.toInt()
        const val RED = 0xFFFF4D6D.toInt()
        const val DIM = 0xFF8A93B2.toInt()
        const val WHITE = 0xFFFFFFFF.toInt()
        const val ICE = 0xFFBDE8FF.toInt()
        const val SHIELD = 0xFF5CC8FF.toInt()

        val PALETTE = intArrayOf(
            0xFF2EC4B6.toInt(), 0xFFFF6B6B.toInt(), 0xFF9B5DE5.toInt(),
            0xFFF4A261.toInt(), 0xFF4CC9F0.toInt(), 0xFFF15BB5.toInt(), ICE
        )
        const val ICE_IDX = 6

        // Icons
        const val I_PLAY = 0
        const val I_SHOP = 1
        const val I_TROPHY = 2
        const val I_CHART = 3
        const val I_GEAR = 4
        const val I_CROWN = 5
        const val I_LOCK = 6
        const val I_CHECK = 7
        const val I_SHIELD = 8
        const val I_MAGNET = 9
        const val I_HEART = 10
        const val I_INFINITY = 11
        const val I_PLUS = 12
        const val I_SOUND = 13
        const val I_MUSIC = 14
        const val I_VIBRATE = 15
        const val I_SPEED = 16
        const val I_EYE = 17
        const val I_WHEEL = 18
        const val I_TARGET = 19
        const val I_SHAKE = 20
        const val I_GLOBE = 21
        const val I_CHEST = 22
        const val I_LIST = 23
    }

    private fun font(ctx: Context, file: String, fallback: Typeface): Typeface = try {
        Typeface.createFromAsset(ctx.assets, "fonts/$file")
    } catch (_: Exception) { fallback }

    // Outfit für Deutsch/Englisch, Manrope (mit kyrillischen Buchstaben) für Russisch
    private val outfitBold = font(context, "Outfit-ExtraBold.ttf", Typeface.DEFAULT_BOLD)
    private val outfitReg = font(context, "Outfit-Medium.ttf", Typeface.DEFAULT)
    private val manropeBold = font(context, "Manrope-ExtraBold.ttf", Typeface.DEFAULT_BOLD)
    private val manropeReg = font(context, "Manrope-SemiBold.ttf", Typeface.DEFAULT)

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val grad = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bmp = Paint(Paint.FILTER_BITMAP_FLAG)
    private val bold = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = outfitBold }
    private val reg = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = outfitReg }

    /** Schrift passend zur Sprache wählen. */
    fun applyLanguage(lang: Int) {
        val ru = lang == L.RU
        bold.typeface = if (ru) manropeBold else outfitBold
        reg.typeface = if (ru) manropeReg else outfitReg
    }
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
    private val heartPath = Path().apply {
        moveTo(0f, 0.85f)
        cubicTo(-1.25f, 0.0f, -0.9f, -1.05f, 0f, -0.45f)
        cubicTo(0.9f, -1.05f, 1.25f, 0.0f, 0f, 0.85f)
        close()
    }
    private val hexPath = Path().apply {
        for (k in 0 until 6) {
            val a = (k * 60.0 - 90.0) * Math.PI / 180.0
            val x = cos(a).toFloat(); val y = sin(a).toFloat()
            if (k == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    private val crownPath = Path().apply {
        moveTo(-1f, 0.6f); lineTo(-1f, -0.45f); lineTo(-0.5f, 0.05f); lineTo(0f, -0.75f)
        lineTo(0.5f, 0.05f); lineTo(1f, -0.45f); lineTo(1f, 0.6f); close()
    }
    private val shieldPath = Path().apply {
        moveTo(0f, -1f); lineTo(0.85f, -0.62f); lineTo(0.75f, 0.25f); quadTo(0.5f, 0.75f, 0f, 1f)
        quadTo(-0.5f, 0.75f, -0.75f, 0.25f); lineTo(-0.85f, -0.62f); close()
    }
    private val playPath = Path().apply { moveTo(-0.42f, -0.62f); lineTo(0.68f, 0f); lineTo(-0.42f, 0.62f); close() }

    private val nebulaCols = intArrayOf(0xFFF15BB5.toInt(), 0xFF7B61FF.toInt(), 0xFF4CC9F0.toInt(), 0xFFFF9F1C.toInt())
    val rainbow = IntArray(36) { Color.HSVToColor(floatArrayOf(it * 10f, 0.6f, 1f)) }

    val planetGlow = Array(PALETTE.size) { makeGlow(PALETTE[it], 0x80) }
    val starGlow = makeGlow(GOLD, 0x70)
    val holeGlow = makeGlow(0xFF7B2CBF.toInt(), 0xA0)
    val goalGlow = makeGlow(GREEN, 0x90)
    val shieldGlow = makeGlow(SHIELD, 0x90)
    val fireGlow = makeGlow(0xFFFF7A00.toInt(), 0x90)
    val whiteGlow = makeGlow(WHITE, 0x90)
    val skinGlow = Array(Skins.skins.size) { makeGlow(Skins.skins[it].glow, 0xA0) }
    private val bodyGlow = arrayOf(
        planetGlow[0], makeGlow(0xFF4C9BE8.toInt(), 0x90), makeGlow(0xFFC9CED6.toInt(), 0x70),
        makeGlow(0xFFE07A4F.toInt(), 0x80), makeGlow(0xFF9C8B78.toInt(), 0x60),
        makeGlow(0xFFE0B98A.toInt(), 0x80), makeGlow(0xFFEED9A6.toInt(), 0x80),
        makeGlow(0xFF9FE7E7.toInt(), 0x80), makeGlow(0xFF3F6BFF.toInt(), 0x90),
        makeGlow(0xFFD9C3A5.toInt(), 0x60), makeGlow(0xFF7FE7FF.toInt(), 0x90),
        makeGlow(0xFFB388FF.toInt(), 0xB0), makeGlow(0xFF7FE7FF.toInt(), 0xC0),
        makeGlow(0xFFF15BB5.toInt(), 0x90), makeGlow(0xFFFFD166.toInt(), 0xC0),
        makeGlow(0xFF9B5DE5.toInt(), 0xC0), makeGlow(0xFFFFFFFF.toInt(), 0xD0),
        makeGlow(0xFF00E5FF.toInt(), 0xB0), makeGlow(0xFFF15BB5.toInt(), 0xC0),
        makeGlow(0xFFE0F7FF.toInt(), 0x90), makeGlow(0xFF9BE564.toInt(), 0xB0),
        makeGlow(0xFF7B61FF.toInt(), 0xD0),
        makeGlow(0xFFB0BEC5.toInt(), 0x70), makeGlow(0xFF2EE5C8.toInt(), 0x90),
        makeGlow(0xFF7B2CBF.toInt(), 0x80), makeGlow(0xFF2EE5C8.toInt(), 0x70),
        makeGlow(0xFF9B5DE5.toInt(), 0xB0)
    )
    val nebula3 = arrayOf(
        makeGlow(0xFF1B4D4A.toInt(), 0x60), makeGlow(0xFF2A1B4D.toInt(), 0x60),
        makeGlow(0xFF2EE5C8.toInt(), 0x20), makeGlow(0xFF3D1B4D.toInt(), 0x55)
    )
    val nebula2 = arrayOf(
        makeGlow(0xFFF15BB5.toInt(), 0x50), makeGlow(0xFF00E5FF.toInt(), 0x45),
        makeGlow(0xFF9BE564.toInt(), 0x30), makeGlow(0xFF7B61FF.toInt(), 0x45)
    )
    val nebula = arrayOf(
        makeGlow(0xFF7B2CBF.toInt(), 0x55), makeGlow(0xFF1E5EFF.toInt(), 0x45),
        makeGlow(0xFF00B4A0.toInt(), 0x35), makeGlow(0xFFF15BB5.toInt(), 0x35)
    )

    // ================================================================ Grundformen

    fun glow(c: Canvas, b: Bitmap, x: Float, y: Float, r: Float) {
        rect.set(x - r, y - r, x + r, y + r)
        c.drawBitmap(b, null, rect, bmp)
    }

    fun glowA(c: Canvas, b: Bitmap, x: Float, y: Float, r: Float, alpha: Int) {
        bmp.alpha = alpha
        glow(c, b, x, y, r)
        bmp.alpha = 255
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

    /** Moderne Glas-Fläche. */
    fun glass(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, alpha: Int = 255, border: Int = WHITE) {
        roundRect(c, l, t, r, b, rad, PANEL, (215 * alpha) / 255)
        roundRect(c, l, t, r, t + (b - t) * 0.5f, rad, WHITE, (10 * alpha) / 255)
        strokeRoundRect(c, l, t, r, b, rad, border, 2.5f, (40 * alpha) / 255)
    }

    fun gradient(l: Float, t: Float, r: Float, b: Float, c1: Int, c2: Int): Shader =
        LinearGradient(l, t, r, b, c1, c2, Shader.TileMode.CLAMP)

    fun gradRoundRect(c: Canvas, l: Float, t: Float, r: Float, b: Float, rad: Float, shader: Shader, alpha: Int = 255) {
        grad.shader = shader; grad.alpha = alpha
        rect.set(l, t, r, b)
        c.drawRoundRect(rect, rad, rad, grad)
    }

    fun gradCircle(c: Canvas, x: Float, y: Float, r: Float, shader: Shader, alpha: Int = 255) {
        grad.shader = shader; grad.alpha = alpha
        c.drawCircle(x, y, r, grad)
    }

    /** Text zeichnen. maxW > 0: Schrift wird verkleinert, falls der Text zu breit ist. */
    fun text(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        isBold: Boolean = true, align: Paint.Align = Paint.Align.CENTER, alpha: Int = 255, maxW: Float = 0f
    ) {
        val p = if (isBold) bold else reg
        p.textSize = size
        if (maxW > 0f) {
            val w = p.measureText(s)
            if (w > maxW) p.textSize = size * maxW / w
        }
        p.color = color; p.alpha = alpha; p.textAlign = align
        c.drawText(s, x, y, p)
    }

    /** Text mit Zeichenabstand (für Überschriften). */
    fun spaced(c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int, spacing: Float, alpha: Int = 255) {
        bold.letterSpacing = spacing
        text(c, s, x, y, size, color, true, Paint.Align.CENTER, alpha)
        bold.letterSpacing = 0f
    }

    fun textWidth(s: String, size: Float, isBold: Boolean = true): Float {
        val p = if (isBold) bold else reg
        p.textSize = size
        return p.measureText(s)
    }

    fun starShape(c: Canvas, x: Float, y: Float, size: Float, rotDeg: Float, color: Int, alpha: Int = 255) {
        shape(c, starPath, x, y, size, rotDeg, color, alpha)
    }

    private fun shape(c: Canvas, p: Path, x: Float, y: Float, size: Float, rotDeg: Float, color: Int, alpha: Int = 255) {
        c.save()
        c.translate(x, y)
        if (rotDeg != 0f) c.rotate(rotDeg)
        c.scale(size, size)
        fill.color = color; fill.alpha = alpha
        c.drawPath(p, fill)
        c.restore()
    }

    fun lerpColor(a: Int, b: Int, f: Float): Int {
        val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt()
        val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt()
        val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt()
        return Color.rgb(r, g, bl)
    }

    /** Sterne-Anzeige als Pille: ★ 1234 */
    fun pill(c: Canvas, right: Float, cy: Float, label: String, icon: Int, iconColor: Int) {
        val tw = textWidth(label, 40f)
        val w = tw + 110f
        glass(c, right - w, cy - 42f, right, cy + 42f, 42f)
        if (icon < 0) starShape(c, right - w + 46f, cy, 20f, 0f, iconColor)
        else icon(c, icon, right - w + 46f, cy, 22f, iconColor)
        text(c, label, right - 32f, cy + 14f, 40f, WHITE, align = Paint.Align.RIGHT)
    }

    // ================================================================ Icons

    fun icon(c: Canvas, id: Int, x: Float, y: Float, s: Float, color: Int, alpha: Int = 255) {
        line.color = color; line.alpha = alpha; line.strokeWidth = s * 0.18f
        fill.color = color; fill.alpha = alpha
        when (id) {
            I_PLAY -> shape(c, playPath, x, y, s, 0f, color, alpha)
            I_SHOP -> {
                rect.set(x - s * 0.8f, y - s * 0.35f, x + s * 0.8f, y + s * 0.9f)
                c.drawRoundRect(rect, s * 0.18f, s * 0.18f, fill)
                rect.set(x - s * 0.42f, y - s * 0.95f, x + s * 0.42f, y + s * 0.1f)
                c.drawArc(rect, 180f, 180f, false, line)
            }
            I_TROPHY -> {
                rect.set(x - s * 0.62f, y - s * 1.05f, x + s * 0.62f, y + s * 0.25f)
                c.drawArc(rect, 0f, 180f, true, fill)
                c.drawRect(x - s * 0.62f, y - s * 0.9f, x + s * 0.62f, y - s * 0.4f, fill)
                c.drawRect(x - s * 0.12f, y + s * 0.2f, x + s * 0.12f, y + s * 0.6f, fill)
                rect.set(x - s * 0.5f, y + s * 0.6f, x + s * 0.5f, y + s * 0.85f)
                c.drawRoundRect(rect, s * 0.08f, s * 0.08f, fill)
                rect.set(x - s * 0.95f, y - s * 0.8f, x - s * 0.45f, y - s * 0.2f)
                c.drawArc(rect, 90f, 180f, false, line)
                rect.set(x + s * 0.45f, y - s * 0.8f, x + s * 0.95f, y - s * 0.2f)
                c.drawArc(rect, 270f, 180f, false, line)
            }
            I_CHART -> {
                val w = s * 0.38f
                rect.set(x - s * 0.85f, y - s * 0.1f, x - s * 0.85f + w, y + s * 0.85f); c.drawRoundRect(rect, 4f, 4f, fill)
                rect.set(x - w / 2f, y - s * 0.85f, x + w / 2f, y + s * 0.85f); c.drawRoundRect(rect, 4f, 4f, fill)
                rect.set(x + s * 0.85f - w, y - s * 0.45f, x + s * 0.85f, y + s * 0.85f); c.drawRoundRect(rect, 4f, 4f, fill)
            }
            I_GEAR -> {
                for (k in 0 until 8) {
                    c.save(); c.translate(x, y); c.rotate(k * 45f)
                    rect.set(-s * 0.2f, -s * 0.98f, s * 0.2f, -s * 0.5f)
                    c.drawRoundRect(rect, 3f, 3f, fill)
                    c.restore()
                }
                c.drawCircle(x, y, s * 0.68f, fill)
                circle(c, x, y, s * 0.28f, PANEL)
                fill.color = color; fill.alpha = alpha
            }
            I_CROWN -> shape(c, crownPath, x, y, s, 0f, color, alpha)
            I_LOCK -> {
                rect.set(x - s * 0.7f, y - s * 0.15f, x + s * 0.7f, y + s * 0.9f)
                c.drawRoundRect(rect, s * 0.15f, s * 0.15f, fill)
                rect.set(x - s * 0.45f, y - s * 0.9f, x + s * 0.45f, y + s * 0.1f)
                c.drawArc(rect, 180f, 180f, false, line)
                c.drawLine(x - s * 0.45f, y - s * 0.4f, x - s * 0.45f, y - s * 0.15f, line)
                c.drawLine(x + s * 0.45f, y - s * 0.4f, x + s * 0.45f, y - s * 0.15f, line)
            }
            I_CHECK -> {
                line.strokeWidth = s * 0.25f
                c.drawLine(x - s * 0.7f, y, x - s * 0.2f, y + s * 0.5f, line)
                c.drawLine(x - s * 0.2f, y + s * 0.5f, x + s * 0.75f, y - s * 0.55f, line)
            }
            I_SHIELD -> shape(c, shieldPath, x, y, s, 0f, color, alpha)
            I_MAGNET -> {
                line.strokeWidth = s * 0.42f; line.strokeCap = Paint.Cap.BUTT
                rect.set(x - s * 0.65f, y - s * 0.55f, x + s * 0.65f, y + s * 0.75f)
                c.drawArc(rect, 0f, 180f, false, line)
                c.drawLine(x - s * 0.65f, y + s * 0.1f, x - s * 0.65f, y - s * 0.55f, line)
                c.drawLine(x + s * 0.65f, y + s * 0.1f, x + s * 0.65f, y - s * 0.55f, line)
                line.color = WHITE; line.alpha = alpha
                c.drawLine(x - s * 0.65f, y - s * 0.55f, x - s * 0.65f, y - s * 0.9f, line)
                c.drawLine(x + s * 0.65f, y - s * 0.55f, x + s * 0.65f, y - s * 0.9f, line)
                line.strokeCap = Paint.Cap.ROUND
            }
            I_HEART -> shape(c, heartPath, x, y, s, 0f, color, alpha)
            I_INFINITY -> {
                line.strokeWidth = s * 0.2f
                rect.set(x - s * 0.95f, y - s * 0.45f, x, y + s * 0.45f); c.drawOval(rect, line)
                rect.set(x, y - s * 0.45f, x + s * 0.95f, y + s * 0.45f); c.drawOval(rect, line)
            }
            I_PLUS -> {
                line.strokeWidth = s * 0.3f
                c.drawLine(x - s * 0.6f, y, x + s * 0.6f, y, line)
                c.drawLine(x, y - s * 0.6f, x, y + s * 0.6f, line)
            }
            I_SOUND -> {
                c.drawRect(x - s * 0.8f, y - s * 0.3f, x - s * 0.4f, y + s * 0.3f, fill)
                line.strokeWidth = s * 0.16f
                c.drawLine(x - s * 0.4f, y - s * 0.3f, x + s * 0.05f, y - s * 0.75f, line)
                c.drawLine(x + s * 0.05f, y - s * 0.75f, x + s * 0.05f, y + s * 0.75f, line)
                c.drawLine(x + s * 0.05f, y + s * 0.75f, x - s * 0.4f, y + s * 0.3f, line)
                rect.set(x - s * 0.1f, y - s * 0.55f, x + s * 0.65f, y + s * 0.55f)
                c.drawArc(rect, -45f, 90f, false, line)
                rect.set(x - s * 0.1f, y - s * 0.9f, x + s * 0.95f, y + s * 0.9f)
                c.drawArc(rect, -45f, 90f, false, line)
            }
            I_MUSIC -> {
                c.drawCircle(x - s * 0.45f, y + s * 0.55f, s * 0.3f, fill)
                c.drawCircle(x + s * 0.55f, y + s * 0.35f, s * 0.3f, fill)
                line.strokeWidth = s * 0.16f
                c.drawLine(x - s * 0.18f, y + s * 0.55f, x - s * 0.18f, y - s * 0.7f, line)
                c.drawLine(x + s * 0.82f, y + s * 0.35f, x + s * 0.82f, y - s * 0.9f, line)
                line.strokeWidth = s * 0.28f
                c.drawLine(x - s * 0.18f, y - s * 0.7f, x + s * 0.82f, y - s * 0.9f, line)
            }
            I_VIBRATE -> {
                rect.set(x - s * 0.35f, y - s * 0.75f, x + s * 0.35f, y + s * 0.75f)
                c.drawRoundRect(rect, s * 0.12f, s * 0.12f, fill)
                line.strokeWidth = s * 0.14f
                c.drawLine(x - s * 0.62f, y - s * 0.4f, x - s * 0.62f, y + s * 0.4f, line)
                c.drawLine(x + s * 0.62f, y - s * 0.4f, x + s * 0.62f, y + s * 0.4f, line)
                c.drawLine(x - s * 0.88f, y - s * 0.22f, x - s * 0.88f, y + s * 0.22f, line)
                c.drawLine(x + s * 0.88f, y - s * 0.22f, x + s * 0.88f, y + s * 0.22f, line)
            }
            I_SPEED -> {
                line.strokeWidth = s * 0.16f
                rect.set(x - s * 0.9f, y - s * 0.8f, x + s * 0.9f, y + s * 1.0f)
                c.drawArc(rect, 180f, 180f, false, line)
                c.drawLine(x, y + s * 0.1f, x + s * 0.45f, y - s * 0.45f, line)
                c.drawCircle(x, y + s * 0.1f, s * 0.14f, fill)
            }
            I_WHEEL -> {
                line.strokeWidth = s * 0.16f
                c.drawCircle(x, y, s * 0.85f, line.apply { style = Paint.Style.STROKE })
                for (k in 0 until 4) {
                    val a = k * 0.785f
                    c.drawLine(x - cos(a) * s * 0.85f, y - sin(a) * s * 0.85f, x + cos(a) * s * 0.85f, y + sin(a) * s * 0.85f, line)
                }
                c.drawCircle(x, y, s * 0.25f, fill)
            }
            I_TARGET -> {
                line.strokeWidth = s * 0.16f
                c.drawCircle(x, y, s * 0.85f, line)
                c.drawCircle(x, y, s * 0.5f, line)
                c.drawCircle(x, y, s * 0.18f, fill)
            }
            I_SHAKE -> {
                line.strokeWidth = s * 0.14f
                rect.set(x - s * 0.5f, y - s * 0.7f, x + s * 0.5f, y + s * 0.7f)
                c.save(); c.rotate(-12f, x, y); c.drawRoundRect(rect, s * 0.12f, s * 0.12f, line); c.restore()
                c.drawLine(x - s * 0.9f, y - s * 0.5f, x - s * 0.75f, y - s * 0.2f, line)
                c.drawLine(x + s * 0.9f, y + s * 0.5f, x + s * 0.75f, y + s * 0.2f, line)
            }
            I_CHEST -> {
                rect.set(x - s * 0.9f, y - s * 0.1f, x + s * 0.9f, y + s * 0.8f)
                c.drawRoundRect(rect, s * 0.12f, s * 0.12f, fill)
                rect.set(x - s * 0.9f, y - s * 0.75f, x + s * 0.9f, y + s * 0.2f)
                c.drawArc(rect, 180f, 180f, true, fill)
                fill.color = PANEL; fill.alpha = alpha
                c.drawRect(x - s * 0.9f, y - s * 0.12f, x + s * 0.9f, y + s * 0.02f, fill)
                c.drawRect(x - s * 0.15f, y - s * 0.25f, x + s * 0.15f, y + s * 0.2f, fill)
                fill.color = color
            }
            I_LIST -> {
                line.strokeWidth = s * 0.18f
                for (k in -1..1) {
                    c.drawCircle(x - s * 0.7f, y + k * s * 0.6f, s * 0.13f, fill)
                    c.drawLine(x - s * 0.35f, y + k * s * 0.6f, x + s * 0.85f, y + k * s * 0.6f, line)
                }
            }
            I_GLOBE -> {
                line.strokeWidth = s * 0.14f
                c.drawCircle(x, y, s * 0.85f, line)
                rect.set(x - s * 0.38f, y - s * 0.85f, x + s * 0.38f, y + s * 0.85f)
                c.drawOval(rect, line)
                c.drawLine(x - s * 0.85f, y, x + s * 0.85f, y, line)
            }
            I_EYE -> {
                line.strokeWidth = s * 0.14f
                rect.set(x - s * 0.95f, y - s * 0.55f, x + s * 0.95f, y + s * 0.55f)
                c.drawOval(rect, line)
                c.drawCircle(x, y, s * 0.3f, fill)
            }
        }
    }

    // ================================================================ Planeten

    private fun shade(c: Canvas, x: Float, y: Float, r: Float, col: Int) {
        circle(c, x + r * 0.18f, y + r * 0.18f, r * 0.82f, Color.BLACK, 50)
        circle(c, x - r * 0.06f, y - r * 0.06f, r * 0.80f, col)
    }

    private fun highlight(c: Canvas, x: Float, y: Float, r: Float) {
        circle(c, x - r * 0.35f, y - r * 0.35f, r * 0.22f, Color.WHITE, 60)
    }

    fun drawPlanet(c: Canvas, type: Int, colorIdx: Int, x: Float, y: Float, r: Float, reduced: Boolean) {
        val col = PALETTE[colorIdx]
        if (!reduced) glow(c, planetGlow[colorIdx], x, y, r * 2.3f)
        if (type == 2) ringBack(c, x, y, r, lighten(col), -18f)
        circle(c, x, y, r, col)
        shade(c, x, y, r, col)
        if (type == 1) {
            line.color = Color.WHITE; line.alpha = 110; line.strokeWidth = 4f
            c.drawLine(x - r * 0.5f, y - r * 0.2f, x + r * 0.1f, y + r * 0.45f, line)
            c.drawLine(x + r * 0.1f, y - r * 0.55f, x + r * 0.45f, y + r * 0.1f, line)
        }
        highlight(c, x, y, r)
        if (type == 2) ringFront(c, x, y, r, lighten(col), -18f)
    }

    private fun lighten(col: Int) = lerpColor(col, Color.WHITE, 0.45f)

    private fun ringBack(c: Canvas, x: Float, y: Float, r: Float, col: Int, rot: Float) {
        c.save(); c.translate(x, y); c.rotate(rot)
        stroke.color = col; stroke.alpha = 170; stroke.strokeWidth = r * 0.12f
        rect.set(-r * 1.75f, -r * 0.45f, r * 1.75f, r * 0.45f)
        c.drawArc(rect, 180f, 180f, false, stroke)
        c.restore()
    }

    private fun ringFront(c: Canvas, x: Float, y: Float, r: Float, col: Int, rot: Float) {
        c.save(); c.translate(x, y); c.rotate(rot)
        stroke.color = col; stroke.alpha = 220; stroke.strokeWidth = r * 0.12f
        rect.set(-r * 1.75f, -r * 0.45f, r * 1.75f, r * 0.45f)
        c.drawArc(rect, 0f, 180f, false, stroke)
        c.restore()
    }

    private fun banded(c: Canvas, x: Float, y: Float, r: Float, base: Int, b1: Int, b2: Int, spot: Int) {
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
        if (spot != 0) {
            fill.color = spot; fill.alpha = 220
            rect.set(0.15f, 0.12f, 0.55f, 0.32f)
            c.drawOval(rect, fill)
        }
        fill.color = Color.BLACK; fill.alpha = 45
        c.drawCircle(0.22f, 0.22f, 0.9f, fill)
        c.restore()
        highlight(c, x, y, r)
    }

    /** Echte Himmelskörper der Reise. */
    fun drawBody(c: Canvas, body: Int, x: Float, y: Float, r: Float, t: Float, reduced: Boolean) {
        if (!reduced && body != Levels.B_ANDROMEDA) glow(c, bodyGlow[body], x, y, r * 2.2f)
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
                circle(c, x, y, r, col); shade(c, x, y, r, col)
                val cr = 0xFF9AA0AA.toInt()
                circle(c, x - r * 0.3f, y - r * 0.25f, r * 0.2f, cr)
                circle(c, x + r * 0.3f, y + r * 0.15f, r * 0.26f, cr)
                circle(c, x - r * 0.1f, y + r * 0.45f, r * 0.12f, cr)
                circle(c, x + r * 0.35f, y - r * 0.4f, r * 0.1f, cr)
                highlight(c, x, y, r)
            }
            Levels.B_MARS -> {
                val col = 0xFFD9653B.toInt()
                circle(c, x, y, r, col); shade(c, x, y, r, col)
                val dk = 0xFFA8432A.toInt()
                circle(c, x - r * 0.25f, y + r * 0.1f, r * 0.3f, dk, 160)
                circle(c, x + r * 0.35f, y - r * 0.1f, r * 0.2f, dk, 160)
                circle(c, x, y - r * 0.82f, r * 0.17f, Color.WHITE, 220)
                highlight(c, x, y, r)
            }
            Levels.B_CERES -> {
                val col = 0xFF8D7B68.toInt()
                circle(c, x, y, r, col); shade(c, x, y, r, col)
                val cr = 0xFF6B5B4B.toInt()
                circle(c, x - r * 0.3f, y - r * 0.2f, r * 0.22f, cr)
                circle(c, x + r * 0.28f, y + r * 0.3f, r * 0.18f, cr)
                highlight(c, x, y, r)
                for (k in 0 until 7) {
                    val a = t * 0.6f + k * 0.8976f
                    val d = r * (1.45f + 0.12f * sin(k * 2.1f))
                    circle(c, x + cos(a) * d, y + sin(a) * d * 0.55f, r * (0.07f + 0.03f * (k % 3)), 0xFFA89A88.toInt())
                }
            }
            Levels.B_JUPITER -> banded(c, x, y, r, 0xFFD8B48A.toInt(), 0xFFB5835A.toInt(), 0xFFEFDCBB.toInt(), 0xFFC1440E.toInt())
            Levels.B_SATURN -> {
                ringBack(c, x, y, r, 0xFFF5E6C4.toInt(), -18f)
                banded(c, x, y, r, 0xFFE3C98F.toInt(), 0xFFC9A96E.toInt(), 0xFFF2E2B8.toInt(), 0)
                ringFront(c, x, y, r, 0xFFF5E6C4.toInt(), -18f)
            }
            Levels.B_URANUS -> {
                ringBack(c, x, y, r, 0xFFDFFFFF.toInt(), 80f)
                val col = 0xFF9FE7E7.toInt()
                circle(c, x, y, r, col); shade(c, x, y, r, col)
                circle(c, x, y - r * 0.2f, r * 0.55f, Color.WHITE, 40)
                highlight(c, x, y, r)
                ringFront(c, x, y, r, 0xFFDFFFFF.toInt(), 80f)
            }
            Levels.B_NEPTUNE -> {
                banded(c, x, y, r, 0xFF3F6BFF.toInt(), 0xFF2E50D0.toInt(), 0xFF6C8CFF.toInt(), 0xFF1B2F8A.toInt())
            }
            Levels.B_PLUTO -> {
                val col = 0xFFB89F84.toInt()
                circle(c, x, y, r, col); shade(c, x, y, r, col)
                val light = 0xFFF0E2CF.toInt()
                circle(c, x + r * 0.05f, y + r * 0.15f, r * 0.3f, light)
                circle(c, x + r * 0.35f, y + r * 0.15f, r * 0.3f, light)
                circle(c, x + r * 0.2f, y + r * 0.38f, r * 0.24f, light)
                highlight(c, x, y, r)
            }
            Levels.B_COMET -> {
                // Schweif nach rechts oben
                for (k in 6 downTo 1) {
                    val d = k * r * 0.45f
                    circle(c, x + d, y - d * 0.6f, r * (1f - k * 0.11f), 0xFF7FE7FF.toInt(), 30 + (6 - k) * 12)
                }
                val col = 0xFFD7F3FF.toInt()
                circle(c, x, y, r * 0.8f, col)
                shade(c, x, y, r * 0.8f, col)
                circle(c, x - r * 0.2f, y + r * 0.1f, r * 0.15f, 0xFF9CC6D8.toInt())
                highlight(c, x, y, r * 0.8f)
            }
            Levels.B_ANDROMEDA -> drawGalaxy(c, x, y, r, t, 0xFFB388FF.toInt(), 0xFFFFD6F0.toInt(), reduced)
            Levels.B_PULSAR -> drawPulsar(c, x, y, r, t, 0xFF7FE7FF.toInt())
            Levels.B_NEBULA -> {
                val cols = nebulaCols
                for (k in 0 until 9) {
                    val a = k * 0.7f + t * 0.15f
                    val d = r * (0.25f + 0.55f * ((k * 37) % 10) / 10f)
                    circle(c, x + cos(a) * d, y + sin(a) * d * 0.8f, r * (0.45f - k * 0.025f), cols[k % 4], 70)
                }
                for (k in 0 until 6) circle(c, x + cos(k * 1.9f) * r * 0.6f, y + sin(k * 2.3f) * r * 0.5f, r * 0.05f, Color.WHITE, 220)
                circle(c, x, y, r * 0.18f, Color.WHITE, 200)
            }
            Levels.B_QUASAR -> {
                // Jets nach oben und unten
                for (k in 1..6) {
                    val f = k / 6f
                    val w = r * (0.32f - f * 0.2f)
                    circle(c, x, y - r * f * 1.7f, w, 0xFFFFD166.toInt(), (200 - f * 160).toInt())
                    circle(c, x, y + r * f * 1.7f, w, 0xFFFFD166.toInt(), (200 - f * 160).toInt())
                }
                c.save(); c.translate(x, y); c.scale(1f, 0.32f)
                for (k in 0 until 3) ring(c, 0f, 0f, r * (0.6f + k * 0.18f), 0xFFFF9F1C.toInt(), r * 0.12f, 200 - k * 50)
                c.restore()
                circle(c, x, y, r * 0.3f, Color.WHITE)
            }
            Levels.B_BLACKHOLE -> {
                c.save(); c.translate(x, y); c.rotate(-15f); c.scale(1f, 0.35f)
                for (k in 0 until 4) ring(c, 0f, 0f, r * (0.75f + k * 0.17f), lerpColor(0xFFFFD166.toInt(), 0xFF9B5DE5.toInt(), k / 3f), r * 0.14f, 230 - k * 40)
                c.restore()
                circle(c, x, y, r * 0.55f, Color.BLACK)
                ring(c, x, y, r * 0.58f, 0xFFFFD166.toInt(), r * 0.05f, 220)
                c.save(); c.translate(x, y); c.rotate(-15f); c.scale(1f, 0.35f)
                stroke.color = 0xFFFFE8A3.toInt(); stroke.alpha = 240; stroke.strokeWidth = r * 0.14f
                rect.set(-r * 0.75f, -r * 0.75f, r * 0.75f, r * 0.75f)
                c.drawArc(rect, 0f, 180f, false, stroke)
                c.restore()
            }
            Levels.B_BIGBANG -> {
                for (k in 0 until 24) {
                    val a = k * 0.2618f + t * 0.3f
                    val len = r * (0.9f + 0.35f * sin(t * 3f + k))
                    line.color = rainbow[(k * 3) % 36]; line.alpha = 200; line.strokeWidth = r * 0.06f
                    c.drawLine(x + cos(a) * r * 0.3f, y + sin(a) * r * 0.3f, x + cos(a) * len, y + sin(a) * len, line)
                }
                circle(c, x, y, r * 0.42f, 0xFFFFD166.toInt())
                circle(c, x, y, r * 0.3f, Color.WHITE)
            }
            Levels.B_CRYSTAL -> {
                for (k in 0 until 6) {
                    val a = k * 1.047f + t * 0.3f
                    shape(c, hexPath, x + cos(a) * r * 0.55f, y + sin(a) * r * 0.55f, r * 0.42f, t * 20f + k * 10f, 0xFF7FE7FF.toInt(), 170)
                }
                shape(c, hexPath, x, y, r * 0.6f, -t * 25f, 0xFFE0F7FF.toInt())
                shape(c, hexPath, x, y, r * 0.3f, t * 40f, Color.WHITE)
            }
            Levels.B_NEONSTAR -> {
                for (k in 0 until 3) ring(c, x, y, r * (0.7f + k * 0.2f) + sin(t * 3f + k) * 6f, 0xFFF15BB5.toInt(), r * 0.05f, 200 - k * 50)
                starShape(c, x, y, r * 0.75f, t * 40f, 0xFFF15BB5.toInt())
                starShape(c, x, y, r * 0.45f, -t * 60f, Color.WHITE)
            }
            Levels.B_MIRROR -> {
                circle(c, x, y, r, 0xFFE0F7FF.toInt())
                c.save(); c.clipRect(x, y - r - 2f, x + r + 2f, y + r + 2f)
                circle(c, x, y, r, 0xFF2A2F5A.toInt())
                c.restore()
                circle(c, x - r * 0.4f, y - r * 0.3f, r * 0.2f, 0xFF9AA6C8.toInt())
                circle(c, x + r * 0.4f, y - r * 0.3f, r * 0.2f, 0xFFB8C3E6.toInt())
                line.color = Color.WHITE; line.alpha = 230; line.strokeWidth = r * 0.05f
                c.drawLine(x, y - r * 1.15f, x, y + r * 1.15f, line)
                ring(c, x, y, r, 0xFF9BE564.toInt(), r * 0.05f, 200)
            }
            Levels.B_VORTEX -> {
                drawGalaxy(c, x, y, r, t * 2f, 0xFF9BE564.toInt(), 0xFFE0FFE0.toInt(), true)
                line.color = Color.WHITE; line.alpha = 230; line.strokeWidth = r * 0.06f
                c.drawLine(x, y, x + cos(t * 2f) * r * 0.45f, y + sin(t * 2f) * r * 0.45f, line)
                c.drawLine(x, y, x + cos(t * 0.3f) * r * 0.3f, y + sin(t * 0.3f) * r * 0.3f, line)
            }
            Levels.B_GATE -> {
                circle(c, x, y, r * 0.8f, Color.BLACK)
                stroke.strokeWidth = r * 0.12f
                rect.set(x - r * 0.9f, y - r * 0.9f, x + r * 0.9f, y + r * 0.9f)
                for (k in 0 until 12) {
                    stroke.color = rainbow[(k * 3 + (t * 20f).toInt()) % 36]; stroke.alpha = 230
                    c.drawArc(rect, t * 90f + k * 30f, 22f, false, stroke)
                }
                drawGalaxy(c, x, y, r * 0.55f, -t * 3f, 0xFF7B61FF.toInt(), Color.WHITE, true)
            }
            Levels.B_SILENT -> {
                for (k in 0 until 3) {
                    val f = ((t * 0.3f + k / 3f) % 1f)
                    ring(c, x, y, r * (0.5f + f * 0.9f), 0xFFB0BEC5.toInt(), r * 0.03f, (180 * (1f - f)).toInt())
                }
                starShape(c, x, y, r * 0.7f, 45f, 0xFFB0BEC5.toInt(), 220)
                circle(c, x, y, r * 0.22f, Color.WHITE, 230)
            }
            Levels.B_VOIDEYE -> {
                circle(c, x, y, r, 0xFF0A0F1A.toInt())
                ring(c, x, y, r, 0xFF2EE5C8.toInt(), r * 0.05f, 200)
                val lx = cos(t * 0.7f) * r * 0.2f; val ly = sin(t * 0.9f) * r * 0.12f
                circle(c, x + lx, y + ly, r * 0.45f, 0xFF2EE5C8.toInt(), 220)
                circle(c, x + lx, y + ly, r * 0.22f, Color.BLACK)
                circle(c, x + lx - r * 0.1f, y + ly - r * 0.1f, r * 0.07f, Color.WHITE)
            }
            Levels.B_SHADOWMOON -> {
                val col = 0xFF2A1B4D.toInt()
                circle(c, x, y, r, col)
                ring(c, x - r * 0.04f, y - r * 0.04f, r * 0.97f, 0xFF2EE5C8.toInt(), r * 0.05f, 170)
                val cr = 0xFF3D2A66.toInt()
                circle(c, x - r * 0.3f, y - r * 0.25f, r * 0.2f, cr)
                circle(c, x + r * 0.3f, y + r * 0.15f, r * 0.26f, cr)
                circle(c, x - r * 0.1f, y + r * 0.45f, r * 0.12f, cr)
            }
            Levels.B_ECHO -> {
                for (k in 0 until 5) {
                    val f = ((t * 0.5f + k / 5f) % 1f)
                    ring(c, x, y, r * (0.2f + f * 1.1f), if (k % 2 == 0) 0xFF2EE5C8.toInt() else Color.WHITE, r * 0.04f, (220 * (1f - f)).toInt())
                }
                circle(c, x, y, r * 0.2f, Color.WHITE)
            }
            Levels.B_VOIDHEART -> {
                val p = 1f + 0.08f * sin(t * 4f)
                shape(c, heartPath, x, y, r * 1.05f * p, 0f, 0xFF2EE5C8.toInt(), 120)
                shape(c, heartPath, x, y, r * 0.95f * p, 0f, 0xFF2A1B4D.toInt())
                shape(c, heartPath, x, y, r * 0.45f * p, 0f, 0xFF9B5DE5.toInt(), 200)
            }
            else -> drawPlanet(c, 0, 0, x, y, r, reduced)
        }
    }

    fun drawPulsar(c: Canvas, x: Float, y: Float, r: Float, t: Float, col: Int) {
        c.save(); c.translate(x, y); c.rotate(t * 120f)
        for (side in 0..1) {
            for (k in 1..5) {
                val f = k / 5f
                circle(c, 0f, (if (side == 0) -1f else 1f) * r * f * 1.6f, r * (0.22f - f * 0.15f), col, (230 - f * 180).toInt())
            }
        }
        c.restore()
        ring(c, x, y, r * 0.55f, col, r * 0.06f, 160)
        circle(c, x, y, r * 0.38f, col)
        circle(c, x, y, r * 0.25f, Color.WHITE)
    }

    fun drawGalaxy(c: Canvas, x: Float, y: Float, r: Float, t: Float, col: Int, core: Int, reduced: Boolean) {
        if (!reduced) {
            glowA(c, bodyGlow[Levels.B_ANDROMEDA], x, y, r * 2.4f, 255)
        }
        c.save()
        c.translate(x, y)
        c.rotate(-25f)
        c.scale(1f, 0.55f)
        for (arm in 0 until 2) {
            for (k in 0 until 14) {
                val f = k / 14f
                val a = t * 0.4f + arm * 3.1416f + f * 4.2f
                val d = r * (0.15f + f * 1.05f)
                circle(c, cos(a) * d, sin(a) * d, r * (0.13f - f * 0.07f), lerpColor(core, col, f), (230 - f * 160).toInt())
            }
        }
        c.restore()
        circle(c, x, y, r * 0.28f, core)
        circle(c, x, y, r * 0.16f, Color.WHITE)
    }

    // ================================================================ Spieler-Skins

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
            Skins.HEART -> {
                val p = 1f + 0.12f * sin(t * 8f).let { it * it }
                shape(c, heartPath, x, y, 17f * s * p, 0f, col)
                circle(c, x - 6f * s, y - 5f * s, 3.5f * s, Color.WHITE, 160)
            }
            Skins.HEX -> {
                shape(c, hexPath, x, y, 16f * s, t * 60f, col)
                shape(c, hexPath, x, y, 8f * s, -t * 90f, Color.WHITE, 140)
            }
            Skins.ATOM -> {
                circle(c, x, y, 7f * s, col)
                stroke.color = sk.color2; stroke.strokeWidth = 2.2f * s
                for (k in 0 until 3) {
                    c.save(); c.translate(x, y); c.rotate(k * 60f + t * 30f)
                    stroke.alpha = 200
                    rect.set(-24f * s, -8f * s, 24f * s, 8f * s)
                    c.drawOval(rect, stroke)
                    val a = t * 5f + k * 2.1f
                    circle(c, cos(a) * 24f * s, sin(a) * 8f * s, 3.5f * s, Color.WHITE)
                    c.restore()
                }
            }
            Skins.SAUCER -> {
                val bob = sin(t * 4f) * 2f * s
                fill.color = sk.color2; fill.alpha = 150
                rect.set(x - 8f * s, y - 14f * s + bob, x + 8f * s, y + 2f * s + bob)
                c.drawOval(rect, fill)
                fill.color = col; fill.alpha = 255
                rect.set(x - 24f * s, y - 5f * s + bob, x + 24f * s, y + 7f * s + bob)
                c.drawOval(rect, fill)
                for (k in 0 until 4) {
                    val on = ((t * 6f).toInt() + k) % 4 == 0
                    circle(c, x - 15f * s + k * 10f * s, y + 1f * s + bob, 2.5f * s, if (on) GOLD else Color.WHITE, if (on) 255 else 120)
                }
            }
            Skins.TWIN -> {
                val a = t * 6f
                val d = 11f * s
                circle(c, x + cos(a) * d, y + sin(a) * d, 9f * s, col)
                circle(c, x - cos(a) * d, y - sin(a) * d, 9f * s, sk.color2)
            }
            Skins.EYE -> {
                circle(c, x, y, 16f * s, Color.WHITE)
                val lx = cos(t * 1.7f) * 4f * s; val ly = sin(t * 2.3f) * 3f * s
                circle(c, x + lx, y + ly, 9f * s, sk.color2)
                circle(c, x + lx, y + ly, 4.5f * s, Color.BLACK)
                circle(c, x + lx - 2.5f * s, y + ly - 2.5f * s, 1.8f * s, Color.WHITE)
                // Blinzeln
                val blink = (t % 4f) > 3.85f
                if (blink) circle(c, x, y, 16.5f * s, sk.glow)
            }
            Skins.GALAXY -> drawGalaxy(c, x, y, 20f * s, t * 3f, sk.color, sk.color2, true)
            Skins.MOON -> {
                circle(c, x, y, 15f * s, col)
                circle(c, x + 7f * s, y - 4f * s, 13f * s, PANEL)
            }
            Skins.FIREBALL -> {
                for (k in 0 until 5) {
                    val a = t * 9f + k * 1.26f
                    circle(c, x + cos(a) * 6f * s, y + sin(a) * 6f * s, (10f - k) * s, if (k % 2 == 0) col else sk.color2, 170)
                }
                circle(c, x, y, 8f * s, Color.WHITE, 230)
            }
            Skins.SNOWFLAKE -> {
                c.save(); c.translate(x, y); c.rotate(t * 40f)
                line.color = col; line.alpha = 255; line.strokeWidth = 3.2f * s
                for (k in 0 until 6) {
                    c.drawLine(0f, 0f, 0f, -18f * s, line)
                    c.drawLine(0f, -11f * s, -5f * s, -15f * s, line)
                    c.drawLine(0f, -11f * s, 5f * s, -15f * s, line)
                    c.rotate(60f)
                }
                c.restore()
                circle(c, x, y, 4f * s, Color.WHITE)
            }
            Skins.ROCKET -> {
                c.save(); c.translate(x, y); c.rotate(sin(t * 2f) * 25f)
                val fl = 1f + 0.3f * sin(t * 30f)
                fill.color = 0xFFFF9F1C.toInt(); fill.alpha = 230
                rect.set(-5f * s, 10f * s, 5f * s, (18f + 8f * fl) * s); c.drawOval(rect, fill)
                fill.color = sk.color2; fill.alpha = 255
                c.drawRect(-11f * s, 4f * s, -5f * s, 14f * s, fill)
                c.drawRect(5f * s, 4f * s, 11f * s, 14f * s, fill)
                fill.color = col
                rect.set(-7f * s, -20f * s, 7f * s, 14f * s); c.drawOval(rect, fill)
                circle(c, 0f, -4f * s, 3.5f * s, ACCENT)
                c.restore()
            }
            Skins.CUBE -> {
                val a = t * 1.3f
                val ca = cos(a); val sa = sin(a)
                val e = 11f * s
                line.color = col; line.alpha = 255; line.strokeWidth = 2.6f * s
                for (k in 0 until 4) {
                    val k2 = (k + 1) % 4
                    val ax = if (k == 0 || k == 3) -e else e; val az = if (k < 2) -e else e
                    val bx = if (k2 == 0 || k2 == 3) -e else e; val bz = if (k2 < 2) -e else e
                    val px1 = ax * ca - az * sa; val pz1 = ax * sa + az * ca
                    val px2 = bx * ca - bz * sa; val pz2 = bx * sa + bz * ca
                    val y1 = -e * 0.8f + pz1 * 0.35f; val y2 = -e * 0.8f + pz2 * 0.35f
                    val y3 = e * 0.8f + pz1 * 0.35f; val y4 = e * 0.8f + pz2 * 0.35f
                    c.drawLine(x + px1, y + y1, x + px2, y + y2, line)
                    c.drawLine(x + px1, y + y3, x + px2, y + y4, line)
                    c.drawLine(x + px1, y + y1, x + px1, y + y3, line)
                }
                circle(c, x, y, 4f * s, sk.color2)
            }
            Skins.SATELLITE -> {
                c.save(); c.translate(x, y); c.rotate(t * 30f)
                fill.color = sk.color2; fill.alpha = 255
                c.drawRect(-26f * s, -6f * s, -10f * s, 6f * s, fill)
                c.drawRect(10f * s, -6f * s, 26f * s, 6f * s, fill)
                fill.color = col
                c.drawRect(-9f * s, -9f * s, 9f * s, 9f * s, fill)
                line.color = col; line.alpha = 255; line.strokeWidth = 2f * s
                c.drawLine(0f, -9f * s, 0f, -17f * s, line)
                c.restore()
                circle(c, x, y, 3f * s, RED, if ((t * 3f).toInt() % 2 == 0) 255 else 80)
            }
            Skins.PULSAR -> drawPulsar(c, x, y, 18f * s, t * 2f, sk.color2)
            Skins.BIGBANG -> {
                for (k in 0 until 12) {
                    val a = k * 0.5236f + t * 2f
                    val len = (16f + 6f * sin(t * 8f + k)) * s
                    line.color = rainbow[(k * 3 + (t * 20f).toInt()) % 36]; line.alpha = 230; line.strokeWidth = 3f * s
                    c.drawLine(x, y, x + cos(a) * len, y + sin(a) * len, line)
                }
                circle(c, x, y, 7f * s, Color.WHITE)
            }
            Skins.PRISM -> {
                c.save(); c.translate(x, y); c.rotate(t * 70f)
                for (k in 0 until 3) {
                    c.save(); c.rotate(k * 120f)
                    fill.color = rainbow[(k * 12 + (t * 20f).toInt()) % 36]; fill.alpha = 220
                    c.drawRect(-4f * s, -20f * s, 4f * s, -4f * s, fill)
                    c.restore()
                }
                c.restore()
                shape(c, hexPath, x, y, 9f * s, -t * 90f, Color.WHITE)
            }
            Skins.MIRROR -> {
                circle(c, x, y, 15f * s, sk.color)
                c.save(); c.clipRect(x, y - 16f * s, x + 16f * s, y + 16f * s)
                circle(c, x, y, 15f * s, sk.color2)
                c.restore()
                ring(c, x, y, 15f * s, sk.glow, 2.5f * s)
                val o = sin(t * 3f) * 6f * s
                circle(c, x - 6f * s, y + o, 3.5f * s, sk.color2)
                circle(c, x + 6f * s, y - o, 3.5f * s, sk.color)
            }
            Skins.FLOWER -> {
                c.save(); c.translate(x, y); c.rotate(t * 40f)
                for (k in 0 until 6) {
                    fill.color = col; fill.alpha = 235
                    rect.set(-5f * s, -21f * s, 5f * s, -5f * s)
                    c.drawOval(rect, fill)
                    c.rotate(60f)
                }
                c.restore()
                circle(c, x, y, 6.5f * s, sk.color2)
            }
            Skins.DONUT -> {
                ring(c, x, y, 11f * s, 0xFFE0A96D.toInt(), 9f * s)
                ring(c, x, y, 11f * s, col, 6f * s)
                for (k in 0 until 6) {
                    val a = k * 1.047f + 0.4f
                    fill.color = rainbow[(k * 6) % 36]; fill.alpha = 255
                    c.save(); c.translate(x + cos(a) * 11f * s, y + sin(a) * 11f * s); c.rotate(k * 50f)
                    c.drawRect(-2.5f * s, -0.9f * s, 2.5f * s, 0.9f * s, fill)
                    c.restore()
                }
            }
            Skins.SHURIKEN -> {
                c.save(); c.translate(x, y); c.rotate(t * 400f)
                for (k in 0 until 4) {
                    shape(c, crownPath, 0f, -11f * s, 6f * s, 0f, col)
                    c.rotate(90f)
                }
                c.restore()
                circle(c, x, y, 4f * s, PANEL)
            }
            Skins.CLOCK -> {
                circle(c, x, y, 16f * s, col)
                ring(c, x, y, 16f * s, sk.color2, 2.5f * s)
                line.color = PANEL; line.alpha = 255; line.strokeWidth = 2.5f * s
                c.drawLine(x, y, x + cos(t * 3f) * 12f * s, y + sin(t * 3f) * 12f * s, line)
                c.drawLine(x, y, x + cos(t * 0.25f) * 8f * s, y + sin(t * 0.25f) * 8f * s, line)
                circle(c, x, y, 2.5f * s, RED)
            }
            Skins.BEE -> {
                val wing = 0.6f + 0.4f * sin(t * 40f)
                fill.color = Color.WHITE; fill.alpha = 160
                rect.set(x - 12f * s, y - 16f * s * wing - 4f * s, x - 1f * s, y - 4f * s); c.drawOval(rect, fill)
                rect.set(x + 1f * s, y - 16f * s * wing - 4f * s, x + 12f * s, y - 4f * s); c.drawOval(rect, fill)
                fill.color = col; fill.alpha = 255
                rect.set(x - 15f * s, y - 10f * s, x + 15f * s, y + 10f * s); c.drawOval(rect, fill)
                fill.color = Color.BLACK
                c.drawRect(x - 4f * s, y - 9f * s, x + 1f * s, y + 9f * s, fill)
                c.drawRect(x + 6f * s, y - 7f * s, x + 10f * s, y + 7f * s, fill)
            }
            Skins.GHOST -> {
                val bob = sin(t * 3f) * 3f * s
                fill.color = col; fill.alpha = 215
                rect.set(x - 13f * s, y - 16f * s + bob, x + 13f * s, y + 10f * s + bob)
                c.drawRoundRect(rect, 13f * s, 13f * s, fill)
                for (k in 0 until 3) circle(c, x - 8.5f * s + k * 8.5f * s, y + 10f * s + bob, 4.4f * s, col, 215)
                circle(c, x - 5f * s, y - 5f * s + bob, 3f * s, Color.BLACK)
                circle(c, x + 5f * s, y - 5f * s + bob, 3f * s, Color.BLACK)
            }
            Skins.ORB -> {
                circle(c, x, y, 17f * s, sk.color2, 90)
                drawGalaxy(c, x, y, 12f * s, t * 3f, col, Color.WHITE, true)
                ring(c, x, y, 17f * s, Color.WHITE, 2f * s, 160)
                circle(c, x - 6f * s, y - 7f * s, 4f * s, Color.WHITE, 150)
            }
            Skins.VOID -> {
                circle(c, x, y, 15f * s, 0xFF05070D.toInt())
                ring(c, x, y, 15f * s + sin(t * 4f) * 1.5f * s, sk.color2, 3f * s)
                for (k in 0 until 3) {
                    val a = t * 2f + k * 2.094f
                    circle(c, x + cos(a) * 21f * s, y + sin(a) * 21f * s, 2.5f * s, sk.color2, 200)
                }
            }
            Skins.CROWN -> {
                shape(c, crownPath, x, y, 18f * s, sin(t * 3f) * 8f, col)
                circle(c, x, y - 2f * s, 3.5f * s, RED)
                circle(c, x - 10f * s, y + 3f * s, 2.5f * s, ACCENT)
                circle(c, x + 10f * s, y + 3f * s, 2.5f * s, GREEN)
            }
        }
    }

    // ================================================================ Spuren

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
                Skins.T_ZIGZAG -> {
                    val dx = x - px; val dy = y - py
                    val len = sqrt(dx * dx + dy * dy) + 0.001f
                    val o = (if (i % 2 == 0) 1f else -1f) * 10f * (1f - f * 0.5f)
                    val mx = (px + x) / 2f - dy / len * o; val my = (py + y) / 2f + dx / len * o
                    seg(c, px, py, mx, my, tr.color, (f * 230f).toInt(), 2f + 4f * f)
                    seg(c, mx, my, x, y, tr.color, (f * 230f).toInt(), 2f + 4f * f)
                }
                Skins.T_BUBBLES -> if (i % 3 == 0) {
                    ring(c, x, y, 3f + 9f * (1f - f) + 3f, tr.color, 2.5f, (f * 220f).toInt())
                }
                Skins.T_DOUBLE -> {
                    val dx = x - px; val dy = y - py
                    val len = sqrt(dx * dx + dy * dy) + 0.001f
                    val ox = -dy / len * 9f * f; val oy = dx / len * 9f * f
                    seg(c, px + ox, py + oy, x + ox, y + oy, tr.color, (f * 200f).toInt(), 2f + 5f * f)
                    seg(c, px - ox, py - oy, x - ox, y - oy, ACCENT, (f * 200f).toInt(), 2f + 5f * f)
                }
                Skins.T_STARDUST -> if (i % 2 == 0) {
                    starShape(c, x, y, 4f + 9f * f, t * 200f + i * 30f, if (i % 4 == 0) GOLD else Color.WHITE, (f * 240f).toInt())
                }
                Skins.T_HEARTS -> if (i % 3 == 0) {
                    shape(c, heartPath, x, y, 4f + 9f * f, sin(t * 4f + i) * 15f, tr.color, (f * 230f).toInt())
                }
                Skins.T_COMET -> {
                    seg(c, px, py, x, y, tr.color, (f * f * 120f).toInt(), 6f + 34f * f)
                    seg(c, px, py, x, y, Color.WHITE, (f * 200f).toInt(), 2f + 6f * f)
                }
                Skins.T_PIXEL -> if (i % 2 == 0) {
                    val sz = 4f + 6f * f
                    fill.color = rainbow[(i * 5) % 36]; fill.alpha = (f * 240f).toInt()
                    c.drawRect(x - sz, y - sz, x + sz, y + sz, fill)
                }
                Skins.T_GALAXY -> if (i % 2 == 0) {
                    val a = t * 6f + i * 0.8f
                    val d = 14f * (1f - f) + 3f
                    circle(c, x + cos(a) * d, y + sin(a) * d, 2f + 5f * f, tr.color, (f * 230f).toInt())
                    circle(c, x - cos(a) * d, y - sin(a) * d, 1.5f + 3f * f, Color.WHITE, (f * 200f).toInt())
                }
                Skins.T_WAVES -> {
                    val dx = x - px; val dy = y - py
                    val len = sqrt(dx * dx + dy * dy) + 0.001f
                    val o = sin(i * 0.9f + t * 10f) * 10f * f
                    val ox = -dy / len * o; val oy = dx / len * o
                    seg(c, px + ox, py + oy, x + ox, y + oy, tr.color, (f * 220f).toInt(), 2f + 5f * f)
                    seg(c, px - ox, py - oy, x - ox, y - oy, Color.WHITE, (f * 150f).toInt(), 1.5f + 3f * f)
                }
                Skins.T_CONFETTI -> if (i % 2 == 0) {
                    c.save(); c.translate(x + sin(i * 7.3f) * 9f, y + cos(i * 5.1f) * 9f); c.rotate(t * 300f + i * 40f)
                    fill.color = rainbow[(i * 7) % 36]; fill.alpha = (f * 255f).toInt()
                    c.drawRect(-5f * f - 2f, -2f, 5f * f + 2f, 2f, fill)
                    c.restore()
                }
                Skins.T_SHADOW -> {
                    seg(c, px, py, x, y, 0xFF2A1B4D.toInt(), (f * 200f).toInt(), 10f + 22f * f)
                    seg(c, px, py, x, y, tr.color, (f * 160f).toInt(), 2f + 2f * f)
                }
                Skins.T_LASER -> {
                    seg(c, px, py, x, y, tr.color, (f * 110f).toInt(), 6f + 8f * f)
                    seg(c, px, py, x, y, Color.WHITE, (f * 255f).toInt(), 1.5f + 2f * f)
                }
                Skins.T_MATRIX -> if (i % 2 == 0) {
                    val sz = 3f + 5f * f
                    fill.color = tr.color; fill.alpha = (f * (if (((t * 10f).toInt() + i) % 3 == 0) 255f else 140f)).toInt()
                    c.drawRect(x - sz, y - sz, x + sz, y + sz, fill)
                }
            }
            px = x; py = y
        }
    }

    private fun seg(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, color: Int, alpha: Int, w: Float) {
        line.color = color; line.alpha = alpha; line.strokeWidth = w
        c.drawLine(x1, y1, x2, y2, line)
    }

    // ================================================================ Hilfen

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
