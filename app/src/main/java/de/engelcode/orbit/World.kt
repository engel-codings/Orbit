package de.engelcode.orbit

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Die Spielwelt: Planeten, Spieler, Sterne, Schwarze Löcher, Partikel, Kamera.
 * Weltbreite = 1000 Einheiten, egal welches Handy.
 */
class World(private val gfx: Gfx) {

    interface Listener {
        fun onLaunch()
        fun onCapture(perfect: Boolean, streak: Int)
        fun onStar()
        fun onDeath()
        fun onGoal()
        fun onPowerup(type: Int)
        fun onShieldBreak()
    }

    companion object {
        const val W = 1000f
        const val FLY_SPEED = 950f
        const val ORBIT_SPEED = 520f
        const val GRAVITY = 2.6e7f
        const val CAPTURE = 110f
        const val ORBIT_GAP = 50f
        const val HOLE_KILL = 46f
        const val MAX_PLANETS = 7
        const val MAX_STARS = 7
        const val MAX_HOLES = 3
        const val MAX_PARTICLES = 140
        const val MAX_POWER = 3
        const val MAX_METEORS = 4
        const val P_SHIELD = 0
        const val P_MAGNET = 1
        const val MAGNET_TIME = 10f
        const val TRAIL = 24

        const val T_NORMAL = 0
        const val T_ICE = 1
        const val T_GIANT = 2

        val BODY_COLOR = intArrayOf(
            0, 0xFF4C9BE8.toInt(), 0xFFC9CED6.toInt(), 0xFFE07A4F.toInt(),
            0xFF9C8B78.toInt(), 0xFFE0B98A.toInt(), 0xFFEED9A6.toInt()
        )
    }

    class Planet {
        var x = 0f; var y = 0f; var r = 0f
        var baseX = 0f; var amp = 0f; var speed = 0f; var phase = 0f
        var colorIdx = 0; var type = 0; var orbitMul = 1f
        var body = 0; var goal = false; var order = 0
        var active = false
    }

    private class Star { var x = 0f; var y = 0f; var spin = 0f; var active = false }
    private class Hole { var x = 0f; var y = 0f; var spin = 0f; var active = false }
    private class Power { var x = 0f; var y = 0f; var spin = 0f; var type = 0; var active = false }
    private class Meteor { var x = 0f; var y = 0f; var vx = 0f; var r = 0f; var spin = 0f; var active = false }
    private class Particle {
        var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
        var life = 0f; var maxLife = 1f; var size = 0f; var color = 0
    }

    var listener: Listener? = null
    var reduced = false
    var shakeOn = true

    var worldH = 1800f; private set
    var camY = 0f; private set
    private var shake = 0f
    var time = 0f; private set

    private val planets = Array(MAX_PLANETS) { Planet() }
    private val stars = Array(MAX_STARS) { Star() }
    private val holes = Array(MAX_HOLES) { Hole() }
    private val particles = Array(MAX_PARTICLES) { Particle() }
    private val powers = Array(MAX_POWER) { Power() }
    private val meteors = Array(MAX_METEORS) { Meteor() }

    var shield = false
    var magnetT = 0f; private set
    var powerupsTaken = 0; private set

    // Modus
    var level = 0; private set          // 0 = Endlos
    var target = 0; private set         // Level: Anzahl Planeten bis zum Ziel
    private var rng = Random(1)
    private var genCount = 0
    private var lastGenX = 0f
    private var lastGenY = 0f

    // Spieler
    private var plX = 0f; private var plY = 0f
    private var vx = 0f; private var vy = 0f
    private var current: Planet? = null
    private var lastPlanet: Planet? = null
    private var angle = 0f
    private var orbitR = 0f
    private var orbitTarget = 0f
    private var dir = 1f
    private var flyTime = 0f
    private val trailX = FloatArray(TRAIL)
    private val trailY = FloatArray(TRAIL)
    private var trailHead = 0
    private var trailCount = 0

    var dead = false; private set
    var won = false; private set
    var score = 0; private set
    var runStars = 0; private set
    var captures = 0; private set
    var perfects = 0; private set
    var streak = 0; private set
    var bestStreak = 0; private set

    val orbiting get() = current != null && !dead
    val progress get() = (current ?: lastPlanet)?.order ?: 0

