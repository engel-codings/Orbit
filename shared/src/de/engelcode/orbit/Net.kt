package de.engelcode.orbit

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Online-Teil: Konto (Login/Registrieren), Cloud-Speicher, Bestenliste, Freunde,
 * 1v1-Warteschlange und Duell-Einladungen. Alles über die Firebase-Datenbank (REST).
 * iPhone (Web) und Android benutzen dieselbe Datenbank.
 */
object Net {
    const val BASE = "https://orbit-game-8f095-default-rtdb.europe-west1.firebasedatabase.app"
    const val API_KEY = "AIzaSyDehLuW3rirZSso_FjJzf6P1vxOjFIoGRs"
    private const val SV = "{\".sv\":\"timestamp\"}"

    interface Listener {
        fun netToast(msg: String)
        fun netMatch(mid: String, role: String, seed: Long, opp: String)
        fun netInvite(fromPid: String, name: String, mid: String)
        fun netCloudLoaded(data: String)
        fun netChanged()
        fun netProfile(): String   // JSON-Teil mit best/level/crowns
        fun netExport(): String
        fun netAvatar(): Int
        fun netPhotoPublic(): Boolean
    }

    var listener: Listener? = null

    // Konto (lokal gemerkt). pid = Firebase-Benutzer-ID
    var pid = ""; private set
    var name = ""; private set
    var email = ""; private set
    private var key = ""
    private var refresh = ""
    private var token = ""
    private var tokenExp = 0L
    private var refreshing = false
    var guest = false; private set
    /** Spieler hat beim Start „Als Gast spielen“ gewählt. */
    var guestChosen = false; private set
    /** Hat eine Online-ID (Gast oder Konto) – erscheint in der Bestenliste. */
    val active get() = pid.isNotEmpty() && token.isNotEmpty()
    /** Richtiges Konto mit E-Mail. */
    val loggedIn get() = pid.isNotEmpty() && !guest
    /** Start-Auswahl erledigt (Gast gewählt oder angemeldet). */
    val chosen get() = loggedIn || guestChosen
    private var store: ((String, String) -> Unit)? = null

    private var getter: ((String) -> String)? = null

    fun init(get: (String) -> String, set: (String, String) -> Unit) {
        store = set; getter = get
        refresh = get("rt")
        if (refresh.isNotEmpty()) { pid = get("pid"); name = get("name"); key = get("key"); email = get("email"); guest = get("guest") == "1" }
        else if (get("pid").isNotEmpty()) { set("pid", ""); set("name", ""); set("key", "") } // altes Konto (vor 0.9.7)
        guestChosen = get("gc") == "1"
    }

    private fun remember() {
        val st = store ?: return
        st("pid", pid); st("name", name); st("key", key); st("rt", refresh); st("email", email); st("guest", if (guest) "1" else ""); st("gc", if (guestChosen) "1" else "")
    }

    // ------------------------------------------------------------------ REST

    private fun ok(r: String?) = r != null && !r.startsWith("\u0000")
    private fun url(path: String) = "$BASE/$path.json" + if (token.isNotEmpty()) "?auth=$token" else ""
    private fun get(path: String, cb: (Any?) -> Unit) = Platform.http("GET", url(path), null) { cb(if (ok(it)) Json.parse(it!!) else null) }
    private fun put(path: String, body: String, cb: (Boolean) -> Unit = {}) = Platform.http("PUT", url(path), body) { cb(ok(it)) }
    private fun patch(path: String, body: String, cb: (Boolean) -> Unit = {}) = Platform.http("PATCH", url(path), body) { cb(ok(it)) }
    private fun del(path: String) = Platform.http("DELETE", url(path), null) {}

    private fun auth(op: String, body: String, cb: (Map<String, Any?>?, String) -> Unit) {
        Platform.http("POST", "https://identitytoolkit.googleapis.com/v1/accounts:$op?key=$API_KEY", body) { r ->
            if (r == null) { cb(null, "OFFLINE"); return@http }
            if (r.startsWith("\u0000")) {
                val msg = Json.str(Json.map(Json.map(Json.parse(r.substring(1)))["error"])["message"])
                cb(null, msg.ifEmpty { "OFFLINE" }); return@http
            }
            cb(Json.map(Json.parse(r)), "")
        }
    }

