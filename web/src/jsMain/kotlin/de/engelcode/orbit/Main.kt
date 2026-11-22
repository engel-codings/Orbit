package de.engelcode.orbit

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import kotlin.math.min

/** Startpunkt der Web-Version: Canvas, Spielschleife und Eingabe. */
fun main() {
    if (document.readyState.toString() == "loading") window.addEventListener("DOMContentLoaded", { start() })
    else start()
}

private fun start() {
    val canvas = document.getElementById("game") as HTMLCanvasElement
    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    val game = Game(Context())
    val c = Canvas(ctx)
    var dpr = 1.0

    fun resize() {
        dpr = min(window.devicePixelRatio, 2.5)
        val vw = window.innerWidth.toDouble()
        val vh = window.innerHeight.toDouble()
        // Hochformat: am PC wird das Spiel mittig in Handy-Form angezeigt
        val w = min(vw, vh * 0.62)
        canvas.style.width = "${w}px"
        canvas.style.height = "${vh}px"
        canvas.width = (w * dpr).toInt()
        canvas.height = (vh * dpr).toInt()
        game.resize(canvas.width, canvas.height)
    }
    resize()
    window.addEventListener("resize", { resize() })

    var last = -1.0
    fun frame(t: Double) {
        val dt = if (last < 0) 0.016 else ((t - last) / 1000.0).coerceIn(0.0, 0.05)
        last = t
        game.update(dt.toFloat())
        ctx.setTransform(1.0, 0.0, 0.0, 1.0, 0.0, 0.0)
        game.draw(c)
        window.requestAnimationFrame { frame(it) }
    }
    window.requestAnimationFrame { frame(it) }

    // Eingabe (Finger und Maus)
    var pointers = 0
    var primary = -1
    fun pos(e: PointerEvent): Pair<Float, Float> {
        val r = canvas.getBoundingClientRect()
        val sx = canvas.width / r.width
        val sy = canvas.height / r.height
        return Pair(((e.clientX - r.left) * sx).toFloat(), ((e.clientY - r.top) * sy).toFloat())
    }
    canvas.addEventListener("pointerdown", { ev ->
        val e = ev as PointerEvent
        e.preventDefault()
        Sound.unlockAll()
        pointers++
        val (x, y) = pos(e)
        if (primary == -1) {
            primary = e.pointerId
            game.onTouch(MotionEvent.ACTION_DOWN, x, y)
        } else game.onTouch(MotionEvent.ACTION_POINTER_DOWN, x, y)
    })
    canvas.addEventListener("pointermove", { ev ->
        val e = ev as PointerEvent
        if (e.pointerId != primary) return@addEventListener
        val (x, y) = pos(e)
        game.onTouch(MotionEvent.ACTION_MOVE, x, y)
    })
    val up = { ev: Event ->
        val e = ev as PointerEvent
        pointers = (pointers - 1).coerceAtLeast(0)
        if (e.pointerId == primary) {
            val (x, y) = pos(e)
            game.onTouch(if (ev.type == "pointercancel") MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP, x, y)
            primary = -1
        }
    }
    canvas.addEventListener("pointerup", up)
    canvas.addEventListener("pointercancel", up)

    // Zurück: Escape-Taste
    window.addEventListener("keydown", { ev ->
        val k = (ev as KeyboardEvent).key
        if (k == "Escape" || k == "Backspace") game.onBack()
    })

    // App im Hintergrund → Pause
    document.addEventListener("visibilitychange", {
        if (document.asDynamic().hidden == true) game.onPause() else game.onResume()
    })

    // Offline-Unterstützung
    try {
        val nav: dynamic = window.navigator
        if (nav.serviceWorker != undefined) nav.serviceWorker.register("sw.js")
    } catch (_: Throwable) {}
}
