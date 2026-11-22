package de.engelcode.orbit

/** Kleiner JSON-Leser/-Schreiber (ohne Bibliothek, läuft auf Android und im Browser). */
object Json {

    fun parse(s: String): Any? = try { P(s).value() } catch (_: Throwable) { null }

    private class P(val s: String) {
        var i = 0
        fun ws() { while (i < s.length && s[i] <= ' ') i++ }
        fun value(): Any? {
            ws()
            if (i >= s.length) return null
            return when (s[i]) {
                '{' -> obj()
                '[' -> arr()
                '"' -> str()
                't' -> { i += 4; true }
                'f' -> { i += 5; false }
                'n' -> { i += 4; null }
                else -> num()
            }
        }
        fun obj(): Map<String, Any?> {
            val m = LinkedHashMap<String, Any?>()
            i++; ws()
            if (s[i] == '}') { i++; return m }
            while (true) {
                ws(); val k = str(); ws(); i++ // :
                m[k] = value(); ws()
                if (s[i] == ',') { i++; continue }
                i++; return m
            }
        }
        fun arr(): List<Any?> {
            val l = ArrayList<Any?>()
            i++; ws()
            if (s[i] == ']') { i++; return l }
            while (true) {
                l.add(value()); ws()
                if (s[i] == ',') { i++; continue }
                i++; return l
            }
        }
        fun str(): String {
            val sb = StringBuilder()
            i++
            while (s[i] != '"') {
                val c = s[i]
                if (c == '\\') {
                    i++
                    when (s[i]) {
                        'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> {}
                        'u' -> { sb.append(s.substring(i + 1, i + 5).toInt(16).toChar()); i += 4 }
                        else -> sb.append(s[i])
                    }
                } else sb.append(c)
                i++
            }
            i++
            return sb.toString()
        }
        fun num(): Double {
            val st = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            return s.substring(st, i).toDouble()
        }
    }

    fun q(s: String): String {
        val sb = StringBuilder("\"")
        for (c in s) when (c) {
            '"' -> sb.append("\\\""); '\\' -> sb.append("\\\\"); '\n' -> sb.append("\\n")
            else -> if (c < ' ') sb.append(' ') else sb.append(c)
        }
        return sb.append('"').toString()
    }

    /** Baut ein JSON-Objekt aus Paaren (Werte: String, Zahl, Boolean, null). */
    fun obj(vararg kv: Pair<String, Any?>): String {
        val sb = StringBuilder("{")
        for ((n, p) in kv.withIndex()) {
            if (n > 0) sb.append(',')
            sb.append(q(p.first)).append(':')
            when (val v = p.second) {
                null -> sb.append("null")
                is String -> sb.append(q(v))
                is Boolean -> sb.append(if (v) "true" else "false")
                else -> sb.append(v.toString())
            }
        }
        return sb.append('}').toString()
    }

    fun num(v: Any?): Double = (v as? Double) ?: (v as? Number)?.toDouble() ?: 0.0
    fun int(v: Any?): Int = num(v).toInt()
    fun long(v: Any?): Long = num(v).toLong()
    fun str(v: Any?): String = v as? String ?: ""
    @Suppress("UNCHECKED_CAST")
    fun map(v: Any?): Map<String, Any?> = v as? Map<String, Any?> ?: emptyMap()
}