    private fun takeTokens(m: Map<String, Any?>) {
        token = Json.str(m["idToken"] ?: m["id_token"])
        val rt = Json.str(m["refreshToken"] ?: m["refresh_token"]); if (rt.isNotEmpty()) refresh = rt
        val exp = Json.str(m["expiresIn"] ?: m["expires_in"]).toLongOrNull() ?: 3600L
        tokenExp = Platform.now() + exp * 1000L
    }

    private fun refreshToken() {
        if (refreshing || refresh.isEmpty()) return
        refreshing = true
        Platform.http("POST", "https://securetoken.googleapis.com/v1/token?key=$API_KEY",
            "{\"grant_type\":\"refresh_token\",\"refresh_token\":${Json.q(refresh)}}") { r ->
            refreshing = false
            if (!ok(r)) {
                if (r != null && (r.contains("TOKEN_EXPIRED") || r.contains("USER_DISABLED") || r.contains("USER_NOT_FOUND") || r.contains("INVALID_REFRESH_TOKEN"))) logout()
                return@http
            }
            takeTokens(Json.map(Json.parse(r!!)))
            remember()
            pushProfile()
            if (myPhoto.isEmpty()) loadMyPhoto()
        }
    }

    private fun authError(code: String) {
        val t = when {
            code.startsWith("EMAIL_EXISTS") -> L.NET_EMAIL_TAKEN
            code.startsWith("INVALID_EMAIL") -> L.NET_BAD_EMAIL
            code.startsWith("WEAK_PASSWORD") -> L.NET_BAD_PW
            code.startsWith("EMAIL_NOT_FOUND") || code.startsWith("INVALID_PASSWORD") || code.startsWith("INVALID_LOGIN_CREDENTIALS") -> L.NET_WRONG
            code.startsWith("TOO_MANY_ATTEMPTS") -> L.NET_TOO_MANY
            else -> L.NET_OFFLINE
        }
        listener?.netToast(L.t(t))
    }

    private fun keyOf(n: String) = n.trim().lowercase().replace(Regex("[.#$\\[\\]/]"), "_")
    private fun randId(n: Int): String {
        val ch = "abcdefghijkmnpqrstuvwxyz23456789"
        return (1..n).map { ch[Random.nextInt(ch.length)] }.joinToString("")
    }

    fun validName(n: String) = n.trim().length in 3..16 && n.trim().all { it.isLetterOrDigit() || it == '_' || it == '-' || it == ' ' }

    // ------------------------------------------------------------------ Konto

    var busy = false; private set

    fun register(mail: String, n: String, pw: String, done: (Boolean) -> Unit) {
        val k = keyOf(n)
        busy = true
        get("names/$k") { v ->
            if (v != null && Json.str(v) != pid) { busy = false; listener?.netToast(L.t(L.NET_NAME_TAKEN)); done(false); return@get }
            if (guest && active) {
                // Gast-Konto einmalig übernehmen: gleiche ID, Fortschritt und Platz bleiben
                auth("update", Json.obj("idToken" to token, "email" to mail.trim(), "password" to pw, "returnSecureToken" to true)) { m, err ->
                    if (m == null) { busy = false; authError(err); done(false); return@auth }
                    takeTokens(m)
                    guest = false; email = mail.trim()
                    forgetGuest()
                    remember()
                    busy = false
                    rename(n) { }
                    pushSave()
                    listener?.netChanged()
                    done(true)
                }
                return@get
            }
            auth("signUp", Json.obj("email" to mail.trim(), "password" to pw, "returnSecureToken" to true)) { m, err ->
                if (m == null) { busy = false; authError(err); done(false); return@auth }
                takeTokens(m)
                pid = Json.str(m["localId"]); email = mail.trim(); guest = false
                put("names/$k", Json.q(pid)) { okName ->
                    busy = false
                    name = n.trim(); key = if (okName) k else ""
                    if (!okName) name = n.trim() + "_" + randId(3)
                    remember()
                    pushProfile(); pushSave()
                    listener?.netChanged()
                    done(true)
                }
            }
        }
    }

