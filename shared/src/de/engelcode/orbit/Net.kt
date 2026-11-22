package de.engelcode.orbit

import kotlin.random.Random

/**
 * Online-Teil: Konto (Login/Registrieren), Cloud-Speicher, Bestenliste, Freunde,
 * 1v1-Warteschlange und Duell-Einladungen. Alles über die Firebase-Datenbank (REST).
 * iPhone (Web) und Android benutzen dieselbe Datenbank.
 */
object Net {
    const val BASE = "https://orbit-game-8f095-default-rtdb.europe-west1.firebasedatabase.app"
    private const val SV = "{\".sv\":\"timestamp\"}"

    interface Listener {
        fun netToast(msg: String)
        fun netMatch(mid: String, role: String, seed: Long, opp: String)
        fun netInvite(fromPid: String, name: String, mid: String)
        fun netCloudLoaded(data: String)
        fun netChanged()
        fun netProfile(): String   // JSON-Teil mit best/level/crowns
        fun netExport(): String
    }

    var listener: Listener? = null

    // Konto (lokal gemerkt)
    var pid = ""; private set
    var name = ""; private set
    private var key = ""
    val loggedIn get() = pid.isNotEmpty()
    private var prefs: Any? = null
    private var store: ((String, String) -> Unit)? = null

    fun init(get: (String) -> String, set: (String, String) -> Unit) {
        pid = get("pid"); name = get("name"); key = get("key")
        store = set
    }

    private fun remember() {
        store?.invoke("pid", pid); store?.invoke("name", name); store?.invoke("key", key)
    }

    // ------------------------------------------------------------------ REST

    private fun url(path: String) = "$BASE/$path.json"
    private fun get(path: String, cb: (Any?) -> Unit) = Platform.http("GET", url(path), null) { cb(it?.let { s -> Json.parse(s) }) }
    private fun put(path: String, body: String, cb: (Boolean) -> Unit = {}) = Platform.http("PUT", url(path), body) { cb(it != null) }
    private fun patch(path: String, body: String, cb: (Boolean) -> Unit = {}) = Platform.http("PATCH", url(path), body) { cb(it != null) }
    private fun del(path: String) = Platform.http("DELETE", url(path), null) {}

    private fun keyOf(n: String) = n.trim().lowercase().replace(Regex("[.#$\\[\\]/]"), "_")
    private fun randId(n: Int): String {
        val ch = "abcdefghijkmnpqrstuvwxyz23456789"
        return (1..n).map { ch[Random.nextInt(ch.length)] }.joinToString("")
    }

    fun validName(n: String) = n.trim().length in 3..16 && n.trim().all { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }

    // ------------------------------------------------------------------ Konto

    var busy = false; private set

    fun register(n: String, pw: String, done: (Boolean) -> Unit) {
        val k = keyOf(n)
        busy = true
        get("users/$k") { v ->
            if (v != null) { busy = false; listener?.netToast(L.t(L.NET_NAME_TAKEN)); done(false); return@get }
            val salt = randId(12)
            val newPid = randId(14)
            val body = Json.obj("name" to n.trim(), "salt" to salt, "hash" to Sha256.hex(salt + pw), "pid" to newPid)
            put("users/$k", body) { ok ->
                busy = false
                if (!ok) { listener?.netToast(L.t(L.NET_OFFLINE)); done(false); return@put }
                pid = newPid; name = n.trim(); key = k; remember()
                pushProfile(); pushSave()
                listener?.netChanged()
                done(true)
            }
        }
    }

    fun login(n: String, pw: String, done: (Boolean) -> Unit) {
        val k = keyOf(n)
        busy = true
        get("users/$k") { v ->
            val m = Json.map(v)
            if (m.isEmpty()) { busy = false; listener?.netToast(L.t(L.NET_WRONG)); done(false); return@get }
            if (Sha256.hex(Json.str(m["salt"]) + pw) != Json.str(m["hash"])) { busy = false; listener?.netToast(L.t(L.NET_WRONG)); done(false); return@get }
            pid = Json.str(m["pid"]); name = Json.str(m["name"]); key = k; remember()
            get("saves/$pid") { sv ->
                busy = false
                val data = Json.str(Json.map(sv)["d"])
                listener?.netChanged()
                done(true)
                if (data.isNotEmpty()) listener?.netCloudLoaded(data) else { pushProfile(); pushSave() }
            }
        }
    }

