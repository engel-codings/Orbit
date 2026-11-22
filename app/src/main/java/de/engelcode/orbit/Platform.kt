package de.engelcode.orbit

import java.util.Locale
import java.util.TimeZone

/** Android-Teil der Plattform-Funktionen (die Web-Version hat ihre eigene Platform.kt). */
object Platform {
    fun now(): Long = System.currentTimeMillis()
    fun tzOffset(now: Long): Int = TimeZone.getDefault().getOffset(now)
    fun systemLanguage(): String = Locale.getDefault().language
}