    fun login(mail: String, pw: String, done: (Boolean) -> Unit) {
        busy = true
        auth("signInWithPassword", Json.obj("email" to mail.trim(), "password" to pw, "returnSecureToken" to true)) { m, err ->
            if (m == null) { busy = false; authError(err); done(false); return@auth }
            if (guest && active) dropGuest()
            takeTokens(m)
            pid = Json.str(m["localId"]); email = mail.trim(); guest = false
            get("players/$pid") { pv ->
                val pm = Json.map(pv)
                name = Json.str(pm["name"]).ifEmpty { mail.substringBefore('@') }
                key = Json.str(pm["key"])
                remember()
                get("saves/$pid") { sv ->
                    busy = false
                    val data = Json.str(Json.map(sv)["d"])
                    listener?.netChanged()
                    done(true)
                    if (data.isNotEmpty()) listener?.netCloudLoaded(data) else { pushProfile(); pushSave() }
                }
            }
        }
    }

    /** Prüft automatisch, ob es zu der E-Mail schon ein Konto gibt. */
    fun checkEmail(mail: String, cb: (Boolean) -> Unit) {
        auth("createAuthUri", Json.obj("identifier" to mail.trim(), "continueUri" to "https://engel-codings.github.io/Orbit/")) { m, _ ->
            cb(m != null && m["registered"] == true)
        }
    }

    fun resetPassword(mail: String) {
        auth("sendOobCode", Json.obj("requestType" to "PASSWORD_RESET", "email" to mail.trim())) { m, err ->
            if (m == null && !err.startsWith("EMAIL_NOT_FOUND")) { authError(err); return@auth }
            listener?.netToast(L.t(L.NET_RESET_SENT))
        }
    }

    /** Gast-Eintrag aus der Bestenliste entfernen (beim Anmelden mit einem bestehenden Konto). */
    private fun forgetGuest() { store?.invoke("g_rt", ""); store?.invoke("g_pid", "") }

    private fun dropGuest(t: String = token, p: String = pid, k: String = key) {
        forgetGuest()
        Platform.http("DELETE", "$BASE/avatars/$p.json?auth=$t", null) {}
        Platform.http("DELETE", "$BASE/players/$p.json?auth=$t", null) {}
        Platform.http("DELETE", "$BASE/saves/$p.json?auth=$t", null) {}
        if (k.isNotEmpty()) Platform.http("DELETE", "$BASE/names/$k.json?auth=$t", null) {}
        Platform.http("POST", "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=$API_KEY", Json.obj("idToken" to t)) {}
    }

    private var guestBusy = false
    private var guestRetry = 0f

    fun chooseGuest() {
        guestChosen = true
        // Immer dasselbe Gast-Konto auf diesem Gerät wiederverwenden
        val g = getter
        if (g != null && pid.isEmpty() && g("g_rt").isNotEmpty()) {
            refresh = g("g_rt"); pid = g("g_pid"); name = g("g_name"); key = g("g_key"); guest = true
            token = ""; tokenExp = 0L
        }
        remember()
        guestRetry = 0f
    }

    /** Gast-ID erstellen: zufälliger Name, damit man sofort in der Bestenliste steht. */
    private fun createGuest() {
        if (guestBusy) return
        guestBusy = true
        auth("signUp", "{\"returnSecureToken\":true}") { m, _ ->
            if (m == null) { guestBusy = false; guestRetry = 30f; return@auth }
            takeTokens(m)
            pid = Json.str(m["localId"]); guest = true; email = ""
            val n = "Orbit-user-" + (10000 + Random.nextInt(90000))
            val k = keyOf(n)
            put("names/$k", Json.q(pid)) { okN ->
                guestBusy = false
                name = n; key = if (okN) k else ""
                remember()
                store?.invoke("g_rt", refresh); store?.invoke("g_pid", pid); store?.invoke("g_name", name); store?.invoke("g_key", key)
                pushProfile()
                listener?.netChanged()
            }
        }
    }

