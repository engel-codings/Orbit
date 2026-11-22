package de.engelcode.orbit

import kotlinx.browser.window
import kotlin.js.Date

/** Web-Teil der Plattform-Funktionen. */
object Platform {
    fun now(): Long = Date.now().toLong()
    fun tzOffset(now: Long): Int = (-Date(now.toDouble()).getTimezoneOffset() * 60000.0).toInt()
    fun systemLanguage(): String = window.navigator.language.substringBefore('-').lowercase()
}
