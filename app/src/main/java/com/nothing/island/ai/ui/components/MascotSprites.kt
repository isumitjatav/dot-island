package com.nothing.island.ai.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Original pixel sprites for the mascot preview. Each sprite is a grid where
 * each character maps to a color via [palette]:
 *   '0' = transparent, other chars = palette key.
 * Sprites are our own drawings inspired by animal shapes, not copied assets.
 */
object MascotSprites {
    /** How a sprite moves while walking in the pill. */
    enum class Motion { WALK, BOUNCE, FLOAT, WADDLE }

    data class Sprite(
        val name: String,
        val rows: List<String>,
        val palette: Map<Char, Color>,
        val motion: Motion = Motion.WALK,
        /** Alternate leg frames for a walk cycle; null = no leg animation. */
        val walkFrames: List<List<String>>? = null,
    )

    /** Builds two leg-swap frames by moving the last two rows left/right. */
    private fun legFrames(base: List<String>): List<List<String>> {
        val last = base.last()
        val secondLast = base[base.size - 2]
        val frameA = base.dropLast(2) + listOf("0$secondLast", "00$last")
        val frameB = base.dropLast(2) + listOf("$secondLast" + "0", "$last" + "00")
        return listOf(frameA, frameB)
    }

    private val RED = Color(0xFFD71920)
    private val WHITE = Color(0xFFFFFFFF)
    private val BLACK = Color(0xFF000000)
    private val ORANGE = Color(0xFFF57C1F)
    private val CREAM = Color(0xFFFFF2CC)
    private val CYAN = Color(0xFF00E5FF)
    private val PURPLE = Color(0xFFB388FF)
    private val GREEN = Color(0xFF30D158)
    private val TEAL = Color(0xFF4DD0C4)
    private val YELLOW = Color(0xFFFFD60A)
    private val NAVY = Color(0xFF1A2A4F)

    val CAT = Sprite(
        name = "CAT",
        rows = listOf(
            "10000001",
            "11111111",
            "11022011",
            "11111111",
            "01111110",
            "00111100",
            "01100110",
            "01000010",
        ),
        palette = mapOf('1' to RED, '2' to BLACK),
        walkFrames = legFrames(
            listOf(
                "10000001",
                "11111111",
                "11022011",
                "11111111",
                "01111110",
                "00111100",
                "01100110",
                "01000010",
            )
        ),
    )

    val GHOST = Sprite(
        name = "GHOST",
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

    /**
     * Detailed fox: orange body, white muzzle and cheek fluff, pointed ears,
     * dark eyes and nose, and a fluffy tail. Original 12x12 drawing.
     */
    val FOX = Sprite(
        name = "FOX",
        rows = listOf(
            "100000000001", // ear tips
            "110000000011",
            "111000000111", // ears
            "011111111110", // head top
            "011322223110", // eyes + white brow
            "012222222210", // white muzzle band
            "011222222110", // cheek fluff
            "001122211100", // lower muzzle
            "001112211100", // nose
            "000111111000", // neck
            "000111111000", // body
            "000110011000", // legs
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

    /** Teal robot/alien creature with big outlined eyes and antenna arms. */
    val ALIEN = Sprite(
        name = "ALIEN",
        rows = listOf(
            "11000011", // antenna tips
            "01100110",
            "01111110", // head
            "12211221", // big outlined eyes
            "12211221",
            "01122110", // mouth
            "00111100",
            "01100110", // legs
            "11000011",
        ),
        palette = mapOf('1' to TEAL, '2' to BLACK),
        motion = Motion.BOUNCE,
    )

    /** Bee character with striped head and sparkles. */
    val BEE = Sprite(
        name = "BEE",
        rows = listOf(
            "00100000", // sparkle
            "00111100", // wings
            "01122110", // head top
            "12222221", // striped head
            "12122121", // eyes + stripes
            "01111110",
            "00111100", // body
            "01100110", // legs
        ),
        palette = mapOf('1' to YELLOW, '2' to NAVY),
        motion = Motion.BOUNCE,
    )

    val ALL = listOf(CAT, GHOST, ROBOT, FOX, OWL, SLIME, ALIEN, BEE)
}
