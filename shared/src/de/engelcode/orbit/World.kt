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
        fun onSkip(n: Int) {}
        fun onRedirect() {}
        fun onBoost() {}
        fun onWormhole() {}
        fun onExplosion() {}
        fun onCombo(n: Int) {}
    }

    companion object {
        const val W = 1000f
        const val FLY_SPEED = 1020f
        const val ORBIT_SPEED = 640f
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
    private class Rock { var x = 0f; var y = 0f; var r = 0f; var spin = 0f; var active = false }
    private class Laser { var y = 0f; var x1 = 0f; var x2 = 0f; var phase = 0f; var active = false }
    private class Worm { var ax = 0f; var ay = 0f; var bx = 0f; var by = 0f; var spin = 0f; var active = false }
    private class Ring { var x = 0f; var y = 0f; var spin = 0f; var active = false }
    private class Comet { var x = 0f; var y = 0f; var vx = 0f; var vy = 0f; var t = 0f; var active = false }
    private class Meteor { var x = 0f; var y = 0f; var vx = 0f; var r = 0f; var spin = 0f; var active = false }
    private class Particle {
        var x = 0f; var y = 0f; var vx = 0f; var vy = 0f
        var life = 0f; var maxLife = 1f; var size = 0f; var color = 0
    }

    var listener: Listener? = null
    /** Grafikqualität: 0 = Hoch, 1 = Mittel, 2 = Sparsam. */
    var quality = 0
    val reduced get() = quality == 2
    private val noGlow get() = quality >= 1
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

    private val rocks = Array(30) { Rock() }
    private val lasers = Array(3) { Laser() }
    private val worms = Array(2) { Worm() }
    private val rings = Array(4) { Ring() }
    private val comets = Array(4) { Comet() }
    private var cometTimer = 3f
    private var boostT = 0f
    private var wormCool = 0f

    // Fähigkeiten (aus Upgrades)
    var redirects = 1
    var captureMul = 1f
    var magnetBonus = 0f
    var aimMul = 1f
    var skippedTotal = 0; private set
    var redirectsUsed = 0; private set
    var wormsUsed = 0; private set

    var shield = false
    fun giveShield() { shield = true }
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
    /** Sonnenwind (nur Dimension 4): positive Werte = nach rechts. */
    val windX: Float get() = if (dim == 4) sin(time * 0.55f) * 330f + sin(time * 1.7f) * 60f else 0f

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
    fun reset(level: Int, seed: Long = 0L, endlessDim: Int = 1) {
        this.level = level
        dim = if (level > 0) Levels.dimension(level) else endlessDim
        rng = when {
            level > 0 -> Random(Levels.seed(level))
            seed != 0L -> Random(seed)
            else -> Random(Platform.now() * 31L + Random.nextLong())
        }
        target = if (level > 0) Levels.planetCount(level) else 0
        for (p in planets) p.active = false
        for (s in stars) s.active = false
        for (h in holes) h.active = false
        for (p in powers) p.active = false
        for (m in meteors) m.active = false
        for (r in rocks) r.active = false
        for (l in lasers) l.active = false
        for (w in worms) w.active = false
        for (r in rings) r.active = false
        for (c in comets) c.active = false
        boostT = 0f; wormCool = 0f; skippedTotal = 0; redirectsUsed = 0; wormsUsed = 0; hazardGap = 1; ghostOrder = -1; combo = 0; bestCombo = 0
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
        // Nach einem Hindernis kommt immer mindestens eine ruhige Lücke
        val calm = hazardGap < 1
        hazardGap++
        var hazard = false
        if (metOn && !isGoal && !calm && rng.nextFloat() < 0.1f + 0.14f * d) {
            hazard = true
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
        // Neue Elemente: Boost-Ring, Asteroiden, Wurmloch, Laser (stufenweise freigeschaltet)
        if (!isGoal) {
            val lv = if (level > 0) level else genCount
            val my = (lastGenY + y) / 2f
            val mx = (lastGenX + x) / 2f
            if (lv >= Levels.BOOST_FROM && rng.nextFloat() < 0.3f) {
                for (r in rings) if (!r.active) {
                    r.x = (mx + (rng.nextFloat() - 0.5f) * 300f).coerceIn(90f, W - 90f); r.y = my + (rng.nextFloat() - 0.5f) * 80f
                    if (dist(r.x, r.y, holeX, holeY) > 180f) r.active = true
                    break
                }
            }
            val f = rng.nextFloat()
            if (calm || hazard) {
                // ruhige Lücke: höchstens ein Wurmloch
                if (lv >= Levels.WORM_FROM && f < 0.12f) spawnWorm(mx, my, y, p.r)
            } else if (lv >= Levels.ROCKS_FROM && f < 0.16f + 0.1f * d) {
                hazard = true
                val side = if (mx < W / 2f) 1f else -1f
                val cx = (mx + side * (200f + rng.nextFloat() * 120f)).coerceIn(80f, W - 80f)
                val n = 3 + rng.nextInt(3)
                var made = 0
                for (r in rocks) if (!r.active && made < n) {
                    r.x = (cx + (rng.nextFloat() - 0.5f) * 170f).coerceIn(30f, W - 30f)
                    r.y = my + (rng.nextFloat() - 0.5f) * 150f
                    r.r = 16f + rng.nextFloat() * 14f; r.spin = rng.nextFloat() * 6f
                    if (dist(r.x, r.y, x, y) > p.r + CAPTURE + 40f && dist(r.x, r.y, lastGenX, lastGenY) > 230f) r.active = true
                    made++
                }
            } else if (lv >= Levels.LASER_FROM && f < 0.26f + 0.06f * d) {
                hazard = true
                for (l in lasers) if (!l.active) {
                    val left = rng.nextFloat() < 0.5f
                    l.y = my; l.x1 = if (left) 0f else W * 0.3f; l.x2 = if (left) W * 0.7f else W
                    l.phase = rng.nextFloat() * 3f
                    if (kotlin.math.abs(my - y) > p.r + CAPTURE + 30f) l.active = true
                    break
                }
            } else if (lv >= Levels.WORM_FROM && f < 0.45f) spawnWorm(mx, my, y, p.r)
        }
        if (hazard) hazardGap = 0
        lastGenX = x
        lastGenY = y
        genCount++
        return true
    }

    private var hazardGap = 0
    var ghostOrder = -1
    var combo = 0; private set
    var bestCombo = 0; private set
    var comboBonus = 0f
    var ghostName = ""

    /** Dimension 7 (Pulsar): Fangzone pulsiert zwischen klein und groß. */
    fun pulseMul() = if (dim == 7) 0.7f + 0.45f * (0.5f + 0.5f * sin(time * 2.4f)) else 1f

    private fun spawnWorm(mx: Float, my: Float, y: Float, pr: Float) {
        for (w in worms) if (!w.active) {
            w.ax = (if (mx < W / 2f) mx + 260f else mx - 260f).coerceIn(90f, W - 90f); w.ay = my
            w.bx = (W - w.ax).coerceIn(90f, W - 90f); w.by = y - pr - 260f
            w.active = true
            break
        }
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

    /** Fähigkeit „Umlenken“: im Flug zum nächsten Planeten vor dir lenken. */
    fun redirect(): Boolean {
        if (current != null || dead || won || redirects <= 0) return false
        if (flyTime < 0.2f) return false
        var best: Planet? = null
        var bd = Float.MAX_VALUE
        for (p in planets) {
            if (!p.active || p === lastPlanet || p.y > plY + 60f) continue
            val d = dist(p.x, p.y, plX, plY)
            if (d < bd) { bd = d; best = p }
        }
        val t = best ?: return false
        val dx = t.x - plX; val dy = t.y - plY
        val d = sqrt(dx * dx + dy * dy)
        vx = dx / d * FLY_SPEED; vy = dy / d * FLY_SPEED
        flyTime = 0f
        redirects--
        redirectsUsed++
        burst(plX, plY, Gfx.ACCENT, 18, 300f)
        listener?.onRedirect()
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

    private var live = false

    fun update(dtIn: Float, playing: Boolean) {
        val dt = if (dim == 6) dtIn * 1.2f else dtIn
        live = playing
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
                    if (pw.type == P_SHIELD) shield = true else magnetT = MAGNET_TIME + magnetBonus
                    burst(pw.x, pw.y, if (pw.type == P_SHIELD) Gfx.SHIELD else Gfx.RED, 18, 320f)
                    listener?.onPowerup(pw.type)
                }
            }
            if (pw.y > camY + worldH + 200f) pw.active = false
        }
        for (i in meteors.indices) {
            val a = meteors[i]; if (!a.active) continue
            for (j in i + 1 until meteors.size) {
                val b = meteors[j]; if (!b.active) continue
                if (dist(a.x, a.y, b.x, b.y) < a.r + b.r) {
                    explode((a.x + b.x) / 2f, (a.y + b.y) / 2f)
                    a.active = false; b.active = false
                }
            }
        }
        for (m in meteors) if (m.active) {
            m.x += m.vx * dt
            m.spin += dt * 3f
            if (m.x > W + 120f) m.x = -120f
            if (m.x < -120f) m.x = W + 120f
            if (m.y > camY + worldH + 300f) m.active = false
        }

        if (boostT > 0f) boostT -= dt
        if (wormCool > 0f) wormCool -= dt
        for (r in rocks) if (r.active) { r.spin += dt; if (r.y > camY + worldH + 300f) r.active = false }
        for (l in lasers) if (l.active && l.y > camY + worldH + 300f) l.active = false
        for (w in worms) if (w.active) { w.spin += dt * 3f; if (w.ay > camY + worldH + 300f && w.by > camY + worldH + 300f) w.active = false }
        for (r in rings) if (r.active) {
            r.spin += dt * 2f
            if (playing && !dead && current == null && dist(r.x, r.y, plX, plY) < 55f) {
                r.active = false
                boostT = 1.2f
                score += 2
                popT = 0.9f; popX = r.x; popY = r.y - 40f; popStr = "BOOST +2"
                burst(r.x, r.y, Gfx.GOLD, 20, 400f)
                listener?.onBoost()
            }
            if (r.y > camY + worldH + 300f) r.active = false
        }
        updateComets(dt)

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
        val pulse = pulseMul()
        for (p in planets) {
            if (!p.active || p === lastPlanet) continue
            val dx = p.x - plX; val dy = p.y - plY
            val d2 = dx * dx + dy * dy
            val d = sqrt(d2)
            if (d < p.r + CAPTURE * captureMul * pulse) { capture(p, d); return true }
            if (d < 750f) {
                val k = p.r / 100f
                val a = GRAVITY * k * k / d2 * (when (dim) { 2 -> -0.55f; 3 -> 0f; 4 -> 0.8f; 5 -> 1.15f; 6 -> 0.9f; 8 -> 1.05f; else -> 1f })
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
        for (r in rocks) {
            if (!r.active) continue
            val rr = r.r + 12f
            val dx = r.x - plX; val dy = r.y - plY
            if (dx * dx + dy * dy < rr * rr) { r.active = false; explode(r.x, r.y); die(); return false }
        }
        for (l in lasers) {
            if (!l.active || !laserOn(l)) continue
            if (kotlin.math.abs(plY - l.y) < 9f && plX > l.x1 && plX < l.x2) { die(); return false }
        }
        if (wormCool <= 0f) for (w in worms) {
            if (!w.active) continue
            if (dist(w.ax, w.ay, plX, plY) < 45f) {
                burst(plX, plY, 0xFF7B61FF.toInt(), 20, 350f)
                plX = w.bx; plY = w.by
                trailCount = 0
                wormCool = 0.6f
                flyTime = 0f
                wormsUsed++
                burst(plX, plY, Gfx.ORANGE, 20, 350f)
                listener?.onWormhole()
                break
            }
        }
        if (dim == 4) ax += windX
        if (dim == 8) ax += (W / 2f - plX) * 1.7f
        vx += ax * h; vy += ay * h
        val sp = sqrt(vx * vx + vy * vy)
        val speed = (if (dim == 3) FLY_SPEED * 1.12f else FLY_SPEED) * (if (boostT > 0f) 1.35f else 1f) *
            (if (dim == 5) max(0.68f, 1f - flyTime * 0.2f) else 1f)
        if (sp > 0.001f) { vx = vx / sp * speed; vy = vy / sp * speed }
        plX += vx * h; plY += vy * h

        if (plX < -60f || plX > W + 60f ||
            plY > camY + worldH + 80f || plY < camY - worldH * 1.5f ||
            flyTime > 5f
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

        val skipped = (p.order - (lastPlanet?.order ?: p.order) - 1).coerceAtLeast(0)
        val perfect = d < p.r + 72f
        if (perfect) {
            streak++; perfects++
            if (streak > bestStreak) bestStreak = streak
        } else streak = 0
        val pts = if (perfect) 1 + min(streak, 4) else 1
        // Combo: schnelle Sprünge hintereinander geben Bonus
        if (flyTime < 1.15f + comboBonus) combo++ else combo = 1
        val comboPts = if (combo >= 3) min(combo - 2, 6) else 0
        if (combo > bestCombo) bestCombo = combo
        score += pts + skipped * 2 + comboPts
        captures++
        if (skipped > 0) {
            skippedTotal += skipped
            popT = 1f; popX = plX; popY = plY - 40f
            popStr = L.f(L.SKIP_POP, skipped * 2)
            burst(plX, plY, Gfx.GOLD, 16, 340f)
            listener?.onSkip(skipped)
        }
        if (perfect && skipped == 0) {
            popT = 0.9f; popX = plX; popY = plY - 40f
            popStr = L.f(L.PERFECT_POP, pts)
        }
        if (comboPts > 0) {
            popT = 1f; popX = plX; popY = plY - 90f
            popStr = L.f(L.COMBO_POP, combo, comboPts)
            listener?.onCombo(combo)
        }
        val col = if (p.body in 1 until BODY_COLOR.size) BODY_COLOR[p.body] else Gfx.PALETTE[(if (p.body != 0) p.body else p.colorIdx) % Gfx.PALETTE.size]
        burst(plX, plY, col, if (perfect) 22 else 12, if (perfect) 380f else 260f)
        listener?.onCapture(perfect, streak)
        if (p.goal) {
            won = true
            burst(p.x, p.y, Gfx.GREEN, 50, 600f)
            burst(p.x, p.y, Gfx.GOLD, 40, 500f)
            listener?.onGoal()
        }
    }

    private fun laserOn(l: Laser) = ((time + l.phase) % 2.6f) < 1.0f

    /** Explosion mit Partikeln und Wackeln. */
    private fun explode(x: Float, y: Float) {
        burst(x, y, Gfx.ORANGE, 26, 520f)
        burst(x, y, Gfx.GOLD, 16, 380f)
        burst(x, y, Color.WHITE, 10, 260f)
        shake = max(shake, 0.45f)
        if (live) listener?.onExplosion()
    }

    /** Hintergrund-Action: zwei Kometen stoßen zusammen und explodieren. */
    private fun updateComets(dt: Float) {
        if (reduced) return
        cometTimer -= dt
        if (cometTimer <= 0f) {
            cometTimer = 4f + Random.nextFloat() * 5f
            val tx = 150f + Random.nextFloat() * (W - 300f)
            val ty = camY + worldH * (0.15f + Random.nextFloat() * 0.5f)
            var k = 0
            for (c in comets) if (!c.active && k < 2) {
                val sx = if (k == 0) -80f else W + 80f
                val sy = ty - 200f + Random.nextFloat() * 120f
                c.x = sx; c.y = sy
                c.vx = (tx - sx) / 1.4f; c.vy = (ty - sy) / 1.4f
                c.t = 1.4f; c.active = true
                k++
            }
        }
        var boom = false; var bx = 0f; var by = 0f
        for (c in comets) if (c.active) {
            c.x += c.vx * dt; c.y += c.vy * dt; c.t -= dt
            if (c.t <= 0f) { c.active = false; boom = true; bx = c.x; by = c.y }
        }
        if (boom) {
            burst(bx, by, Gfx.ORANGE, 22, 450f)
            burst(bx, by, Color.WHITE, 10, 250f)
            if (live) {
                shake = max(shake, 0.25f)
                listener?.onExplosion()
            }
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
        val n = when (quality) { 0 -> count; 1 -> count * 3 / 4; else -> count / 2 }
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
            val col = if (p.body in 1 until BODY_COLOR.size) BODY_COLOR[p.body] else Gfx.PALETTE[(if (p.body != 0) p.body else p.colorIdx) % Gfx.PALETTE.size]
            if (p === current) {
                gfx.ring(c, p.x, p.y, orbitTarget, col, 3f, 70)
            } else if (p !== lastPlanet) {
                gfx.ring(c, p.x, p.y, p.r + CAPTURE, if (p.goal) Gfx.GREEN else col, if (p.goal) 4f else 2f, if (p.goal) 110 else 40)
            }
            if (p.goal && p.body == 0) {
                if (!reduced) gfx.glow(c, gfx.goalGlow, p.x, p.y, p.r * 2.6f)
            }
            if (p.body != 0) gfx.drawBody(c, p.body, p.x, p.y, p.r, time, noGlow)
            else {
                gfx.drawPlanet(c, p.type, p.colorIdx, p.x, p.y, p.r, noGlow)
                if (dim == 2) gfx.ring(c, p.x, p.y, p.r + 4f, Color.WHITE, 3f, 150 + (sin(time * 4f + p.phase) * 80f).toInt())
                if (dim == 4) gfx.ring(c, p.x, p.y, p.r + 3f, 0xFFFF9F1C.toInt(), 4f, 140 + (sin(time * 5f + p.phase) * 70f).toInt())
                if (dim == 5) {
                    gfx.ring(c, p.x, p.y, p.r + 4f, 0xFFBDE8FF.toInt(), 5f, 170)
                    for (k in 0 until 6) {
                        val a = k * 1.0472f + p.phase
                        gfx.circle(c, p.x + cos(a) * (p.r + 4f), p.y + sin(a) * (p.r + 4f), 5f, Color.WHITE, 200)
                    }
                }
                if (dim == 6) gfx.ring(c, p.x, p.y, p.r + 5f + sin(time * 9f + p.phase) * 3f, 0xFFE040FB.toInt(), 3f, 200)
                if (dim == 7) {
                    val pm = pulseMul()
                    gfx.ring(c, p.x, p.y, p.r + CAPTURE * captureMul * pm, 0xFF64FFDA.toInt(), 3f, (60 + 140 * (pm - 0.7f) / 0.45f).toInt())
                }
                if (dim == 8) gfx.ring(c, p.x, p.y, p.r + 4f, 0xFFFF1744.toInt(), 3f, 150)
                if (dim == 3) {
                    gfx.circle(c, p.x, p.y, p.r, Color.BLACK, 140)
                    gfx.ring(c, p.x, p.y, p.r, 0xFF2EE5C8.toInt(), 3f, 160 + (sin(time * 2f + p.phase) * 60f).toInt())
                }
            }
            if (p.goal) drawGoalMarker(c, p)
        }

        for (s in stars) {
            if (!s.active || s.y < top || s.y > bottom) continue
            if (!noGlow) gfx.glow(c, gfx.starGlow, s.x, s.y, 60f)
            gfx.starShape(c, s.x, s.y, 22f + sin(time * 5f + s.spin) * 3f, s.spin * 57.3f, Gfx.GOLD)
        }

        for (cm in comets) if (cm.active) {
            for (k in 1..5) gfx.circle(c, cm.x - cm.vx * 0.02f * k, cm.y - cm.vy * 0.02f * k, 9f - k * 1.4f, Gfx.ORANGE, 200 - k * 35)
            gfx.circle(c, cm.x, cm.y, 8f, Color.WHITE)
        }
        for (r in rings) if (r.active && r.y > top && r.y < bottom) {
            gfx.ring(c, r.x, r.y, 46f + sin(time * 6f + r.spin) * 3f, Gfx.GOLD, 7f, 230)
            gfx.ring(c, r.x, r.y, 34f, Color.WHITE, 2f, 120)
            gfx.icon(c, Gfx.I_SPEED, r.x, r.y, 14f, Gfx.GOLD)
        }
        for (rk in rocks) if (rk.active && rk.y > top && rk.y < bottom) {
            gfx.circle(c, rk.x, rk.y, rk.r, 0xFF5D4E42.toInt())
            gfx.circle(c, rk.x + cos(rk.spin) * rk.r * 0.35f, rk.y + sin(rk.spin) * rk.r * 0.35f, rk.r * 0.3f, 0xFF3E342C.toInt())
            gfx.ring(c, rk.x, rk.y, rk.r, Gfx.ORANGE, 2f, 90)
        }
        for (l in lasers) if (l.active && l.y > top && l.y < bottom) {
            val on = laserOn(l)
            gfx.circle(c, l.x1.coerceAtLeast(14f), l.y, 14f, Gfx.RED)
            gfx.circle(c, l.x2.coerceAtMost(W - 14f), l.y, 14f, Gfx.RED)
            gfx.line.color = Gfx.RED
            if (on) {
                gfx.line.alpha = 120; gfx.line.strokeWidth = 16f
                c.drawLine(l.x1, l.y, l.x2, l.y, gfx.line)
                gfx.line.color = Color.WHITE; gfx.line.alpha = 255; gfx.line.strokeWidth = 4f
                c.drawLine(l.x1, l.y, l.x2, l.y, gfx.line)
            } else {
                gfx.line.alpha = 70; gfx.line.strokeWidth = 3f
                var xx = l.x1
                while (xx < l.x2) { c.drawLine(xx, l.y, min(xx + 18f, l.x2), l.y, gfx.line); xx += 36f }
            }
        }
        for (w in worms) if (w.active) {
            for (k in 0 until 3) {
                gfx.ring(c, w.ax, w.ay, 26f + k * 12f, 0xFF7B61FF.toInt(), 4f, 220 - k * 60)
                gfx.ring(c, w.bx, w.by, 26f + k * 12f, Gfx.ORANGE, 4f, 220 - k * 60)
            }
            gfx.circle(c, w.ax, w.ay, 18f, Color.BLACK)
            gfx.circle(c, w.bx, w.by, 18f, Color.BLACK)
            gfx.starShape(c, w.ax, w.ay, 14f, w.spin * 60f, 0xFFB388FF.toInt())
            gfx.starShape(c, w.bx, w.by, 14f, -w.spin * 60f, Gfx.GOLD)
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
            if (!noGlow) gfx.glow(c, gfx.fireGlow, m.x + dirX * m.r * 0.8f, m.y, m.r * 3f)
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
        // Bot-Gegner als Geist sichtbar
        if (ghostOrder >= 0) for (p in planets) {
            if (!p.active || p.order != ghostOrder) continue
            val a = time * 2.2f
            val gx = p.x + cos(a) * (p.r + 34f); val gy = p.y + sin(a) * (p.r + 34f)
            gfx.circle(c, gx, gy, 26f, Gfx.ORANGE, 70)
            gfx.circle(c, gx, gy, 16f, Gfx.ORANGE, 200)
            gfx.circle(c, gx, gy, 7f, Color.WHITE, 220)
            gfx.text(c, ghostName, gx, gy - 38f, 26f, Gfx.ORANGE, alpha = 220)
            break
        }
        c.restore()
    }

    private val nebX = floatArrayOf(150f, 820f, 300f, 760f)
    private val nebY = floatArrayOf(0.2f, 0.65f, 1.15f, 1.6f)

    /** Sternenhimmel + Nebel (auch für Menüs). */
    fun drawBackground(c: Canvas) {
        if (quality == 0) {
            val band = worldH * 2f
            for (i in 0 until 4) {
                var y = (nebY[i] * worldH - camY * 0.03f) % band
                if (y < 0f) y += band
                y -= worldH * 0.4f
                gfx.glow(c, when (dim) { 2 -> gfx.nebula2[i]; 3 -> gfx.nebula3[i]; 4 -> gfx.nebula4[i]; 5 -> gfx.nebula5[i]; 6 -> gfx.nebula6[i]; 7 -> gfx.nebula7[i]; 8 -> gfx.nebula8[i]; else -> gfx.nebula[i] }, nebX[i], y, 720f)
            }
        }
        if (dim == 8) {
            // Schwarzes Loch in der Mitte: zieht alles zur Bildmitte
            val cx = W / 2f
            gfx.line.color = 0xFF7C4DFF.toInt(); gfx.line.strokeWidth = 3f
            for (k in 0 until 18) {
                val yy = ((k * 0.0556f) * worldH + time * 60f) % worldH
                val off = 160f + (k % 3) * 70f - ((time * 90f + k * 37f) % 200f)
                gfx.line.alpha = 90
                c.drawLine(cx - off, yy, cx - off * 0.6f, yy + 18f, gfx.line)
                c.drawLine(cx + off, yy, cx + off * 0.6f, yy + 18f, gfx.line)
            }
            gfx.line.color = 0xFF000000.toInt(); gfx.line.alpha = 140; gfx.line.strokeWidth = 26f
            c.drawLine(cx, 0f, cx, worldH, gfx.line)
            gfx.line.color = 0xFFFF1744.toInt(); gfx.line.alpha = 120; gfx.line.strokeWidth = 3f
            c.drawLine(cx - 14f, 0f, cx - 14f, worldH, gfx.line)
            c.drawLine(cx + 14f, 0f, cx + 14f, worldH, gfx.line)
        }
        if (dim == 4) {
            // sichtbare Windstreifen
            val w = windX
            val len = 40f + kotlin.math.abs(w) * 0.25f
            val a = (60 + kotlin.math.abs(w) * 0.35f).toInt().coerceAtMost(200)
            gfx.line.color = 0xFFFFC38A.toInt(); gfx.line.strokeWidth = 3f
            for (k in 0 until 16) {
                val baseY = (k * 0.0625f + (k % 3) * 0.02f) * worldH
                var x = (k * 211f + time * w * 1.6f) % (W + 200f)
                if (x < 0f) x += W + 200f
                x -= 100f
                gfx.line.alpha = a
                c.drawLine(x, baseY, x - len * kotlin.math.sign(w), baseY, gfx.line)
            }
        }
        drawStarfield(c)
    }

    private fun drawStarfield(c: Canvas) {
        val layers = when (quality) { 0 -> 3; 1 -> 2; else -> 1 }
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
        if (!noGlow) gfx.glow(c, gfx.holeGlow, h.x, h.y, 170f)
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
            val d = 30f + i * 26f * aimMul
            gfx.circle(c, plX + tx * d, plY + ty * d, 5f - i * 0.4f, Color.WHITE, 150 - i * 22)
        }
    }
}
