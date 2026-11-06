package com.nothing.island.ai.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nothing.island.ai.NothingBorder
import com.nothing.island.ai.NothingDarkGray
import com.nothing.island.ai.NothingWhite
import com.nothing.island.ai.media.ArtworkShape

/**
 * Renders album artwork with user-configurable shape (Circle, Squircle, Rounded Rect)
 * and optional continuous rotation while audio is actively playing.
 */
@Composable
fun MusicArtworkView(
    bitmap: Bitmap?,
    fallbackChar: Char = 'M',
    shape: ArtworkShape = ArtworkShape.CIRCLE,
    isPlaying: Boolean = false,
    rotationEnabled: Boolean = true,
    rotationDurationMs: Int = 10000,
    sizeDp: Dp = 48.dp,
    accentColor: Color = Color.Unspecified,
    glowIntensity: Float = 0.5f,
    modifier: Modifier = Modifier
) {
    val composeShape: Shape = when (shape) {
        ArtworkShape.CIRCLE -> CircleShape
        ArtworkShape.SQUIRCLE -> RoundedCornerShape(percent = 35)
        ArtworkShape.ROUNDED_RECT -> RoundedCornerShape(10.dp)
    }

    // Artwork rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "ArtworkSpin")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying && rotationEnabled) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = rotationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ArtworkRotationAngle"
    )

    val currentRotation = if (rotationEnabled && isPlaying) rotationAngle else 0f

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(
                if (glowIntensity > 0f && accentColor != Color.Unspecified) {
                    Modifier.border(1.5.dp, accentColor.copy(alpha = 0.4f * glowIntensity), composeShape)
                } else {
                    Modifier.border(1.dp, NothingBorder, composeShape)
                }
            )
            .clip(composeShape)
            .background(NothingDarkGray)
            .rotate(currentRotation),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Album Artwork",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Elegant fallback: Nothing OS styled monochrome icon
            Text(
                text = fallbackChar.uppercaseChar().toString(),
                color = NothingWhite,
                fontSize = (sizeDp.value * 0.45f).sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