    fun logout() {
        if (inQueue) leaveQueue()
        pid = ""; name = ""; key = ""; email = ""; refresh = ""; token = ""; tokenExp = 0L; guest = false; guestChosen = false
        remember()
        friends.clear()
        listener?.netChanged()
    }

    fun rename(n: String, done: (Boolean) -> Unit) {
        if (!active) return
        val nk = keyOf(n)
        if (nk == key) {
            name = n.trim(); remember(); patch("players/$pid", Json.obj("name" to name))
            listener?.netChanged(); done(true); return
        }
        busy = true
        get("names/$nk") { v ->
            if (v != null) { busy = false; listener?.netToast(L.t(L.NET_NAME_TAKEN)); done(false); return@get }
            put("names/$nk", Json.q(pid)) { okP ->
                busy = false
                if (!okP) { listener?.netToast(L.t(L.NET_OFFLINE)); done(false); return@put }
                if (key.isNotEmpty()) del("names/$key")
                key = nk; name = n.trim(); remember()
                patch("players/$pid", Json.obj("name" to name, "key" to key))
                listener?.netChanged()
                done(true)
            }
        }
    }

    // ------------------------------------------------------------------ Google

    fun googleLogin(rt: String, done: (Boolean) -> Unit) {
        busy = true
        val oldT = token; val oldP = pid; val oldK = key; val oldName = name
        val wasGuest = guest && active
        Platform.http("POST", "https://securetoken.googleapis.com/v1/token?key=$API_KEY",
            "{\"grant_type\":\"refresh_token\",\"refresh_token\":${Json.q(rt)}}") { r ->
            if (!ok(r)) { busy = false; listener?.netToast(L.t(L.NET_OFFLINE)); done(false); return@http }
            val m = Json.map(Json.parse(r!!))
            takeTokens(m)
            pid = Json.str(m["user_id"]); guest = false; email = "Google"
            get("players/$pid") { pv ->
                val pm = Json.map(pv)
                if (pm.isNotEmpty()) {
                    // bestehendes Konto
                    name = Json.str(pm["name"]); key = Json.str(pm["key"])
                    remember()
                    if (wasGuest) dropGuest(oldT, oldP, oldK)
                    get("saves/$pid") { sv ->
                        busy = false
                        val data = Json.str(Json.map(sv)["d"])
                        listener?.netChanged(); done(true)
                        if (data.isNotEmpty()) listener?.netCloudLoaded(data) else { pushProfile(); pushSave() }
                    }
                } else {
                    // neues Konto: Gast-Fortschritt und Name einmalig übernehmen
                    val n = if (wasGuest) oldName else "Orbit-user-" + (10000 + Random.nextInt(90000))
                    val k = keyOf(n)
                    val claim = {
                        put("names/$k", Json.q(pid)) { okN ->
                            busy = false
                            name = n; key = if (okN) k else ""
                            remember(); pushProfile(); pushSave()
                            if (myPhoto.isNotEmpty()) setPhoto(myPhoto)
                            listener?.netChanged(); done(true)
                        }
                    }
                    if (wasGuest && oldK.isNotEmpty()) {
                        Platform.http("DELETE", "$BASE/names/$oldK.json?auth=$oldT", null) { claim(); dropGuest(oldT, oldP, "") }
                    } else { if (wasGuest) dropGuest(oldT, oldP, oldK); claim() }
                }
            }
        }
    }

    // ------------------------------------------------------------------ Avatar & Foto

    var myPhoto = ""; private set

    fun setPhoto(b64: String) {
        if (!active) return
        myPhoto = b64
        put("avatars/$pid", "{\"d\":${Json.q(b64)},\"pub\":${listener?.netPhotoPublic() ?: false}}")
        pushProfile()
    }

    fun setPhotoPublic(pub: Boolean) {
        if (!active || myPhoto.isEmpty()) { pushProfile(); return }
        patch("avatars/$pid", "{\"pub\":$pub}") { pushProfile() }
    }

    fun removePhoto() {
        myPhoto = ""
        if (active) del("avatars/$pid")
        pushProfile()
    }