    private var popT = 0f
    private var popX = 0f
    private var popY = 0f
    private var popStr = ""
    var dim = 1; private set

    private val layerPts = Array(3) { FloatArray(0) }
    private val layerDepth = floatArrayOf(0.04f, 0.12f, 0.25f)
    private val starPaints = Array(3) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            strokeCap = android.graphics.Paint.Cap.ROUND
            strokeWidth = 2.5f + it * 1.5f
            alpha = 60 + it * 60
        }
    }
    private val flagPath = Path().apply {
        moveTo(0f, 0f); lineTo(46f, 16f); lineTo(0f, 32f); close()
    }

    // ===================================================================== Aufbau

    fun resize(h: Float) {
        worldH = h
        val rnd = Random(7)
        for (l in 0 until 3) {
            val n = 40 + l * 10
            val arr = FloatArray(n * 2)
            for (i in 0 until n) {
                arr[i * 2] = rnd.nextFloat() * W
                arr[i * 2 + 1] = rnd.nextFloat() * h
            }
            layerPts[l] = arr
        }
    }

    /** level = 0 → Endlos-Modus, sonst Level 1..50. */
    fun reset(level: Int, seed: Long = 0L) {
        this.level = level
        dim = if (level > 0) Levels.dimension(level) else 1
        rng = when {
            level > 0 -> Random(Levels.seed(level))
            seed != 0L -> Random(seed)
            else -> Random(System.nanoTime())
        }
        target = if (level > 0) Levels.planetCount(level) else 0
        for (p in planets) p.active = false
        for (s in stars) s.active = false
        for (h in holes) h.active = false
        for (p in powers) p.active = false
        for (m in meteors) m.active = false
        for (p in particles) p.life = 0f
        shield = false; magnetT = 0f; powerupsTaken = 0
        score = 0; runStars = 0; captures = 0; perfects = 0; streak = 0; bestStreak = 0
        dead = false; won = false
        shake = 0f; popT = 0f

        val first = planets[0]
        setupPlanet(first, W / 2f, worldH * 0.70f, 100f, 0f, T_NORMAL)
        first.body = if (level > 0) Levels.startBody(level) else Levels.B_EARTH
        first.order = 0
        lastGenX = first.x
        lastGenY = first.y
        genCount = 1
        fillPlanets()

        current = first
        lastPlanet = null
        angle = -PI.toFloat() / 2f
        orbitTarget = first.r + ORBIT_GAP
        orbitR = orbitTarget
        dir = 1f
        plX = first.x + cos(angle) * orbitR
        plY = first.y + sin(angle) * orbitR
        trailCount = 0
        camY = first.y - worldH * 0.70f
    }

    private fun setupPlanet(p: Planet, x: Float, y: Float, r: Float, amp: Float, type: Int) {
        p.baseX = x; p.x = x; p.y = y; p.r = r
        p.amp = amp
        p.speed = 0.6f + rng.nextFloat() * 0.8f
        p.phase = rng.nextFloat() * 6.28f
        p.type = type
        p.colorIdx = if (type == T_ICE) Gfx.ICE_IDX else rng.nextInt(6)
        p.orbitMul = when (type) { T_ICE -> 1.45f; T_GIANT -> 0.8f; else -> 1f }
        p.body = 0
        p.goal = false
        p.active = true
    }

    private fun fillPlanets() {
        var guard = 0
        while (guard++ < MAX_PLANETS && spawnNext()) { }
    }

    private fun spawnNext(): Boolean {
        if (level > 0 && genCount > target) return false
        var p: Planet? = null
        for (q in planets) if (!q.active) { p = q; break }
        if (p == null) return false

        val isGoal = level > 0 && genCount == target
        val d = if (level > 0) Levels.difficulty(level) else min(1f, genCount / 40f)

        var type = T_NORMAL
        val typesOn = if (level > 0) level >= Levels.TYPES_FROM else genCount >= 6
        if (typesOn && !isGoal) {
            val rr = rng.nextFloat()
            if (rr < 0.14f) type = T_ICE else if (rr < 0.24f) type = T_GIANT
        }
        var r = (80f - 30f * d) + rng.nextFloat() * (35f - 5f * d)
        if (type == T_GIANT) r *= 1.35f
        val gap = 400f + 120f * d + rng.nextFloat() * 160f

        var amp = 0f
        val moveOn = if (level > 0) level >= Levels.MOVING_FROM && genCount >= 2 else genCount >= 8
        if (moveOn && !isGoal && type != T_GIANT && rng.nextFloat() < 0.25f + 0.45f * d) {
            amp = 60f + rng.nextFloat() * 120f * (0.5f + d)
        }
        var goalBody = 0
        if (isGoal) {
            goalBody = Levels.goalBody(level)
            r = if (goalBody != 0) 110f else 85f
        }
        val maxAmp = W / 2f - r - 80f
        if (amp > maxAmp) amp = max(0f, maxAmp)
        val margin = r + 70f + amp
        val x = margin + rng.nextFloat() * (W - 2f * margin)
        val y = lastGenY - gap

        setupPlanet(p, x, y, r, amp, type)
        p.order = genCount
        if (isGoal) { p.goal = true; p.body = goalBody }

        // Schwarzes Loch
        var holeX = -9999f; var holeY = -9999f
        val holesOn = if (level > 0) level >= Levels.HOLES_FROM else genCount >= 14
        if (holesOn && !isGoal && rng.nextFloat() < 0.15f + 0.2f * d) {
            val mx = (lastGenX + x) / 2f; val my = (lastGenY + y) / 2f
            val side = if (mx < W / 2f) 1f else -1f
            val hx = (mx + side * (250f + rng.nextFloat() * 90f)).coerceIn(70f, W - 70f)
            val hy = my + (rng.nextFloat() - 0.5f) * 120f
            if (dist(hx, hy, x, y) > r + CAPTURE + 80f && dist(hx, hy, lastGenX, lastGenY) > 250f) {
                for (h in holes) if (!h.active) {
                    h.x = hx; h.y = hy; h.spin = rng.nextFloat() * 6f; h.active = true
                    holeX = hx; holeY = hy
                    break
                }
            }
        }

        // Stern oder Power-up dazwischen
        var sx = ((lastGenX + x) / 2f + (rng.nextFloat() - 0.5f) * 240f).coerceIn(60f, W - 60f)
        val sy = (lastGenY + y) / 2f
        if (dist(sx, sy, holeX, holeY) < 160f) sx = (lastGenX + x) / 2f
        val powerOn = if (level > 0) level >= Levels.POWERUPS_FROM else genCount >= 5
        val roll = rng.nextFloat()
        if (powerOn && !isGoal && roll < 0.07f) {
            for (pw in powers) if (!pw.active) {
                pw.x = sx; pw.y = sy; pw.spin = 0f; pw.active = true
                pw.type = if (rng.nextFloat() < 0.5f) P_SHIELD else P_MAGNET
                break
            }
        } else if (roll < 0.67f) {
            for (s in stars) if (!s.active) {
                s.x = sx; s.y = sy; s.spin = rng.nextFloat() * 6f; s.active = true
                break
            }
        }

        // Meteorit
        val metOn = if (level > 0) level >= Levels.METEORS_FROM else genCount >= 20
        if (metOn && !isGoal && rng.nextFloat() < 0.12f + 0.2f * d) {
            for (m in meteors) if (!m.active) {
                m.x = rng.nextFloat() * W
                m.y = (lastGenY + y) / 2f + (rng.nextFloat() - 0.5f) * 160f
                m.vx = (if (rng.nextFloat() < 0.5f) -1f else 1f) * (220f + rng.nextFloat() * 180f * (0.5f + d))
                m.r = 26f + rng.nextFloat() * 10f
                m.spin = 0f
                m.active = true
                break
            }
        }
        lastGenX = x
        lastGenY = y
        genCount++
        return true
    }

    private fun dist(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx; val dy = ay - by
        return sqrt(dx * dx + dy * dy)
    }

    // ===================================================================== Steuerung

    fun launch(): Boolean {
        val c = current ?: return false
        if (dead || won) return false
        vx = -sin(angle) * dir * FLY_SPEED
        vy = cos(angle) * dir * FLY_SPEED
        lastPlanet = c
        current = null
        flyTime = 0f
        listener?.onLaunch()
        return true
    }

    /** Wiederbeleben auf dem zuletzt besuchten Planeten. */
    fun revive(): Boolean {
        if (!respawn()) return false
        burst(plX, plY, Gfx.GREEN, 26, 380f)
        return true
    }

    private fun respawn(): Boolean {
        val p = lastPlanet ?: current ?: return false
        if (!p.active) return false
        dead = false
        current = p
        angle = -PI.toFloat() / 2f
        orbitTarget = p.r + ORBIT_GAP
        orbitR = orbitTarget
        plX = p.x + cos(angle) * orbitR
        plY = p.y + sin(angle) * orbitR
        flyTime = 0f
        streak = 0
        trailCount = 0
        return true
    }

    // ===================================================================== Update

    fun update(dt: Float, playing: Boolean) {
        time += dt

        for (p in planets) if (p.active && p.amp > 0f) {
            p.x = p.baseX + sin(time * p.speed + p.phase) * p.amp
        }

        if (!dead) {
            val steps = max(1, ceil(dt / (1f / 240f)).toInt())
            val h = dt / steps
            for (i in 0 until steps) if (!stepPlayer(h, playing)) break
            trailX[trailHead] = plX; trailY[trailHead] = plY
            trailHead = (trailHead + 1) % TRAIL
            if (trailCount < TRAIL) trailCount++
        }

        if (magnetT > 0f) magnetT -= dt
        for (s in stars) if (s.active) {
            s.spin += dt * 2f
            if (magnetT > 0f && playing && !dead) {
                val dx = plX - s.x; val dy = plY - s.y
                val d = sqrt(dx * dx + dy * dy)
                if (d < 420f && d > 1f) { s.x += dx / d * 900f * dt; s.y += dy / d * 900f * dt }
            }
            if (playing && !dead) {
                val dx = s.x - plX; val dy = s.y - plY
                if (dx * dx + dy * dy < 55f * 55f) {
                    s.active = false
                    runStars++
                    burst(s.x, s.y, Gfx.GOLD, 12, 260f)
                    listener?.onStar()
                }
            }
            if (s.y > camY + worldH + 200f) s.active = false
        }
        for (h in holes) if (h.active) {
            h.spin += dt * 2.5f
            if (h.y > camY + worldH + 300f) h.active = false
        }
        for (pw in powers) if (pw.active) {
            pw.spin += dt
            if (playing && !dead) {
                val dx = pw.x - plX; val dy = pw.y - plY
                if (dx * dx + dy * dy < 65f * 65f) {
                    pw.active = false
                    powerupsTaken++
                    if (pw.type == P_SHIELD) shield = true else magnetT = MAGNET_TIME
                    burst(pw.x, pw.y, if (pw.type == P_SHIELD) Gfx.SHIELD else Gfx.RED, 18, 320f)
                    listener?.onPowerup(pw.type)
                }
            }
            if (pw.y > camY + worldH + 200f) pw.active = false
        }
        for (m in meteors) if (m.active) {
            m.x += m.vx * dt
            m.spin += dt * 3f
            if (m.x > W + 120f) m.x = -120f
            if (m.x < -120f) m.x = W + 120f
            if (m.y > camY + worldH + 300f) m.active = false
        }

        // Kamera
        val anchor = current ?: lastPlanet
        var targetY = camY
        if (anchor != null) targetY = anchor.y - worldH * 0.70f
        if (current == null && playing && !dead) targetY = min(targetY, plY - worldH * 0.40f)
        camY += (targetY - camY) * min(1f, dt * 3.5f)

        for (p in planets) if (p.active && p !== current && p !== lastPlanet && p.y > camY + worldH + 300f) {
            p.active = false
        }
        fillPlanets()

        for (p in particles) if (p.life > 0f) {
            p.life -= dt
            p.x += p.vx * dt; p.y += p.vy * dt
            p.vx *= 0.96f; p.vy *= 0.96f
        }
        if (shake > 0f) shake = max(0f, shake - dt * 2.5f)
        if (popT > 0f) { popT -= dt; popY -= dt * 60f }
    }

    private fun stepPlayer(h: Float, playing: Boolean): Boolean {
        val c = current
        if (c != null) {
            orbitR += (orbitTarget - orbitR) * min(1f, h * 5f)
            angle += dir * ORBIT_SPEED * c.orbitMul / orbitR * h
            if (angle > 6.2832f) angle -= 6.2832f
            if (angle < -6.2832f) angle += 6.2832f
            plX = c.x + cos(angle) * orbitR
            plY = c.y + sin(angle) * orbitR
            return true
        }
        if (!playing) return true

        flyTime += h
        var ax = 0f
        var ay = 0f
        for (p in planets) {
            if (!p.active || p === lastPlanet) continue
            val dx = p.x - plX; val dy = p.y - plY
            val d2 = dx * dx + dy * dy
            val d = sqrt(d2)
            if (d < p.r + CAPTURE) { capture(p, d); return true }
            if (d < 750f) {
                val k = p.r / 100f
                val a = GRAVITY * k * k / d2 * (if (dim == 2) -0.55f else 1f)
                ax += dx / d * a; ay += dy / d * a
            }
        }
        for (hl in holes) {
            if (!hl.active) continue
            val dx = hl.x - plX; val dy = hl.y - plY
            val d2 = dx * dx + dy * dy
            val d = sqrt(d2)
            if (d < HOLE_KILL) { die(); return false }
            if (d < 650f) {
                val a = GRAVITY * 0.9f / d2
                ax += dx / d * a; ay += dy / d * a
            }
        }
        for (m in meteors) {
            if (!m.active) continue
            val dx = m.x - plX; val dy = m.y - plY
            val rr = m.r + 12f
            if (dx * dx + dy * dy < rr * rr) { die(); return false }
        }
        vx += ax * h; vy += ay * h
        val sp = sqrt(vx * vx + vy * vy)
        if (sp > 0.001f) { vx = vx / sp * FLY_SPEED; vy = vy / sp * FLY_SPEED }
        plX += vx * h; plY += vy * h

        if (plX < -60f || plX > W + 60f ||
            plY > camY + worldH + 80f || plY < camY - worldH * 0.6f ||
            flyTime > 2.6f
        ) {
            die()
            return false
        }
        return true
    }

    private fun capture(p: Planet, d: Float) {
        val rx = plX - p.x; val ry = plY - p.y
        current = p
        angle = atan2(ry, rx)
        orbitR = d
        orbitTarget = p.r + ORBIT_GAP
        dir = if (rx * vy - ry * vx >= 0f) 1f else -1f

        val perfect = d < p.r + 72f
        if (perfect) {
            streak++; perfects++
            if (streak > bestStreak) bestStreak = streak
        } else streak = 0
        val pts = if (perfect) 1 + min(streak, 4) else 1
        score += pts
        captures++
        if (perfect) {
            popT = 0.9f; popX = plX; popY = plY - 40f
            popStr = L.f(L.PERFECT_POP, pts)
        }
        val col = if (p.body != 0) BODY_COLOR[p.body] else Gfx.PALETTE[p.colorIdx]
        burst(plX, plY, col, if (perfect) 22 else 12, if (perfect) 380f else 260f)
        listener?.onCapture(perfect, streak)
        if (p.goal) {
            won = true
            burst(p.x, p.y, Gfx.GREEN, 50, 600f)
            burst(p.x, p.y, Gfx.GOLD, 40, 500f)
            listener?.onGoal()
        }
    }

    private fun die() {
        if (shield) {
            shield = false
            burst(plX, plY, Gfx.SHIELD, 30, 450f)
            if (respawn()) {
                burst(plX, plY, Gfx.SHIELD, 20, 300f)
                listener?.onShieldBreak()
                return
            }
        }
        dead = true
        shake = 1f
        burst(plX, plY, Color.WHITE, 36, 520f)
        listener?.onDeath()
    }

    private fun burst(x: Float, y: Float, color: Int, count: Int, speed: Float) {
        val n = if (reduced) count / 2 else count
        var made = 0
        for (p in particles) {
            if (made >= n) break
            if (p.life > 0f) continue
            val a = Random.nextFloat() * 6.2832f
            val s = speed * (0.3f + Random.nextFloat() * 0.7f)
            p.x = x; p.y = y
            p.vx = cos(a) * s; p.vy = sin(a) * s
            p.maxLife = 0.4f + Random.nextFloat() * 0.5f
            p.life = p.maxLife
            p.size = 4f + Random.nextFloat() * 6f
            p.color = color
            made++
        }
    }

    // ===================================================================== Zeichnen

    fun draw(c: Canvas, skin: Int, trail: Int) {
        drawBackground(c)
        var sx = 0f; var sy = 0f
        if (shake > 0f && shakeOn) {
            sx = (Random.nextFloat() - 0.5f) * 28f * shake
            sy = (Random.nextFloat() - 0.5f) * 28f * shake
        }
        c.save()
        c.translate(sx, -camY + sy)

        val top = camY - 400f
        val bottom = camY + worldH + 400f

        for (h in holes) if (h.active && h.y > top && h.y < bottom) drawHole(c, h)

        for (p in planets) {
            if (!p.active || p.y < top || p.y > bottom) continue
            val col = if (p.body != 0) BODY_COLOR[p.body] else Gfx.PALETTE[p.colorIdx]
            if (p === current) {
                gfx.ring(c, p.x, p.y, orbitTarget, col, 3f, 70)
            } else if (p !== lastPlanet) {
                gfx.ring(c, p.x, p.y, p.r + CAPTURE, if (p.goal) Gfx.GREEN else col, if (p.goal) 4f else 2f, if (p.goal) 110 else 40)
            }
            if (p.goal && p.body == 0) {
                if (!reduced) gfx.glow(c, gfx.goalGlow, p.x, p.y, p.r * 2.6f)
            }
            if (p.body != 0) gfx.drawBody(c, p.body, p.x, p.y, p.r, time, reduced)
            else {
                gfx.drawPlanet(c, p.type, p.colorIdx, p.x, p.y, p.r, reduced)
                if (dim == 2) gfx.ring(c, p.x, p.y, p.r + 4f, Color.WHITE, 3f, 150 + (sin(time * 4f + p.phase) * 80f).toInt())
            }
            if (p.goal) drawGoalMarker(c, p)
        }

        for (s in stars) {
            if (!s.active || s.y < top || s.y > bottom) continue
            if (!reduced) gfx.glow(c, gfx.starGlow, s.x, s.y, 60f)
            gfx.starShape(c, s.x, s.y, 22f + sin(time * 5f + s.spin) * 3f, s.spin * 57.3f, Gfx.GOLD)
        }

        for (pw in powers) {
            if (!pw.active || pw.y < top || pw.y > bottom) continue
            val col = if (pw.type == P_SHIELD) Gfx.SHIELD else Gfx.RED
            if (!reduced) gfx.glow(c, if (pw.type == P_SHIELD) gfx.shieldGlow else gfx.fireGlow, pw.x, pw.y, 85f)
            val bob = sin(time * 3f + pw.spin) * 6f
            gfx.circle(c, pw.x, pw.y + bob, 38f, Gfx.PANEL, 230)
            gfx.ring(c, pw.x, pw.y + bob, 38f, col, 4f)
            gfx.icon(c, if (pw.type == P_SHIELD) Gfx.I_SHIELD else Gfx.I_MAGNET, pw.x, pw.y + bob, 20f, col)
        }
        for (m in meteors) {
            if (!m.active || m.y < top || m.y > bottom) continue
            val dirX = if (m.vx > 0f) -1f else 1f
            if (!reduced) gfx.glow(c, gfx.fireGlow, m.x + dirX * m.r * 0.8f, m.y, m.r * 3f)
            for (k in 1..4) gfx.circle(c, m.x + dirX * k * m.r * 0.55f, m.y, m.r * (1f - k * 0.18f), 0xFFFF7A00.toInt(), 150 - k * 30)
            gfx.circle(c, m.x, m.y, m.r, 0xFF6D5D4F.toInt())
            gfx.circle(c, m.x + cos(m.spin) * m.r * 0.4f, m.y + sin(m.spin) * m.r * 0.4f, m.r * 0.25f, 0xFF4E4237.toInt())
            gfx.circle(c, m.x - cos(m.spin + 2f) * m.r * 0.45f, m.y - sin(m.spin + 2f) * m.r * 0.45f, m.r * 0.18f, 0xFF4E4237.toInt())
        }

        if (!dead) {
            val start = (trailHead - trailCount + TRAIL) % TRAIL
            gfx.drawTrail(c, trail, trailX, trailY, start, trailCount, TRAIL, gfx.skinColor(skin, time), time)
            if (current != null && !won) drawAim(c)
            gfx.drawSkin(c, skin, plX, plY, 1f, time, reduced)
            if (shield) {
                gfx.ring(c, plX, plY, 34f + sin(time * 5f) * 2f, Gfx.SHIELD, 4f, 200)
                gfx.circle(c, plX, plY, 34f, Gfx.SHIELD, 40)
            }
            if (magnetT > 0f) {
                val a = (magnetT.coerceAtMost(1f) * 120f).toInt()
                gfx.ring(c, plX, plY, 60f + (time * 120f) % 60f, Gfx.RED, 3f, a)
            }
        }

        for (p in particles) {
            if (p.life <= 0f) continue
            val f = p.life / p.maxLife
            gfx.circle(c, p.x, p.y, p.size * (0.4f + 0.6f * f), p.color, (f * 255f).toInt())
        }

        if (popT > 0f) {
            gfx.text(c, popStr, popX, popY, 54f, Gfx.GOLD, alpha = (min(1f, popT / 0.4f) * 255f).toInt())
        }
        c.restore()
    }

    private val nebX = floatArrayOf(150f, 820f, 300f, 760f)
    private val nebY = floatArrayOf(0.2f, 0.65f, 1.15f, 1.6f)

    /** Sternenhimmel + Nebel (auch für Menüs). */
    fun drawBackground(c: Canvas) {
        if (!reduced) {
            val band = worldH * 2f
            for (i in 0 until 4) {
                var y = (nebY[i] * worldH - camY * 0.03f) % band
                if (y < 0f) y += band
                y -= worldH * 0.4f
                gfx.glow(c, if (dim == 2) gfx.nebula2[i] else gfx.nebula[i], nebX[i], y, 720f)
            }
        }
        drawStarfield(c)
    }

    private fun drawStarfield(c: Canvas) {
        val layers = if (reduced) 1 else 3
        for (l in 0 until layers) {
            var off = (-camY * layerDepth[l]) % worldH
            if (off < 0f) off += worldH
            c.save()
            c.translate(0f, off)
            c.drawPoints(layerPts[l], starPaints[l])
            c.translate(0f, -worldH)
            c.drawPoints(layerPts[l], starPaints[l])
            c.restore()
        }
    }

    private fun drawHole(c: Canvas, h: Hole) {
        if (!reduced) gfx.glow(c, gfx.holeGlow, h.x, h.y, 170f)
        val st = gfx.stroke
        st.color = 0xFFB388FF.toInt(); st.alpha = 210; st.strokeWidth = 7f
        gfx.rect.set(h.x - 62f, h.y - 62f, h.x + 62f, h.y + 62f)
        for (k in 0 until 3) c.drawArc(gfx.rect, h.spin * 57.3f + k * 120f, 70f, false, st)
        st.alpha = 110; st.strokeWidth = 4f
        gfx.rect.set(h.x - 88f, h.y - 88f, h.x + 88f, h.y + 88f)
        for (k in 0 until 3) c.drawArc(gfx.rect, -h.spin * 40f + k * 120f, 50f, false, st)
        gfx.circle(c, h.x, h.y, 40f, Color.BLACK)
        gfx.ring(c, h.x, h.y, 40f, 0xFF9B5DE5.toInt(), 4f)
    }

    private fun drawGoalMarker(c: Canvas, p: Planet) {
        val bx = p.x
        val by = p.y - p.r
        gfx.line.color = Color.WHITE; gfx.line.alpha = 230; gfx.line.strokeWidth = 5f
        c.drawLine(bx, by, bx, by - 80f, gfx.line)
        c.save()
        c.translate(bx, by - 80f)
        c.rotate(sin(time * 3f) * 4f)
        gfx.fill.color = Gfx.GREEN; gfx.fill.alpha = 255
        c.drawPath(flagPath, gfx.fill)
        c.restore()
        val label = if (p.body != 0) Levels.bodyName(p.body) else L.t(L.GOAL)
        gfx.text(c, label, p.x, p.y + p.r + 70f, 44f, Gfx.GREEN)
    }

    private fun drawAim(c: Canvas) {
        val tx = -sin(angle) * dir
        val ty = cos(angle) * dir
        for (i in 1..6) {
            val d = 30f + i * 26f
            gfx.circle(c, plX + tx * d, plY + ty * d, 5f - i * 0.4f, Color.WHITE, 150 - i * 22)
        }
    }
}
