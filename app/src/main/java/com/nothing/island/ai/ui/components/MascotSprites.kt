package com.nothing.island.ai.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Original pixel sprites for the Dot Island mascot system. Each sprite is a grid where
 * each character maps to a color via [palette]:
 *   '0' = transparent, other chars = palette key.
 * All sprites are crisp pixel-grid designs built for AMOLED punch-hole capsules.
 */
object MascotSprites {
    /** How a sprite moves while patrolling in the pill. */
    enum class Motion { WALK, BOUNCE, FLOAT, WADDLE }

    data class Sprite(
        val name: String,
        val displayName: String = name,
        val rows: List<String>,
        val palette: Map<Char, Color>,
        val motion: Motion = Motion.WALK,
        /** Subtle idle blink / tail-wag frames; null = static idle. */
        val idleFrames: List<List<String>>? = null,
        /** Alternate leg frames for a walk cycle; null = no leg animation. */
        val walkFrames: List<List<String>>? = null,
        /** Rhythmic playback frames when music is active. */
        val musicFrames: List<List<String>>? = null,
        /** Energized red/white pulse frames while charging. */
        val chargingFrames: List<List<String>>? = null,
        /** Perked ears / widened eyes when a call or alert arrives. */
        val alertFrames: List<List<String>>? = null,
        /** Happy tap / pet frames with pixel heart accent. */
        val happyFrames: List<List<String>>? = null,
    )

    /** Builds two leg-swap frames by shifting the bottom two rows left/right. */
    private fun legFrames(base: List<String>): List<List<String>> {
        val last = base.last()
        val secondLast = base[base.size - 2]
        val frameA = base.dropLast(2) + listOf("0$secondLast", "00$last")
        val frameB = base.dropLast(2) + listOf("${secondLast}0", "${last}00")
        return listOf(frameA, frameB)
    }

    private val RED = Color(0xFFD71921)
    private val WHITE = Color(0xFFFFFFFF)
    private val LIGHT_GRAY = Color(0xFFB8B8C0)
    private val DARK_GRAY = Color(0xFF2B2B30)
    private val BLACK = Color(0xFF000000)
    private val ORANGE = Color(0xFFF57C1F)
    private val CREAM = Color(0xFFFFF2CC)
    private val CYAN = Color(0xFF00E5FF)
    private val PURPLE = Color(0xFFB388FF)
    private val GREEN = Color(0xFF30D158)
    private val TEAL = Color(0xFF4DD0C4)
    private val YELLOW = Color(0xFFFFD60A)
    private val NAVY = Color(0xFF1A2A4F)

    private val dotCatBase = listOf(
        "1000001000",
        "1100011000",
        "1111111000",
        "1211121000",
        "1111111000",
        "0111110000",
        "0111111001",
        "1111111001",
        "1111111110",
        "1100011000",
    )

    private val dotCatTailWag = listOf(
        "1000001000",
        "1100011000",
        "1111111000",
        "1211121000",
        "1111111000",
        "0111110000",
        "0111111010",
        "1111111010",
        "1111111100",
        "1100011000",
    )

    private val dotCatBlink = listOf(
        "1000001000",
        "1100011000",
        "1111111000",
        "1311131000",
        "1111111000",
        "0111110000",
        "0111111001",
        "1111111001",
        "1111111110",
        "1100011000",
    )