    fun loadMyPhoto() {
        if (!active) return
        get("avatars/$pid/d") { v -> myPhoto = Json.str(v); listener?.netChanged() }
    }

    fun loadPhoto(uid: String, cb: (String) -> Unit) = get("avatars/$uid/d") { v -> cb(Json.str(v)) }

    val hasPhoto get() = myPhoto.isNotEmpty()

    // ------------------------------------------------------------------ Chat

    class Msg(val id: String, val uid: String, val name: String, val av: Int, val ph: Boolean, val text: String, val t: Long, val img: String = "")
    val chat = ArrayList<Msg>()
    var chatLoaded = false; private set
    private var lastSend = 0L

    fun loadChat() {
        if (!active) return
        Platform.http("GET", "$BASE/chat.json?orderBy=%22%24key%22&limitToLast=60&auth=$token", null) { r ->
            if (!ok(r)) return@http
            val list = ArrayList<Msg>()
            for ((k, e) in Json.map(Json.parse(r!!))) {
                val m = Json.map(e)
                list.add(Msg(k, Json.str(m["uid"]), Json.str(m["name"]), Json.int(m["av"]), m["ph"] == true, Json.str(m["text"]), Json.long(m["t"])))
            }
            list.sortBy { it.id }
            chat.clear(); chat.addAll(list)
            chatLoaded = true
            listener?.netChanged()
        }
    }

    fun sendChat(text: String) {
        val t = text.trim().take(200)
        if (!active || t.isEmpty()) return
        val now = Platform.now()
        if (now - lastSend < 2500L) { listener?.netToast(L.t(L.CHAT_SLOW)); return }
        lastSend = now
        val av = listener?.netAvatar() ?: 0
        val ph = hasPhoto && (listener?.netPhotoPublic() ?: false)
        Platform.http("POST", url("chat"), "{\"uid\":${Json.q(pid)},\"name\":${Json.q(name)},\"av\":$av,\"ph\":$ph,\"text\":${Json.q(t)},\"t\":$SV}") { loadChat() }
    }

    fun inviteAny(uid: String, n: String) = invite(Friend(uid, n, 0, 0L))

    // ------------------------------------------------------------------ Freundescode & Suche

    /** 6-stelliger Freundescode aus der Spieler-ID. */
    val myCode get() = codeOf(pid)
    fun codeOf(uid: String): String {
        var h = 7L
        for (ch in uid) h = (h * 31 + ch.code) and 0xFFFFFFFFL
        val al = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val sb = StringBuilder()
        var v = h
        repeat(6) { sb.append(al[(v % al.length).toInt()]); v /= al.length }
        return sb.toString()
    }

    fun pushCode() { if (active) put("codes/$myCode", Json.q(pid)) }

    /** Sucht per Benutzername oder Freundescode und fügt als Freund hinzu. */
    fun searchAdd(q: String) {
        if (!loggedIn) return
        val t = q.trim()
        get("names/${keyOf(t)}") { v ->
            val uid = Json.str(v)
            if (uid.isNotEmpty()) { addFriendId(uid, t); loadFriends(); return@get }
            get("codes/${t.uppercase().replace(" ", "")}") { c ->
                val u2 = Json.str(c)
                if (u2.isEmpty()) { listener?.netToast(L.t(L.NET_NOT_FOUND)); return@get }
                get("players/$u2/name") { n -> addFriendId(u2, Json.str(n).ifEmpty { t }); loadFriends() }
            }
        }
    }

    // ------------------------------------------------------------------ Clans

    var clanId = ""; private set
    var clanName = ""; private set
    var clanOwner = ""; private set
    val clanMembers = ArrayList<Pair<String, String>>()
    var clanLoaded = false; private set

