package com.trelegantboutique.admin.util

/**
 * Colour helpers shared by the product screens: the main colour of a photo,
 * a saree-friendly name for a colour (the same name list and Lab matching as
 * the website's colornamer.js), and hex conversions.
 */
object ColorTools {

    /** Name list copied from the website (assets/js/colornamer.js). */
    val NAMES: List<Pair<String, Int>> = listOf(
        "Black" to 0x000000,
        "Jet Black" to 0x0A0A0A,
        "Charcoal" to 0x36454F,
        "Gunmetal" to 0x2A3439,
        "Dim Grey" to 0x696969,
        "Grey" to 0x808080,
        "Ash Grey" to 0xB2BEB5,
        "Silver" to 0xC0C0C0,
        "Light Grey" to 0xD3D3D3,
        "Pearl Grey" to 0xE6E6E6,
        "White Smoke" to 0xF5F5F5,
        "White" to 0xFFFFFF,
        "Snow White" to 0xFFFAFA,
        "Ivory" to 0xFFFFF0,
        "Cream" to 0xFFFDD0,
        "Off White" to 0xFAF5EC,
        "Beige" to 0xF5F5DC,
        "Tan" to 0xD2B48C,
        "Khaki" to 0xC3B091,
        "Sand" to 0xC2B280,
        "Camel" to 0xC19A6B,
        "Fawn" to 0xE5AA70,
        "Taupe" to 0x8B7E74,
        "Warm Grey" to 0x98817B,
        "Grey Blue" to 0x7690A6,
        "Maroon" to 0x800000,
        "Deep Maroon" to 0x5C0A1E,
        "Wine" to 0x722F37,
        "Burgundy" to 0x6D071A,
        "Oxblood" to 0x4A1C1C,
        "Dark Red" to 0x8B0000,
        "Firebrick" to 0xB22222,
        "Brick Red" to 0xCB4154,
        "Blood Red" to 0x960018,
        "Red" to 0xE12B2B,
        "Crimson" to 0xDC143C,
        "Scarlet" to 0xFF2400,
        "Cherry Red" to 0xD2042D,
        "Chilli Red" to 0xC21807,
        "Tomato" to 0xFF6347,
        "Kumkum Red" to 0x9A2B2B,
        "Vermilion" to 0xE34234,
        "Rust Red" to 0xA53F2B,
        "Rust" to 0xB7410E,
        "Copper" to 0xB87333,
        "Copper Red" to 0xA0562B,
        "Terracotta" to 0xE2725B,
        "Sindoor Red" to 0xB32428,
        "Bridal Red" to 0x9E1B32,
        "Coral" to 0xFF7F50,
        "Coral Pink" to 0xF88379,
        "Salmon" to 0xFA8072,
        "Salmon Pink" to 0xFF91A4,
        "Peach" to 0xFFCBA4,
        "Peachy Pink" to 0xFFDAB9,
        "Rose" to 0xE28E9A,
        "Rose Pink" to 0xE77471,
        "Dusty Rose" to 0xC08081,
        "Blush" to 0xDE5D83,
        "Blush Pink" to 0xF4C2C2,
        "Baby Pink" to 0xF9C6D0,
        "Pink" to 0xFF8DA1,
        "Bubblegum Pink" to 0xFF9FBF,
        "Hot Pink" to 0xFF69B4,
        "Deep Pink" to 0xFF1493,
        "Fuchsia" to 0xFF00FF,
        "Magenta" to 0xC71585,
        "Rani Pink" to 0xD5257A,
        "Rani" to 0xC9184A,
        "Ruby" to 0x9B111E,
        "Ruby Red" to 0xAA1E3D,
        "Onion Pink" to 0xE79EA8,
        "Candy Pink" to 0xE4717A,
        "Berry Pink" to 0x98004F,
        "Mauve Pink" to 0xC67BA0,
        "Orchid Pink" to 0xDA70D6,
        "Watermelon Pink" to 0xFC6C85,
        "Flamingo Pink" to 0xFC8EAC,
        "Carnation Pink" to 0xFFA6C9,
        "Orange" to 0xFF8C1A,
        "Bright Orange" to 0xFF7518,
        "Dark Orange" to 0xFF8C00,
        "Burnt Orange" to 0xCC5500,
        "Pumpkin" to 0xFF7518,
        "Tangerine" to 0xF28500,
        "Apricot" to 0xFBCEB1,
        "Amber" to 0xFFBF00,
        "Saffron" to 0xF4C430,
        "Marigold" to 0xEAA221,
        "Turmeric" to 0xE3A622,
        "Honey" to 0xD9A441,
        "Mustard" to 0xC9A227,
        "Mustard Yellow" to 0xD8A11F,
        "Gold" to 0xD4AF37,
        "Antique Gold" to 0xB08D3F,
        "Golden Yellow" to 0xE8B800,
        "Golden" to 0xE6BE8A,
        "Champagne Gold" to 0xE8D3A2,
        "Yellow" to 0xFFD94A,
        "Lemon Yellow" to 0xF4E04D,
        "Butter Yellow" to 0xF3E5AB,
        "Cream Yellow" to 0xF3E5AB,
        "Banana Yellow" to 0xF7E07F,
        "Sunflower Yellow" to 0xF9C80E,
        "Canary Yellow" to 0xFFEF00,
        "Bronze" to 0x8C6A3F,
        "Copper Gold" to 0xC98A3E,
        "Olive" to 0x808000,
        "Olive Green" to 0x6B8E23,
        "Army Green" to 0x4B5320,
        "Lime" to 0x9ACD32,
        "Lime Green" to 0x8FCE00,
        "Chartreuse" to 0x7FFF00,
        "Apple Green" to 0x8DB600,
        "Mehendi Green" to 0x7D8B3A,
        "Henna Green" to 0x5E6E2E,
        "Green" to 0x2E8B3D,
        "Grass Green" to 0x3F9B3F,
        "Forest Green" to 0x228B22,
        "Dark Green" to 0x0B4D1F,
        "Emerald" to 0x0F8A52,
        "Emerald Green" to 0x0E9E6B,
        "Bottle Green" to 0x0B5D3B,
        "Sea Green" to 0x2E8B57,
        "Jade Green" to 0x00A877,
        "Mint" to 0x98D8B1,
        "Mint Green" to 0x98FF98,
        "Pista Green" to 0x93C572,
        "Pistachio" to 0x9DC183,
        "Sage Green" to 0x9CAF88,
        "Parrot Green" to 0x4DE147,
        "Teal" to 0x0F8B8D,
        "Dark Teal" to 0x0E4D4A,
        "Teal Green" to 0x158078,
        "Rama Green" to 0x1F6B4E,
        "Turquoise" to 0x40E0D0,
        "Aqua" to 0x3FC1C9,
        "Aqua Blue" to 0x00B7C2,
        "Cyan" to 0x00C8D7,
        "Peacock Blue" to 0x0E6E8C,
        "Peacock Green" to 0x00817A,
        "Sky Blue" to 0x5AA9E6,
        "Powder Blue" to 0xA6C8E0,
        "Baby Blue" to 0xA7C7E7,
        "Ice Blue" to 0xC9E6F0,
        "Blue" to 0x2554C7,
        "Royal Blue" to 0x2B4FB8,
        "Cobalt" to 0x0047AB,
        "Cobalt Blue" to 0x1338BE,
        "Cerulean" to 0x2A7FB8,
        "Navy" to 0x1B2A5B,
        "Navy Blue" to 0x001F54,
        "Midnight Blue" to 0x0B1F45,
        "Ink Blue" to 0x25396F,
        "Denim" to 0x4A6FA5,
        "Denim Blue" to 0x3B6EA5,
        "Steel Blue" to 0x4682B4,
        "Slate Blue" to 0x6A5ACD,
        "Indigo" to 0x4B0082,
        "Indigo Blue" to 0x3F00FF,
        "French Blue" to 0x0072BB,
        "Air Force Blue" to 0x5D8AA8,
        "Violet" to 0x7F4FC9,
        "Bright Violet" to 0x8F00FF,
        "Purple" to 0x7B3F9E,
        "Royal Purple" to 0x6B3FA0,
        "Deep Purple" to 0x4B0060,
        "Plum" to 0x8E4585,
        "Wine Purple" to 0x5E2750,
        "Mauve" to 0xB784A7,
        "Lavender" to 0xB57EDC,
        "Light Lavender" to 0xDCD0FF,
        "Lilac" to 0xC8A2C8,
        "Amethyst" to 0x9966CC,
        "Grape" to 0x6F2DA8,
        "Byzantium" to 0x702963,
        "Orchid" to 0xDA70D6,
        "Brown" to 0x7B4B2A,
        "Chocolate" to 0x6B3F28,
        "Dark Chocolate" to 0x3B1F0F,
        "Coffee" to 0x4B3621,
        "Chestnut" to 0x954535,
        "Sienna" to 0xA0522D,
        "Mahogany" to 0x4E0707,
        "Cinnamon" to 0x7B3F00,
        "Caramel" to 0xA5691A,
        "Walnut Brown" to 0x5C4033,
        "Coffee Brown" to 0x4A2C19,
        "Toffee" to 0x8A5A37
    )