    /** Signature perched white DotCat matching ic_launcher_foreground.xml with #D71921 eyes. */
    val DOTCAT = Sprite(
        name = "DOTCAT",
        displayName = "DotCat",
        rows = dotCatBase,
        palette = mapOf(
            '1' to WHITE,
            '2' to RED,
            '3' to LIGHT_GRAY,
        ),
        motion = Motion.WALK,
        idleFrames = listOf(dotCatBase, dotCatBase, dotCatTailWag, dotCatBlink),
        walkFrames = listOf(
            listOf(
                "1000001000",
                "1100011000",
                "1111111000",
                "1211121000",
                "1111111000",
                "0111110000",
                "0111111001",
                "1111111001",
                "1111111110",
                "1000011000",
            ),
            listOf(
                "1000001000",
                "1100011000",
                "1111111000",
                "1211121000",
                "1111111000",
                "0111110000",
                "0111111010",
                "1111111010",
                "1111111100",
                "1100001000",
            ),
        ),
        musicFrames = listOf(
            dotCatBase,
            listOf(
                "0000000000",
                "1000001000",
                "1100011000",
                "1111111000",
                "1211121000",
                "1111111000",
                "0111111010",
                "1111111010",
                "1111111100",
                "1100011000",
            ),
        ),
        chargingFrames = listOf(
            listOf(
                "1000001000",
                "1100011000",
                "1111111000",
                "1211121000",
                "1111111000",
                "0112110000",
                "0111111002",
                "1111111002",
                "1111111120",
                "1100011000",
            ),
            listOf(
                "1000001000",
                "1100011000",
                "1111111000",
                "1211121000",
                "1111111000",
                "0111110000",
                "0112211001",
                "1112211001",
                "1111111110",
                "1100011000",
            ),
        ),
        alertFrames = listOf(
            listOf(
                "1000001000",
                "1000001000",
                "1111111000",
                "1211121000",
                "1211121000",
                "1111111000",
                "0111111010",
                "1111111010",
                "1111111100",
                "1100011000",
            ),
            listOf(
                "1000001000",
                "1100011000",
                "1111111000",
                "1211121000",
                "1211121000",
                "1111111000",
                "0111111001",
                "1111111001",
                "1111111110",
                "1100011000",
            ),
        ),
        happyFrames = listOf(
            listOf(
                "1020201000",
                "1102011000",
                "1111111000",
                "1211121000",
                "1111111000",
                "0111110000",
                "0111111010",
                "1111111010",
                "1111111100",
                "1100011000",
            ),
        ),
    )

    /** Segmented Nothing Glyph ring orb with pulsing #D71921 core. */
    val GLYPH_ORB = Sprite(
        name = "GLYPH_ORB",
        displayName = "GlyphOrb",
        rows = listOf(
            "0011111100",
            "0100000010",
            "1000000001",
            "1000220001",
            "1002222001",
            "1002222001",
            "1000220001",
            "1000000001",
            "0100000010",
            "0011111100",
        ),
        palette = mapOf('1' to WHITE, '2' to RED, '3' to LIGHT_GRAY),
        motion = Motion.FLOAT,
        idleFrames = listOf(
            listOf(
                "0011111100",
                "0100000010",
                "1000000001",
                "1000220001",
                "1002222001",
                "1002222001",
                "1000220001",
                "1000000001",
                "0100000010",
                "0011111100",
            ),
            listOf(
                "0011001100",
                "0100000010",
                "1000000001",
                "1000000001",
                "0000220000",
                "0000220000",
                "1000000001",
                "1000000001",
                "0100000010",
                "0011001100",
            ),
        ),
        musicFrames = listOf(
            listOf(
                "0011111100",
                "0100220010",
                "1002222001",
                "1022222201",
                "1022112201",
                "1022112201",
                "1022222201",
                "1002222001",
                "0100220010",
                "0011111100",
            ),
            listOf(
                "0011001100",
                "0100000010",
                "1000220001",
                "1002222001",
                "0002222000",
                "0002222000",
                "1002222001",
                "1000220001",
                "0100000010",
                "0011001100",
            ),
        ),
        chargingFrames = listOf(
            listOf(
                "0022222200",
                "0200000020",
                "2000110002",
                "2001221002",
                "2012222102",
                "2012222102",
                "2001221002",
                "2000110002",
                "0200000020",
                "0022222200",
            ),
            listOf(
                "0011111100",
                "0100000010",
                "1000220001",
                "1002112001",
                "1021111201",
                "1021111201",
                "1002112001",
                "1000220001",
                "0100000010",
                "0011111100",
            ),
        ),
        alertFrames = listOf(
            listOf(
                "0022222200",
                "0211111120",
                "2100000012",
                "2100220012",
                "2102222012",
                "2102222012",
                "2100220012",
                "2100000012",
                "0211111120",
                "0022222200",
            ),
        ),
    )

    /** Monochrome cassette companion with spinning red/white tape reels. */
    val TAPE_BUDDY = Sprite(
        name = "TAPE_BUDDY",
        displayName = "TapeBuddy",
        rows = listOf(
            "011111111110",
            "113333333311",
            "113213312311",
            "113123321311",
            "113333333311",
            "111111111111",
            "110333333011",
            "111111111111",
            "001100001100",
        ),
        palette = mapOf('1' to WHITE, '2' to RED, '3' to DARK_GRAY),
        motion = Motion.BOUNCE,
        idleFrames = listOf(
            listOf(
                "011111111110",
                "113333333311",
                "113213312311",
                "113123321311",
                "113333333311",
                "111111111111",
                "110333333011",
                "111111111111",
                "001100001100",
            ),
            listOf(
                "011111111110",
                "113333333311",
                "113123321311",
                "113213312311",
                "113333333311",
                "111111111111",
                "110333333011",
                "111111111111",
                "001100001100",
            ),
        ),
        musicFrames = listOf(
            listOf(
                "011111111110",
                "113333333311",
                "113223322311",
                "113113311311",
                "113332233311",
                "111111111111",
                "110332233011",
                "111111111111",
                "011000000110",
            ),
            listOf(
                "011111111110",
                "113333333311",
                "113113311311",
                "113223322311",
                "113331133311",
                "111111111111",
                "110333333011",
                "111111111111",
                "001100001100",
            ),
        ),
        chargingFrames = listOf(
            listOf(
                "011111111110",
                "113333333311",
                "113223322311",
                "113223322311",
                "113333333311",
                "111112211111",
                "110332233011",
                "111111111111",
                "001100001100",
            ),
        ),
        alertFrames = listOf(
            listOf(
                "022111111220",
                "113333333311",
                "113223322311",
                "113223322311",
                "113333333311",
                "111111111111",
                "110333333011",
                "111111111111",
                "001100001100",
            ),
        ),
    )

