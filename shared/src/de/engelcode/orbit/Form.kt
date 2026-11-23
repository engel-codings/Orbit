package de.engelcode.orbit

/** Eingabefeld für die modernen Formulare (Anmelden, Registrieren, Name, Chat …). */
class FormField(val key: String, val hint: String, val type: Int = TEXT, val value: String = "", val max: Int = 40) {
    companion object {
        const val TEXT = 0
        const val EMAIL = 1
        const val PASSWORD = 2
    }
}

/** Beschreibung eines Formulars; wird von Android (eigener Dialog) und Web (HTML) gleich dargestellt. */
class FormSpec(
    val title: String,
    val subtitle: String,
    val fields: List<FormField>,
    val submit: String,
    val links: List<Pair<String, String>> = emptyList(),   // Aktion -> Text
    val google: String? = null,                             // Text des Google-Knopfs oder null
    val divider: String = ""
)