    fun loadClan() {
        if (!active) return
        get("clanOf/$pid") { v ->
            val cid = Json.str(v)
            if (cid.isEmpty()) { clanId = ""; clanName = ""; clanMembers.clear(); clanLoaded = true; listener?.netChanged(); return@get }
            get("clans/$cid") { cv ->
                val m = Json.map(cv)
                if (m.isEmpty()) { del("clanOf/$pid"); clanId = ""; clanLoaded = true; listener?.netChanged(); return@get }
                clanId = cid; clanName = Json.str(m["name"]); clanOwner = Json.str(m["owner"])
                clanMembers.clear()
                for ((u, n) in Json.map(m["members"])) clanMembers.add(u to Json.str(n))
                clanMembers.sortBy { it.second.lowercase() }
                clanLoaded = true
                listener?.netChanged()
            }
        }
    }

    fun createClan(n: String) {
        if (!active) return
        val cid = randId(6).uppercase()
        put("clans/$cid", "{\"name\":${Json.q(n.trim().take(20))},\"owner\":${Json.q(pid)},\"members\":{${Json.q(pid)}:${Json.q(name)}}}") { ok ->
            if (!ok) { listener?.netToast(L.t(L.NET_OFFLINE)); return@put }
            put("clanOf/$pid", Json.q(cid)) { loadClan() }
            listener?.netToast(L.f(L.CLAN_CREATED, n.trim()))
        }
    }

    fun joinClan(code: String) {
        if (!active) return
        val cid = code.trim().uppercase()
        get("clans/$cid/name") { v ->
            if (Json.str(v).isEmpty()) { listener?.netToast(L.t(L.CLAN_NOT_FOUND)); return@get }
            if (clanId.isNotEmpty() && clanId != cid) del("clans/$clanId/members/$pid")
            put("clans/$cid/members/$pid", Json.q(name)) { put("clanOf/$pid", Json.q(cid)) { loadClan() } }
            listener?.netToast(L.f(L.CLAN_JOINED, Json.str(v)))
        }
    }

    /** Jedes Mitglied kann andere per Name oder Freundescode in den Clan holen. */
    fun addToClan(q: String) {
        if (!active || clanId.isEmpty()) return
        val t = q.trim(); val cid = clanId
        val add = { uid: String, n: String ->
            put("clans/$cid/members/$uid", Json.q(n)) { put("clanOf/$uid", Json.q(cid)) { loadClan() } }
            listener?.netToast(L.f(L.CLAN_ADDED, n))
        }
        get("names/${keyOf(t)}") { v ->
            val uid = Json.str(v)
            if (uid.isNotEmpty()) { get("players/$uid/name") { n -> add(uid, Json.str(n).ifEmpty { t }) }; return@get }
            get("codes/${t.uppercase().replace(" ", "")}") { c ->
                val u2 = Json.str(c)
                if (u2.isEmpty()) { listener?.netToast(L.t(L.NET_NOT_FOUND)); return@get }
                get("players/$u2/name") { n -> add(u2, Json.str(n).ifEmpty { t }) }
            }
        }
    }

    fun leaveClan() {
        if (clanId.isEmpty()) return
        del("clans/$clanId/members/$pid"); del("clanOf/$pid")
        listener?.netToast(L.f(L.CLAN_LEFT, clanName))
        clanId = ""; clanName = ""; clanMembers.clear()
        listener?.netChanged()
    }

    // ------------------------------------------------------------------ Private Nachrichten

    val dm = ArrayList<Msg>()
    var dmWith = ""; private set
    var dmName = ""; private set
    var dmLoaded = false; private set
    private fun pair(a: String, b: String) = if (a < b) a + "_" + b else b + "_" + a

    private var dmPath = ""
    val dmIsClan get() = dmPath.startsWith("clanchat/")

    fun openDm(uid: String, n: String) { dmWith = uid; dmName = n; dmPath = "dms/" + pair(pid, uid); dm.clear(); dmLoaded = false; loadDm() }

    fun openClanChat() {
        if (clanId.isEmpty()) return
        dmWith = clanId; dmName = clanName; dmPath = "clanchat/$clanId"; dm.clear(); dmLoaded = false; loadDm()
    }