    fun logout() {
        if (inQueue) leaveQueue()
        pid = ""; name = ""; key = ""; remember()
        friends.clear()
        listener?.netChanged()
    }

    fun rename(n: String, done: (Boolean) -> Unit) {
        val nk = keyOf(n)
        if (!loggedIn) return
        if (nk == key) {
            name = n.trim(); remember(); patch("players/$pid", Json.obj("name" to name)); patch("users/$key", Json.obj("name" to name))
            listener?.netChanged(); done(true); return
        }
        busy = true
        get("users/$nk") { v ->
            if (v != null) { busy = false; listener?.netToast(L.t(L.NET_NAME_TAKEN)); done(false); return@get }
            get("users/$key") { old ->
                val m = Json.map(old)
                if (m.isEmpty()) { busy = false; done(false); return@get }
                val body = Json.obj("name" to n.trim(), "salt" to Json.str(m["salt"]), "hash" to Json.str(m["hash"]), "pid" to pid)
                put("users/$nk", body) { ok ->
                    busy = false
                    if (!ok) { listener?.netToast(L.t(L.NET_OFFLINE)); done(false); return@put }
                    del("users/$key")
                    key = nk; name = n.trim(); remember()
                    patch("players/$pid", Json.obj("name" to name))
                    listener?.netChanged()
                    done(true)
                }
            }
        }
    }

    // ------------------------------------------------------------------ Profil & Cloud

    fun pushProfile() {
        if (!loggedIn) return
        val extra = listener?.netProfile() ?: ""
        patch("players/$pid", "{\"name\":${Json.q(name)},$extra,\"seen\":$SV}")
    }

    var lastCloud = 0L; private set
    fun pushSave() {
        if (!loggedIn) return
        val d = listener?.netExport() ?: return
        put("saves/$pid", "{\"d\":${Json.q(d)},\"t\":$SV}") { if (it) lastCloud = Platform.now() }
    }

    // ------------------------------------------------------------------ Bestenliste

    class Entry(val pid: String, val name: String, val level: Int, val best: Int, val wins: Int, val crowns: Int)

    val board = ArrayList<Entry>()
    var boardLoaded = false; private set
    var boardBusy = false; private set

    fun loadBoard() {
        if (boardBusy) return
        boardBusy = true
        get("players") { v ->
            boardBusy = false
            if (v == null && !boardLoaded) return@get
            board.clear()
            for ((k, e) in Json.map(v)) {
                val m = Json.map(e)
                val n = Json.str(m["name"]); if (n.isEmpty()) continue
                board.add(Entry(k, n, Json.int(m["level"]), Json.int(m["best"]), Json.int(m["wins"]), Json.int(m["crowns"])))
            }
            boardLoaded = true
            listener?.netChanged()
        }
    }

    // ------------------------------------------------------------------ Freunde

    class Friend(val pid: String, var name: String, var level: Int, var seen: Long)
    val friends = ArrayList<Friend>()
    var serverNow = 0L; private set

    fun loadFriends() {
        if (!loggedIn) return
        get("friends/$pid") { v ->
            val ids = Json.map(v).keys.toList()
            get("players") { pl ->
                val all = Json.map(pl)
                friends.clear()
                var maxSeen = 0L
                for ((_, e) in all) maxSeen = maxOf(maxSeen, Json.long(Json.map(e)["seen"]))
                serverNow = maxSeen
                for (id in ids) {
                    val m = Json.map(all[id])
                    friends.add(Friend(id, Json.str(m["name"]).ifEmpty { "?" }, Json.int(m["level"]), Json.long(m["seen"])))
                }
                listener?.netChanged()
            }
        }
    }