    private val LAB: List<DoubleArray> = NAMES.map { lab(it.second) }

    private fun lab(rgb: Int): DoubleArray {
        var r = ((rgb shr 16) and 255) / 255.0
        var g = ((rgb shr 8) and 255) / 255.0
        var b = (rgb and 255) / 255.0
        r = if (r > 0.04045) Math.pow((r + 0.055) / 1.055, 2.4) else r / 12.92
        g = if (g > 0.04045) Math.pow((g + 0.055) / 1.055, 2.4) else g / 12.92
        b = if (b > 0.04045) Math.pow((b + 0.055) / 1.055, 2.4) else b / 12.92
        var x = (r * 0.4124 + g * 0.3576 + b * 0.1805) / 0.95047
        var y = (r * 0.2126 + g * 0.7152 + b * 0.0722)
        var z = (r * 0.0193 + g * 0.1192 + b * 0.9505) / 1.08883
        x = if (x > 0.008856) Math.cbrt(x) else (7.787 * x + 16.0 / 116.0)
        y = if (y > 0.008856) Math.cbrt(y) else (7.787 * y + 16.0 / 116.0)
        z = if (z > 0.008856) Math.cbrt(z) else (7.787 * z + 16.0 / 116.0)
        return doubleArrayOf(116 * y - 16, 500 * (x - y), 200 * (y - z))
    }