    val CAT = DOTCAT

    val GHOST = Sprite(
        name = "GHOST",
        displayName = "Ghost",
        rows = listOf(
            "00111100",
            "01111110",
            "11022011",
            "11111111",
            "11111111",
            "11111111",
            "11111111",
            "11011011",
        ),
        palette = mapOf('1' to WHITE, '2' to BLACK),
        motion = Motion.FLOAT,
    )

    val ROBOT = Sprite(
        name = "ROBOT",
        displayName = "Robot",
        rows = listOf(
            "00111100",
            "01111110",
            "11022011",
            "11111111",
            "01111110",
            "01000010",
            "11111111",
            "01000010",
        ),
        palette = mapOf('1' to CYAN, '2' to BLACK),
        walkFrames = legFrames(
            listOf(
                "00111100",
                "01111110",
                "11022011",
                "11111111",
                "01111110",
                "01000010",
                "11111111",
                "01000010",
            )
        ),
    )

    val FOX = Sprite(
        name = "FOX",
        displayName = "Fox",
        rows = listOf(
            "100000000001",
            "110000000011",
            "111000000111",
            "011111111110",
            "011322223110",
            "012222222210",
            "011222222110",
            "001122211100",
            "001112211100",
            "000111111000",
            "000111111000",
            "000110011000",
        ),
        palette = mapOf(
            '1' to ORANGE,
            '2' to CREAM,
            '3' to BLACK,
        ),
        motion = Motion.WADDLE,
        walkFrames = legFrames(
            listOf(
                "100000000001",
                "110000000011",
                "111000000111",
                "011111111110",
                "011322223110",
                "012222222210",
                "011222222110",
                "001122211100",
                "001112211100",
                "000111111000",
                "000111111000",
                "000110011000",
            )
        ),
    )

    val OWL = Sprite(
        name = "OWL",
        displayName = "Owl",
        rows = listOf(
            "11000011",
            "11111111",
            "11022011",
            "01111110",
            "00111100",
            "01111110",
            "11022011",
            "00100100",
        ),
        palette = mapOf('1' to PURPLE, '2' to BLACK),
        walkFrames = legFrames(
            listOf(
                "11000011",
                "11111111",
                "11022011",
                "01111110",
                "00111100",
                "01111110",
                "11022011",
                "00100100",
            )
        ),
    )

    val SLIME = Sprite(
        name = "SLIME",
        displayName = "Slime",
        rows = listOf(
            "00111100",
            "01111110",
            "11111111",
            "11022011",
            "11111111",
            "11111111",
            "11111111",
            "01111110",
        ),
        palette = mapOf('1' to GREEN, '2' to BLACK),
    )

    val ALIEN = Sprite(
        name = "ALIEN",
        displayName = "Alien",
        rows = listOf(
            "11000011",
            "01100110",
            "01111110",
            "12211221",
            "12211221",
            "01122110",
            "00111100",
            "01100110",
            "11000011",
        ),
        palette = mapOf('1' to TEAL, '2' to BLACK),
        motion = Motion.BOUNCE,
    )

    val BEE = Sprite(
        name = "BEE",
        displayName = "Bee",
        rows = listOf(
            "00100000",
            "00111100",
            "01122110",
            "12222221",
            "12122121",
            "01111110",
            "00111100",
            "01100110",
        ),
        palette = mapOf('1' to YELLOW, '2' to NAVY),
        motion = Motion.BOUNCE,
    )

    val ALL = listOf(DOTCAT, GLYPH_ORB, TAPE_BUDDY, GHOST, ROBOT, FOX, OWL, SLIME, ALIEN, BEE)
}