    fun addFriend(n: String) {
        if (!loggedIn) return
        get("users/${keyOf(n)}") { v ->
            val m = Json.map(v)
            val fp = Json.str(m["pid"])
            if (fp.isEmpty()) { listener?.netToast(L.t(L.NET_NOT_FOUND)); return@get }
            if (fp == pid) return@get
            put("friends/$pid/$fp", "true")
            put("friends/$fp/$pid", "true")
            listener?.netToast(L.f(L.NET_FRIEND_ADDED, Json.str(m["name"])))
            loadFriends()
        }
    }

    fun removeFriend(fp: String) {
        del("friends/$pid/$fp"); del("friends/$fp/$pid")
        friends.removeAll { it.pid == fp }
        listener?.netChanged()
    }

    // ------------------------------------------------------------------ Duell-Einladung

    var inviteMid = ""; private set
    private var inviteT = 0f

    fun invite(f: Friend) {
        if (!loggedIn) return
        val mid = pid + "_" + randId(6)
        val seed = Random.nextLong(1, 1_000_000_000L)
        put("matches/$mid", Json.obj("a" to pid, "b" to f.pid, "an" to name, "bn" to f.name, "seed" to seed, "go" to false))
        put("invites/${f.pid}/$pid", "{\"name\":${Json.q(name)},\"mid\":${Json.q(mid)},\"t\":$SV}")
        inviteMid = mid; inviteOpp = f.name; inviteSeed = seed; inviteT = 0f
    }
    private var inviteOpp = ""
    private var inviteSeed = 0L

    fun cancelInvite() {
        if (inviteMid.isEmpty()) return
        del("matches/$inviteMid"); inviteMid = ""
    }

    fun acceptInvite(fromPid: String, mid: String) {
        del("invites/$pid/$fromPid")
        get("matches/$mid") { v ->
            val m = Json.map(v)
            if (m.isEmpty()) { listener?.netToast(L.t(L.NET_GONE)); return@get }
            patch("matches/$mid", Json.obj("go" to true))
            listener?.netMatch(mid, "b", Json.long(m["seed"]), Json.str(m["an"]))
        }
    }

    fun declineInvite(fromPid: String) = del("invites/$pid/$fromPid")

    private val seenInvites = HashSet<String>()
    private fun pollInvites() {
        get("invites/$pid") { v ->
            var newest = 0L
            for ((_, e) in Json.map(v)) newest = maxOf(newest, Json.long(Json.map(e)["t"]))
            for ((from, e) in Json.map(v)) {
                val m = Json.map(e)
                val mid = Json.str(m["mid"])
                if (Json.long(m["t"]) < newest - 120_000L) { del("invites/$pid/$from"); continue }
                if (seenInvites.add(mid)) listener?.netInvite(from, Json.str(m["name"]), mid)
            }
        }
    }

    // ------------------------------------------------------------------ 1v1-Warteschlange

    var inQueue = false; private set
    var queueTime = 0f; private set

    fun joinQueue() {
        if (!loggedIn || inQueue) return
        inQueue = true; queueTime = 0f; beat = 0f
        put("queue/$pid", "{\"name\":${Json.q(name)},\"t\":$SV}")
    }

    fun leaveQueue() {
        if (!inQueue) return
        inQueue = false
        del("queue/$pid")
    }

    private var beat = 0f
    private var pollT = 0f
    private var invPollT = 3f
    private var seenT = 0f
    private var matchBusy = false

    private fun pollQueue() {
        if (matchBusy) return
        get("queue") { v ->
            if (!inQueue) return@get
            val q = Json.map(v)
            val me = Json.map(q[pid])
            val mine = Json.str(me["match"])
            if (mine.isNotEmpty()) { startFromQueue(mine); return@get }
            var newest = 0L
            for ((_, e) in q) newest = maxOf(newest, Json.long(Json.map(e)["t"]))
            // ältester anderer wartender Spieler
            var best: String? = null; var bestT = Long.MAX_VALUE; var bestName = ""
            for ((k, e) in q) {
                if (k == pid) continue
                val m = Json.map(e)
                if (Json.str(m["match"]).isNotEmpty()) continue
                val t = Json.long(m["t"])
                if (t < newest - 15_000L) continue
                if (t < bestT) { bestT = t; best = k; bestName = Json.str(m["name"]) }
            }
            val other = best ?: return@get
            val myT = Json.long(me["t"])
            // Der Neuere erstellt das Spiel
            if (myT > bestT || (myT == bestT && pid > other)) {
                matchBusy = true
                val mid = other + "_" + pid
                val seed = Random.nextLong(1, 1_000_000_000L)
                put("matches/$mid", Json.obj("a" to other, "b" to pid, "an" to bestName, "bn" to name, "seed" to seed, "go" to true)) { ok ->
                    if (!ok) { matchBusy = false; return@put }
                    patch("queue/$other", Json.obj("match" to mid)) {
                        matchBusy = false
                        inQueue = false
                        del("queue/$pid")
                        listener?.netMatch(mid, "b", seed, bestName)
                    }
                }
            }
        }
    }