    fun loadDm() {
        if (!active || dmWith.isEmpty()) return
        val w = dmWith
        Platform.http("GET", "$BASE/$dmPath.json?orderBy=%22%24key%22&limitToLast=60&auth=$token", null) { r ->
            if (!ok(r) || w != dmWith) return@http
            val list = ArrayList<Msg>()
            for ((k, e) in Json.map(Json.parse(r!!))) {
                val m = Json.map(e)
                val mine = Json.str(m["uid"]) == pid
                list.add(Msg(k, Json.str(m["uid"]), if (mine) name else Json.str(m["name"]).ifEmpty { dmName }, Json.int(m["av"]), m["ph"] == true,
                    Json.str(m["text"]), Json.long(m["t"]), Json.str(m["img"])))
            }
            list.sortBy { it.id }
            dm.clear(); dm.addAll(list); dmLoaded = true
            listener?.netChanged()
        }
    }

    fun sendDm(text: String, img: String = "") {
        if (!active || dmWith.isEmpty()) return
        val t = text.trim().take(300)
        if (t.isEmpty() && img.isEmpty()) return
        val av = listener?.netAvatar() ?: 0
        val ph = hasPhoto && (listener?.netPhotoPublic() ?: false)
        Platform.http("POST", url(dmPath),
            "{\"uid\":${Json.q(pid)},\"name\":${Json.q(name)},\"av\":$av,\"ph\":$ph,\"text\":${Json.q(t)},\"img\":${Json.q(img)},\"t\":$SV}") { loadDm() }
    }

    fun addFriendId(uid: String, n: String) {
        if (!loggedIn || uid == pid) return
        put("friends/$pid/$uid", "true")
        put("friends/$uid/$pid", "true")
        listener?.netToast(L.f(L.NET_FRIEND_ADDED, n))
    }

    // ------------------------------------------------------------------ Profil & Cloud

    fun pushProfile() {
        if (!active) return
        val extra = listener?.netProfile() ?: ""
        patch("players/$pid", "{\"name\":${Json.q(name)},\"key\":${Json.q(key)},$extra,\"seen\":$SV}")
        if (!codePushed) { codePushed = true; pushCode() }
    }

    var lastCloud = 0L; private set
    private var codePushed = false
    fun pushSave() {
        if (!active) return
        val d = listener?.netExport() ?: return
        put("saves/$pid", "{\"d\":${Json.q(d)},\"t\":$SV}") { if (it) lastCloud = Platform.now() }
    }

    // ------------------------------------------------------------------ Bestenliste

