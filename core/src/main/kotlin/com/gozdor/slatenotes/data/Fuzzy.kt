package com.gozdor.slatenotes.data

object Fuzzy {
    /**
     * Lower score = better match; null = no match. Both strings should already be lowercase.
     * Exact > prefix > substring > every character in order (subsequence).
     */
    fun score(query: String, text: String): Int? {
        if (query.isEmpty()) return 0
        if (text == query) return 0
        if (text.startsWith(query)) return 1
        val i = text.indexOf(query)
        if (i >= 0) return 2 + i
        var ti = 0
        var gaps = 0
        for (ch in query) {
            val found = text.indexOf(ch, ti)
            if (found < 0) return null
            gaps += found - ti
            ti = found + 1
        }
        return 100 + gaps
    }
}
