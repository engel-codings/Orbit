package de.engelcode.orbit

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * Zeichenfläche + Spielschleife in einem eigenen Thread.
 * Kein Browser, keine Engine: direkt auf die Grafik-Oberfläche von Android.
 */
@SuppressLint("ViewConstructor")
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val game = Game(context)
    @Volatile private var running = false
    private var thread: Thread? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        synchronized(game) {
            game.resize(width, height)
            game.onResume()
        }
        startLoop()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
        synchronized(game) { game.onPause() }
    }

    fun onBack(): Boolean = synchronized(game) { game.onBack() }

    fun release() = game.release()

    private fun startLoop() {
        if (running) return
        running = true
        thread = Thread(this, "OrbitLoop").also {
            it.priority = Thread.MAX_PRIORITY
            it.start()
        }
    }

    private fun stopLoop() {
        running = false
        val t = thread ?: return
        var retry = true
        while (retry) {
            try {
                t.join()
                retry = false
            } catch (_: InterruptedException) {
            }
        }
        thread = null
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var dt = (now - last) / 1_000_000_000f
            last = now
            if (dt > 0.05f) dt = 0.05f

            val canvas = try {
                if (Build.VERSION.SDK_INT >= 26) holder.lockHardwareCanvas() else holder.lockCanvas()
            } catch (_: Exception) {
                null
            }
            if (canvas == null) {
                try { Thread.sleep(5) } catch (_: InterruptedException) {}
                continue
            }
            try {
                synchronized(game) {
                    game.update(dt)
                    game.draw(canvas)
                }
            } finally {
                try { holder.unlockCanvasAndPost(canvas) } catch (_: Exception) {}
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val a = event.actionMasked
        val i = event.actionIndex
        synchronized(game) { game.onTouch(a, event.getX(i), event.getY(i)) }
        return true
    }
}
