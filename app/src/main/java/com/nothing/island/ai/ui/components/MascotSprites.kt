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
    )

    private val RED = Color(0xFFD71920)
    private val WHITE = Color(0xFFFFFFFF)
    private val BLACK = Color(0xFF000000)
    private val ORANGE = Color(0xFFF57C1F)
    private val CREAM = Color(0xFFFFF2CC)
    private val CYAN = Color(0xFF00E5FF)
    private val PURPLE = Color(0xFFB388FF)
    private val GREEN = Color(0xFF30D158)

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

    val ALL = listOf(CAT, GHOST, ROBOT, FOX, OWL, SLIME)
}
