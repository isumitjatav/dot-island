package com.nothing.island.ai.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Original 8x8 pixel sprites for the mascot preview. Each string is one row,
 * '1' = filled pixel, '.' = eye/highlight pixel, '0' = empty.
 * These are our own simple pixel drawings, not copied assets.
 */
object MascotSprites {
    data class Sprite(
        val name: String,
        val rows: List<String>,
        val color: Color,
    )

    val CAT = Sprite(
        name = "CAT",
        rows = listOf(
            "11000011",
            "11111111",
            "11011011",
            "11111111",
            "01111110",
            "00111100",
            "01100110",
            "01000010",
        ),
        color = Color(0xFFD71920),
    )

    val GHOST = Sprite(
        name = "GHOST",
        rows = listOf(
            "00111100",
            "01111110",
            "11011011",
            "11111111",
            "11111111",
            "11111111",
            "11111111",
            "11011011",
        ),
        color = Color(0xFFFFFFFF),
    )

    val ROBOT = Sprite(
        name = "ROBOT",
        rows = listOf(
            "00111100",
            "01111110",
            "11011011",
            "11111111",
            "01111110",
            "01000010",
            "11111111",
            "01000010",
        ),
        color = Color(0xFF00E5FF),
    )

    val FOX = Sprite(
        name = "FOX",
        rows = listOf(
            "11000011",
            "11100111",
            "11111111",
            "01111110",
            "00111100",
            "00111100",
            "01111110",
            "01000010",
        ),
        color = Color(0xFFFF8A3D),
    )

    val OWL = Sprite(
        name = "OWL",
        rows = listOf(
            "11000011",
            "11111111",
            "11011011",
            "01111110",
            "00111100",
            "01111110",
            "11011011",
            "00100100",
        ),
        color = Color(0xFFB388FF),
    )

    val SLIME = Sprite(
        name = "SLIME",
        rows = listOf(
            "00111100",
            "01111110",
            "11111111",
            "11111111",
            "11011011",
            "11111111",
            "11111111",
            "01111110",
        ),
        color = Color(0xFF30D158),
    )

    val ALL = listOf(CAT, GHOST, ROBOT, FOX, OWL, SLIME)
}