    private fun startFromQueue(mid: String) {
        matchBusy = true
        get("matches/$mid") { v ->
            matchBusy = false
            val m = Json.map(v)
            inQueue = false
            del("queue/$pid")
            if (m.isEmpty()) { listener?.netToast(L.t(L.NET_GONE)); return@get }
            listener?.netMatch(mid, "a", Json.long(m["seed"]), Json.str(m["bn"]))
        }
    }

    // ------------------------------------------------------------------ Laufendes Duell

    var mid = ""; private set
    var role = ""; private set
    var oppName = ""; private set
    var oppScore = 0; private set
    var oppDead = false; private set
    var oppSeenAge = 0f; private set
    private var oppLastT = -1L
    private var myScore = 0
    private var myDead = false
    private var syncT = 0f

    fun startMatch(m: String, r: String, opp: String) {
        mid = m; role = r; oppName = opp
        oppScore = 0; oppDead = false; oppSeenAge = 0f; oppLastT = -1L
        myScore = 0; myDead = false; syncT = 0f
        inviteMid = ""
    }

    fun endMatch() { mid = "" }

    fun report(score: Int, dead: Boolean) {
        myScore = score
        if (dead && !myDead) { myDead = true; syncT = 99f }
    }

    private fun syncMatch() {
        val other = if (role == "a") "b" else "a"
        val m = mid
        patch("matches/$m/$role", "{\"s\":$myScore,\"d\":$myDead,\"t\":$SV}")
        get("matches/$m/$other") { v ->
            if (mid != m) return@get
            val o = Json.map(v)
            if (o.isEmpty()) return@get
            oppScore = Json.int(o["s"]); oppDead = o["d"] == true
            val t = Json.long(o["t"])
            if (t != oppLastT) { oppLastT = t; oppSeenAge = 0f }
        }
    }

    // ------------------------------------------------------------------ Takt

    fun update(dt: Float, watchInvites: Boolean) {
        if (!loggedIn) return
        if (inQueue) {
            queueTime += dt
            beat += dt; pollT += dt
            if (beat > 6f) { beat = 0f; patch("queue/$pid", "{\"t\":$SV}") }
            if (pollT > 1.5f) { pollT = 0f; pollQueue() }
        }
        if (inviteMid.isNotEmpty()) {
            inviteT += dt; pollT += dt
            if (inviteT > 90f) { listener?.netToast(L.t(L.NET_NO_ANSWER)); cancelInvite() }
            else if (pollT > 1.5f) {
                pollT = 0f
                val im = inviteMid
                get("matches/$im/go") { v ->
                    if (v == true && inviteMid == im) listener?.netMatch(im, "a", inviteSeed, inviteOpp)
                }
            }
        }
        if (mid.isNotEmpty()) {
            oppSeenAge += dt
            syncT += dt
            if (syncT > 1f) { syncT = 0f; syncMatch() }
        }
        if (watchInvites && mid.isEmpty()) {
            invPollT += dt
            if (invPollT > 5f) { invPollT = 0f; pollInvites() }
        }
        seenT += dt
        if (seenT > 60f) { seenT = 0f; patch("players/$pid", "{\"seen\":$SV}") }
    }

    fun result(won: Boolean, draw: Boolean) {
        if (!loggedIn) return
        val f = if (draw) "draws" else if (won) "wins" else "losses"
        get("players/$pid/$f") { v -> put("players/$pid/$f", (Json.int(v) + 1).toString()) }
    }
}
