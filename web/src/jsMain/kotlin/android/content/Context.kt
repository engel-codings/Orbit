@file:Suppress("unused", "UNUSED_PARAMETER", "PackageDirectoryMismatch")

package android.content

import android.content.res.AssetManager
import kotlinx.browser.localStorage

/** Browser-Ersatz für Android-Context: Spielstand liegt im localStorage. */
open class Context {
    companion object { const val MODE_PRIVATE = 0 }
    val assets = AssetManager()
    fun getSharedPreferences(name: String, mode: Int) = SharedPreferences("$name.")
}

class SharedPreferences(private val prefix: String) {
    private fun raw(k: String): String? = localStorage.getItem(prefix + k)
    fun contains(k: String) = raw(k) != null
    fun getInt(k: String, d: Int) = raw(k)?.toIntOrNull() ?: d
    fun getLong(k: String, d: Long) = raw(k)?.toLongOrNull() ?: d
    fun getFloat(k: String, d: Float) = raw(k)?.toFloatOrNull() ?: d
    fun getBoolean(k: String, d: Boolean) = raw(k)?.let { it == "true" } ?: d
    fun getString(k: String, d: String?) = raw(k) ?: d
    fun edit() = Editor(prefix)

    class Editor(private val prefix: String) {
        private fun put(k: String, v: String): Editor { localStorage.setItem(prefix + k, v); return this }
        fun putInt(k: String, v: Int) = put(k, v.toString())
        fun putLong(k: String, v: Long) = put(k, v.toString())
        fun putFloat(k: String, v: Float) = put(k, v.toString())
        fun putBoolean(k: String, v: Boolean) = put(k, v.toString())
        fun putString(k: String, v: String) = put(k, v)
        fun apply() {}
        fun commit() = true
    }
}