    class Entry(val pid: String, val name: String, val level: Int, val best: Int, val wins: Int, val crowns: Int,
                val av: Int, val ph: Boolean, val wk: Int, val wb: Int)

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
                board.add(Entry(k, n, Json.int(m["level"]), Json.int(m["best"]), Json.int(m["wins"]), Json.int(m["crowns"]),
                    Json.int(m["av"]), m["ph"] == true, Json.int(m["wk"]), Json.int(m["wb"])))
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
        get("names/${keyOf(n)}") { v ->
            val fp = Json.str(v)
            if (fp.isEmpty()) { listener?.netToast(L.t(L.NET_NOT_FOUND)); return@get }
            if (fp == pid) return@get
            put("friends/$pid/$fp", "true")
            put("friends/$fp/$pid", "true")
            listener?.netToast(L.f(L.NET_FRIEND_ADDED, n.trim()))
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
        if (!active) return
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
        if (!active || inQueue) return
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

    // Bot-Gegner, wenn niemand in der Warteschlange ist
    val isBot get() = mid == "bot"
    private var botT = 0f
    private var botNext = 0f
    private var botTarget = 0
    var botCaps = 0; private set

    private var botMin = 1.7f
    private var botRange = 1.8f
    private var botSpeed = 1f
    private var botBonus = 0.25f
    private var botSkip = 0
    private var botShield = false
    private var botBoostT = 0f
    private var botBoostLen = 0f
    private var botBoostNext = 0f
    /** Angezeigte Werte des Bots (auf den Spieler abgestimmt). */
    var botLevel = 0; private set
    var botSkills = 0; private set
    /** Kurzer Hinweis über dem Bot: 1 = Schild, 2 = Boost, 3 = Bonuspunkte. */
    var botTag = 0; private set
    var botTagVal = 0; private set
    var botTagT = 0f; private set
    val botBoosting get() = botBoostT > 0f

    private fun botTagSet(t: Int, v: Int = 0) { botTag = t; botTagVal = v; botTagT = 1.8f }

    /**
     * Stufe 0 = leicht, 1 = mittel, 2 = etwas schwerer (aber nie zu schwer).
     * Der Bot bekommt Fähigkeiten und Extras passend zum eigenen Stand:
     * gleiche Anzahl Fähigkeiten, gleiches Level, Tempo/Sterne/Schild/Boost wie der Spieler.
     */
    fun startBot(n: String, tier: Int, skills: Int = 0, level: Int = 0, best: Int = 0,
                 speed: Float = 0f, bonus: Float = 0f, shieldPct: Int = 0,
                 boostStart: Float = 0f, boostLen: Float = 0f, skip: Int = 0) {
        val power = 0.55f * min(skills, 37) / 37f + 0.45f * min(level, 600) / 600f
        var t = when (tier) { 0 -> 5 + Random.nextInt(8); 1 -> 10 + Random.nextInt(12); else -> 18 + Random.nextInt(14) } * (1f + 1.1f * power)
        if (best > 5) {
            val anchor = best * when (tier) { 0 -> 0.55f; 1 -> 0.8f; else -> 1.0f }
            t = min((t + anchor) / 2f, best * 1.1f + 3f)
        }
        botMin = when (tier) { 0 -> 2.1f; 1 -> 1.7f; else -> 1.35f }
        botRange = when (tier) { 0 -> 1.8f; 1 -> 1.6f; else -> 1.3f }
        botSpeed = 1f + min(speed, 0.25f) + 0.35f * power
        botBonus = min(0.25f + bonus, 0.6f)
        botSkip = skip
        botShield = shieldPct > 0 && Random.nextInt(100) < shieldPct
        botBoostLen = 2.5f + boostLen
        startMatch("bot", "a", n)
        botT = 0f; botNext = 2.5f; botTarget = max(5, t.toInt()); botCaps = 0
        botBoostNext = 12f + Random.nextFloat() * 8f
        botBoostT = boostStart
        botLevel = if (level <= 0) 1 else (level + Random.nextInt(-6, 7)).coerceIn(1, 600)
        botSkills = if (skills <= 0) 0 else (skills + Random.nextInt(-1, 2)).coerceIn(0, 37)
        botTag = 0; botTagT = 0f
        if (boostStart > 0f) botTagSet(2)
    }

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
        if (pid.isEmpty()) {
            if (!guestChosen) return
            guestRetry -= dt
            if (guestRetry <= 0f && !busy) createGuest()
            return
        }
        if (Platform.now() > tokenExp - 300_000L) { refreshToken(); if (token.isEmpty()) return }
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
        if (mid == "bot") {
            oppSeenAge = 0f
            botT += dt
            if (botTagT > 0f) botTagT -= dt
            if (botBoostT > 0f) botBoostT -= dt
            if (!oppDead && botT > botBoostNext) {
                botBoostT = botBoostLen
                botBoostNext = botT + 14f + Random.nextFloat() * 8f
                botTagSet(2)
            }
            if (!oppDead && botT > botNext) {
                botCaps++
                var pts = if (Random.nextFloat() < botBonus) 2 else 1
                if (botSkip > 0 && Random.nextFloat() < 0.12f) { pts += 2 + botSkip; botTagSet(3, pts) }
                oppScore += pts
                var pace = (botMin + Random.nextFloat() * botRange) / botSpeed
                if (botBoostT > 0f) pace *= 0.5f
                if (myDead) pace *= 0.3f   // Spieler ist raus: Bot spielt zügig zu Ende
                botNext = botT + pace
                if (oppScore >= botTarget) {
                    if (botShield) { botShield = false; botTarget += 3 + Random.nextInt(4); botTagSet(1) }
                    else oppDead = true
                }
            }
        } else if (mid.isNotEmpty()) {
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
        if (!active) return
        val f = if (draw) "draws" else if (won) "wins" else "losses"
        get("players/$pid/$f") { v -> put("players/$pid/$f", (Json.int(v) + 1).toString()) }
    }
}