    /** Nearest colour name, e.g. 0xB22222 gives "Firebrick". */
    fun name(rgb: Int): String {
        val t = lab(rgb)
        var best = NAMES[0].first
        var bd = Double.MAX_VALUE
        for (i in LAB.indices) {
            val l = LAB[i]
            val dl = t[0] - l[0]; val da = t[1] - l[1]; val db = t[2] - l[2]
            val d = dl * dl + da * da + db * db
            if (d < bd) { bd = d; best = NAMES[i].first }
        }
        return best
    }

    fun toHex(rgb: Int): String = String.format("#%06x", rgb and 0xFFFFFF)

    /** "#b22222" or "b22222" or "#abc" to 0xRRGGBB, or null. */
    fun parseHex(h: String): Int? {
        var s = h.trim().removePrefix("#")
        if (s.length == 3) s = "" + s[0] + s[0] + s[1] + s[1] + s[2] + s[2]
        if (s.length != 6) return null
        return s.toIntOrNull(16)
    }

    /**
     * Main colour of a picture, given its pixels (ARGB ints of a small copy,
     * about 44 x 44): the same weighting as the website - ignores near-white
     * background and near-black, and favours saturated pixels.
     */
    fun dominant(pixels: IntArray): Int {
        var r = 0.0; var g = 0.0; var b = 0.0; var n = 0.0
        for (p in pixels) {
            val a = (p ushr 24) and 255
            val R = (p shr 16) and 255; val G = (p shr 8) and 255; val B = p and 255
            if (a < 128) continue
            val mx = maxOf(R, G, B); val mn = minOf(R, G, B)
            if (mx > 242 && mn > 242) continue
            if (mx < 14) continue
            val w = 1 + (mx - mn) / 48.0
            r += R * w; g += G * w; b += B * w; n += w
        }
        if (n < 1) {
            r = 0.0; g = 0.0; b = 0.0; n = 0.0
            for (p in pixels) { r += (p shr 16) and 255; g += (p shr 8) and 255; b += p and 255; n += 1.0 }
            if (n < 1) return 0x888888
        }
        fun c(x: Double) = maxOf(0, minOf(255, Math.round(x / n).toInt()))
        return (c(r) shl 16) or (c(g) shl 8) or c(b)
    }

    /** A short list of popular shades for the quick swatches in the colour picker. */
    val QUICK: List<Int> = listOf(
        0x800000, 0x9E1B32, 0xE12B2B, 0xDC143C, 0xFF7F50, 0xFF8DA1, 0xD5257A, 0xFF00FF,
        0xFF8C1A, 0xC9A227, 0xD4AF37, 0xFFD94A, 0x6B8E23, 0x2E8B3D, 0x0B5D3B, 0x98D8B1,
        0x0F8B8D, 0x40E0D0, 0x0E6E8C, 0x5AA9E6, 0x2B4FB8, 0x001F54, 0x7B3F9E, 0xB57EDC,
        0x7B4B2A, 0xD2B48C, 0xF5F5DC, 0xFFFFF0, 0xC0C0C0, 0x808080, 0x36454F, 0x000000
    )
}
