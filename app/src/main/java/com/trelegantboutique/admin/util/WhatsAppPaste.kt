package com.trelegantboutique.admin.util

/**
 * Picks the customer's name, mobile number and address out of a pasted
 * WhatsApp message. The same rules as "Paste from WhatsApp" on the website's
 * New sale page (admin/new-sale.php), plus a guess of the city.
 */
object WhatsAppPaste {

    class Result(val name: String, val phone: String, val address: String, val city: String)

    // 9 to 12 digits, with at most a single space or dash between digits.
    private val PHONE_CANDIDATE = Regex("[+]?\\d(?:[ \\-]?\\d){7,}")
    private val LABEL_NAME = Regex("^(?:name|customer|full name)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val LABEL_PHONE = Regex("^(?:phone|mobile|tel|contact|number|no)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val LABEL_ADDRESS = Regex("^(?:address|addr|location|deliver(?:y)? to)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val LABEL_CITY = Regex("^(?:city|town)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
    private val ADDRESS_HINT = Regex(
        "\\d|road|street|lane|mawatha|junction|town|city|no\\.|colombo|kandy|jaffna|galle|negombo|matara|kurunegala|gampaha",
        RegexOption.IGNORE_CASE
    )
    private val GREETING = Regex("^(?:hi,?|hello,?|hey,?)?\\s*(?:this is|i'?m|i am|my name is)\\s+", RegexOption.IGNORE_CASE)
    private val SEGMENT_SPLIT = Regex("\\s*[,;|]\\s*|\\s+-\\s+")
    private val EDGE_PUNCT = Regex("^[\\s,;|-]+|[\\s,;|-]+$")

    /** Towns customers often write; the last one found in the address becomes the City. */
    private val CITIES = listOf(
        "Colombo", "Dehiwala", "Mount Lavinia", "Moratuwa", "Nugegoda", "Maharagama", "Kotte", "Kaduwela", "Malabe", "Battaramulla",
        "Wattala", "Ja-Ela", "Ja Ela", "Negombo", "Gampaha", "Kadawatha", "Kelaniya", "Ragama", "Minuwangoda", "Kandana",
        "Kalutara", "Panadura", "Horana", "Beruwala", "Aluthgama", "Kandy", "Peradeniya", "Katugastota", "Gampola", "Nawalapitiya",
        "Matale", "Dambulla", "Nuwara Eliya", "Hatton", "Galle", "Hikkaduwa", "Ambalangoda", "Matara", "Weligama", "Tangalle",
        "Hambantota", "Tissamaharama", "Jaffna", "Chavakachcheri", "Point Pedro", "Kilinochchi", "Mullaitivu", "Mannar", "Vavuniya",
        "Trincomalee", "Batticaloa", "Kalmunai", "Ampara", "Akkaraipattu", "Kurunegala", "Kuliyapitiya", "Puttalam", "Chilaw",
        "Anuradhapura", "Polonnaruwa", "Badulla", "Bandarawela", "Ella", "Monaragala", "Ratnapura", "Embilipitiya", "Kegalle",
        "Avissawella", "Homagama", "Piliyandala", "Kesbewa", "Nittambuwa", "Veyangoda", "Kegalla", "Valvettithurai", "Nallur",
        "Kokuvil", "Chunnakam", "Tellippalai", "Manipay"
    )

    private fun clean(s: String) = s.replace(EDGE_PUNCT, "").trim()

    private fun findPhone(line: String): String? {
        for (m in PHONE_CANDIDATE.findAll(line)) {
            val digits = m.value.filter { it.isDigit() }
            if (digits.length in 9..12) return m.value.trim()
        }
        return null
    }

    fun parse(text: String): Result {
        val lines = text.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        var name = ""
        var phone = ""
        var city = ""
        val address = ArrayList<String>()
        val used = HashSet<Int>()

        fun classify(partIn: String) {
            val part = clean(partIn)
            if (part.isEmpty()) return
            if (name.isEmpty() && !ADDRESS_HINT.containsMatchIn(part) && part.split(Regex("\\s+")).size <= 5) {
                name = part
            } else {
                address.add(part)
            }
        }

        // Pass 1: "Label: value" lines first.
        lines.forEachIndexed { i, line ->
            var m = LABEL_NAME.find(line)
            if (name.isEmpty() && m != null) { name = m.groupValues[1].trim(); used.add(i); return@forEachIndexed }
            m = LABEL_PHONE.find(line)
            if (phone.isEmpty() && m != null) { phone = m.groupValues[1].trim(); used.add(i); return@forEachIndexed }
            m = LABEL_CITY.find(line)
            if (city.isEmpty() && m != null) { city = m.groupValues[1].trim(); used.add(i); return@forEachIndexed }
            m = LABEL_ADDRESS.find(line)
            if (m != null) { address.add(m.groupValues[1].trim()); used.add(i) }
        }

        // Pass 2: a phone number anywhere.
        if (phone.isEmpty()) {
            for (i in lines.indices) {
                if (used.contains(i)) continue
                val found = findPhone(lines[i])
                if (found != null) {
                    phone = found
                    if (lines[i] == found) used.add(i)
                    break
                }
            }
        }

        // Pass 3: name and address from the rest.
        lines.forEachIndexed { i, line ->
            if (used.contains(i)) return@forEachIndexed
            val withoutPhone = clean(line.replace(PHONE_CANDIDATE, ""))
            if (withoutPhone.isEmpty()) return@forEachIndexed
            val greeted = withoutPhone.replace(GREETING, "").trim()
            if (greeted != withoutPhone) { classify(greeted); return@forEachIndexed }
            for (part in withoutPhone.split(SEGMENT_SPLIT)) classify(part)
        }

        val addr = address.joinToString(", ")
        if (city.isEmpty()) {
            var bestPos = -1
            for (c in CITIES) {
                val pos = addr.lowercase().lastIndexOf(c.lowercase())
                if (pos > bestPos) { bestPos = pos; city = c }
            }
            if (bestPos < 0) city = ""
        }
        return Result(name, phone, addr, city)
    }
}
